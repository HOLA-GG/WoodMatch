import { requireThat, safeId } from "../woodmatch/domain.mjs";
import { enqueueNotice } from "../woodmatch/lifecycle.mjs";
const text = (v, max) => typeof v === "string" ? v.trim().slice(0, max) : "";
const hash = async (value) =>
  Array.from(
    new Uint8Array(
      await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)),
    ),
  ).map((v) => v.toString(16).padStart(2, "0")).join("");
export async function adminOperation(
  { db, identity, erase, isAdministrator, clock = Date.now },
  actor,
  operation,
  data = {},
) {
  requireThat(actor.webAdmin === true, 403, "permission", "Acceso denegado.");
  if (operation === "cases") {
    const rows = await db.all("support_cases");
    return {
      cases: rows.map(({ _path, ...row }) => row).sort((a, b) =>
        b.updatedAt - a.updatedAt
      ),
    };
  }
  if (operation === "users") {
    return {
      users: (await db.profiles()).map((r) => ({
        uid: r.uid,
        name: r.name,
        version: r.version,
        deleting: r.deleting === true,
      })),
    };
  }
  if (operation === "publications") {
    return {
      publications: (await db.all("wood_wastes")).map((r) => ({
        id: r.id,
        title: r.title,
        description: r.description,
        status: r.status,
        revision: r.revision || 0,
        authorId: r.authorId,
      })),
    };
  }
  if (operation === "audit") {
    return {
      events: (await db.all("administration_audit")).sort((a, b) => b.at - a.at)
        .slice(0, 200).map(({ _path, ...r }) => r),
    };
  }
  if (operation === "caseDetails") {
    const row = await db.get("support_cases/" + safeId(data.id));
    requireThat(row, 404, "missing", "El caso no existe.");
    return row;
  }
  const id = safeId(data.operationId),
    reason = text(data.reason, 1000),
    at = clock();
  requireThat(
    reason.length >= 8,
    400,
    "reason",
    "Explica el motivo con al menos ocho caracteres.",
  );
  requireThat(
    [
      "updateSupportCase",
      "reopenCase",
      "moderatePublication",
      "renameUser",
      "notifyUser",
      "deleteUser",
    ].includes(operation),
    400,
    "operation",
    "Operación no válida.",
  );
  const target = safeId(data.id), auditPath = "administration_audit/" + id;
  const fingerprint = await hash(JSON.stringify({
    operation,
    target,
    reason,
    status: data.status,
    priority: data.priority,
    response: data.response,
    name: data.name,
    title: data.title,
    body: data.body,
    action: data.action,
    caseId: data.caseId,
    version: data.version,
    expectedRevision: data.expectedRevision,
    expectedUpdatedAt: data.expectedUpdatedAt,
  }));
  const prior = await db.get(auditPath);
  requireThat(
    !prior ||
      (prior.actorId === actor.uid && prior.fingerprint === fingerprint),
    409,
    "operation",
    "La referencia pertenece a otra operación.",
  );
  if (prior?.state === "COMPLETED") return { ok: true };
  if (["renameUser", "deleteUser", "notifyUser"].includes(operation)) {
    requireThat(
      target !== actor.uid && !await isAdministrator(target),
      403,
      "protected",
      "Las cuentas administrativas están protegidas.",
    );
  }
  if (operation === "deleteUser") {
    requireThat(
      at - actor.auth_time * 1000 >= 0 && at - actor.auth_time * 1000 <= 300000,
      401,
      "recent",
      "Vuelve a iniciar sesión antes de eliminar una cuenta.",
    );
    requireThat(
      data.confirmation === target,
      400,
      "confirmation",
      "Confirma el identificador exacto de la cuenta.",
    );
    const account = (await identity("lookup", { localId: [target] })).users
      ?.[0];
    requireThat(
      account || prior?.state === "STARTED",
      404,
      "missing",
      "La cuenta no existe.",
    );
    // Tombstone/disable precede cleanup and deletion; the audit is retained across retries.
    await db.set(auditPath, {
      id,
      actorId: actor.uid,
      operation,
      target,
      reason,
      fingerprint,
      at: prior?.at || at,
      state: "STARTED",
    });
    if (account) await erase(target);
    await db.set(auditPath, {
      id,
      actorId: actor.uid,
      operation,
      target,
      reason,
      fingerprint,
      at: prior?.at || at,
      completedAt: clock(),
      state: "COMPLETED",
    });
    return { ok: true };
  }
  await db.atomic(async (tx) => {
    const existing = await tx.get(auditPath);
    if (existing?.state === "COMPLETED") return;
    let noticeUid = target,
      title = text(data.title, 120),
      body = text(data.body, 1000),
      fields = {};
    if (operation === "updateSupportCase") {
      const row = await tx.get("support_cases/" + target),
        status = text(data.status, 32),
        priority = text(data.priority, 16),
        response = text(data.response, 1500);
      requireThat(
        row && row.updatedAt === data.expectedUpdatedAt,
        409,
        "revision",
        "El caso cambió. Recarga antes de continuar.",
      );
      requireThat(
        ["NUEVO", "EN_REVISION", "EN_PROGRESO", "RESUELTO", "CERRADO"].includes(
          status,
        ) && ["BAJA", "MEDIA", "ALTA", "CRITICA"].includes(priority),
        400,
        "status",
        "Estado o prioridad no válidos.",
      );
      requireThat(
        !["RESUELTO", "CERRADO"].includes(status) || response,
        400,
        "response",
        "Escribe una respuesta antes de resolver o cerrar.",
      );
      tx.set("support_cases/" + target, {
        ...row,
        status,
        priority,
        response,
        updatedAt: at,
        ...(response && !row.firstResponseAt ? { firstResponseAt: at } : {}),
        ...(["RESUELTO", "CERRADO"].includes(status) && !row.resolvedAt
          ? { resolvedAt: at }
          : {}),
        history: [...(row.history || []).slice(-49), {
          at,
          action: status,
          actorId: actor.uid,
          reason,
        }],
      });
      noticeUid = row.reporterId;
      title = "Respuesta de soporte";
      body = response || reason;
      fields = { caseId: target };
    } else if (operation === "renameUser") {
      const name = text(data.name, 120);
      requireThat(name.length >= 2, 400, "name", "Escribe un nombre válido.");
      requireThat(
        await tx.get("users/" + target),
        404,
        "missing",
        "No se encontró el perfil.",
      );
      requireThat(
        typeof data.version === "string" && data.version.length > 0,
        400,
        "version",
        "Recarga el perfil.",
      );
      tx.patch("users/" + target, { name }, data.version);
      title = "Nombre actualizado";
      body = reason;
    } else if (operation === "reopenCase") {
      const row = await tx.get("support_cases/" + target);
      requireThat(
        row && ["RESUELTO", "CERRADO"].includes(row.status) &&
          row.updatedAt === data.expectedUpdatedAt,
        409,
        "revision",
        "Recarga el caso antes de reabrirlo.",
      );
      tx.set("support_cases/" + target, {
        ...row,
        status: "EN_PROGRESO",
        updatedAt: at,
        history: [...(row.history || []).slice(-49), {
          action: "REABIERTO",
          at,
          reason,
          actorId: actor.uid,
        }],
      });
      noticeUid = row.reporterId;
      title = "Tu caso fue reabierto";
      body = reason;
      fields = { caseId: target };
    } else if (operation === "moderatePublication") {
      const row = await tx.get("wood_wastes/" + target);
      requireThat(
        row && (row.revision || 0) === data.expectedRevision,
        409,
        "revision",
        "La publicación cambió. Recarga antes de continuar.",
      );
      requireThat(
        ["OCULTAR", "RESTAURAR"].includes(data.action),
        400,
        "action",
        "Acción no válida.",
      );
      requireThat(
        data.action === "OCULTAR"
          ? ["HALLAZGO", "DISPONIBLE"].includes(row.status)
          : row.status === "MODERADO" &&
            ["HALLAZGO", "DISPONIBLE"].includes(row.moderationPreviousStatus),
        409,
        "state",
        "No se puede moderar material reservado o entregado.",
      );
      const caseId = data.caseId ? safeId(data.caseId) : null,
        c = caseId ? await tx.get("support_cases/" + caseId) : null;
      requireThat(
        !caseId || c?.publicationId === target,
        409,
        "case",
        "El reporte no corresponde a esta publicación.",
      );
      tx.set("wood_wastes/" + target, {
        ...row,
        status: data.action === "OCULTAR"
          ? "MODERADO"
          : row.moderationPreviousStatus,
        moderationPreviousStatus: data.action === "OCULTAR"
          ? row.status
          : row.moderationPreviousStatus,
        moderationReason: reason,
        moderatedBy: actor.uid,
        moderatedAt: at,
        updatedAt: at,
        revision: (row.revision || 0) + 1,
      });
      if (caseId) {
        tx.set("support_cases/" + caseId, {
          ...c,
          moderationAction: data.action,
          updatedAt: at,
          history: [...(c.history || []).slice(-49), {
            at,
            action: data.action,
            reason,
            actorId: actor.uid,
          }],
        });
      }
      tx.set("wood_wastes/" + target + "/events/" + id, {
        at,
        action: data.action,
        reason,
        actorId: actor.uid,
      });
      noticeUid = row.authorId;
      title = data.action === "OCULTAR"
        ? "Publicación retirada de visibilidad"
        : "Publicación restaurada";
      body = reason;
    } else {
      requireThat(
        await tx.get("users/" + target),
        404,
        "missing",
        "No se encontró el perfil.",
      );
      requireThat(
        title && body,
        400,
        "notice",
        "Completa el título y el mensaje.",
      );
    }
    await enqueueNotice(tx, hash, noticeUid, id, title, body, fields, at);
    tx.set(auditPath, {
      id,
      actorId: actor.uid,
      operation,
      target,
      reason,
      fingerprint,
      at,
      state: "COMPLETED",
    });
  });
  return { ok: true };
}
