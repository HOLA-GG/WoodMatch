// Shared by the Edge Function and deterministic Node tests. No provider keys here.
export class Fault extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}
export const requireThat = (ok, status, code, message) => {
  if (!ok) throw new Fault(status, code, message);
};
const text = (value, max, optional = false) => {
  requireThat(
    typeof value === "string" && value.length <= max &&
      (optional || value.trim()),
    400,
    "invalid",
    "Revisa los campos.",
  );
  return value.trim();
};
export const safeId = (value) => {
  requireThat(
    typeof value === "string" && /^[A-Za-z0-9_-]{1,128}$/.test(value),
    400,
    "invalid",
    "Identificador no válido.",
  );
  return value;
};
export const SECTORS = [
  "Suba Centro",
  "Niza / La Colina",
  "El Rincón",
  "Tibabuyes",
  "San José de Bavaria",
  "Britalia / Carmelo",
  "Lombardía / Compartir",
  "Guaymaral / Torca",
];
export const MATERIALS = [
  "Árbol",
  "Podas y ramas",
  "Troncos",
  "Estibas",
  "Tablas",
  "Muebles",
  "Madera recuperable",
];
export const CATEGORIES = ["BIOMASA", "ESTANDAR", "FINA"];
export const PRIVACY_VERSION = "2026-PILOTO-2";
export const publicItem = (item) =>
  item && item.schemaVersion >= 2 &&
  ["DISPONIBLE", "RESERVADO", "HALLAZGO"].includes(item.status);
export const participant = (request, uid) =>
  request && [request.authorId, request.requesterId].includes(uid);
export const canReadItem = (item, uid) =>
  item &&
  (publicItem(item) || item.authorId === uid || item.completedBy === uid);

export function validateEntry(data, profile) {
  requireThat(
    ["HALLAZGO", "OFERTA"].includes(data.entryType),
    400,
    "invalid",
    "Elige hallazgo o material ofrecido.",
  );
  if (data.entryType === "OFERTA") {
    requireThat(
      ["GENERADOR", "MIXTO"].includes(profile.role),
      403,
      "role",
      "Tu perfil no puede ofrecer material.",
    );
    requireThat(
      data.authorizedToOffer === true,
      400,
      "authorization",
      "Confirma tu autorización para entregar el material.",
    );
    requireThat(
      Number.isFinite(data.quantityValue) && data.quantityValue > 0 &&
        data.quantityValue <= 1000000 &&
        ["unidades", "bultos", "kg", "m³"].includes(data.quantityUnit),
      400,
      "quantity",
      "Revisa la cantidad y su unidad.",
    );
  }
  requireThat(
    data.locationConfirmed === true && data.exactLocationConsent === true,
    400,
    "location",
    "Confirma el punto y que será visible en el mapa.",
  );
  // Bounds are an initial service area, not a claim about administrative boundaries.
  requireThat(
    Number.isFinite(data.exactLatitude) &&
      Number.isFinite(data.exactLongitude) && data.exactLatitude >= 4.66 &&
      data.exactLatitude <= 4.86 && data.exactLongitude >= -74.18 &&
      data.exactLongitude <= -74.01,
    400,
    "location",
    "El punto debe estar en el área de atención de Suba.",
  );
  requireThat(
    SECTORS.includes(data.subaSector) && MATERIALS.includes(data.wasteType) &&
      CATEGORIES.includes(data.category),
    400,
    "invalid",
    "Revisa el sector y el material.",
  );
  const condition = text(data.materialState, 120);
  return {
    entryType: data.entryType,
    title: text(data.title, 120),
    description: text(data.description ?? "", 3000, true),
    wasteType: data.wasteType,
    category: data.category,
    materialState: condition,
    quantityValue: data.entryType === "OFERTA" ? data.quantityValue : 0,
    quantityUnit: data.entryType === "OFERTA" ? data.quantityUnit : "hallazgo",
    quantityEstimated: true,
    subaSector: data.subaSector,
    exactLatitude: data.exactLatitude,
    exactLongitude: data.exactLongitude,
    locationConfirmed: true,
    exactLocationConsent: true,
    authorizedToOffer: data.entryType === "OFERTA",
    speciesName: text(data.speciesName ?? "", 160, true),
    suggestedUses: "",
    isAiAssisted: false,
  };
}

export function createDomain({ db, hash, passCode, now = Date.now }) {
  // Every mutation's actor, role and current revision are rechecked inside the transaction.
  async function mutate(actor, operation, data, work) {
    const op = safeId(data.operationId);
    const fingerprint = await hash(JSON.stringify({ operation, data }));
    const path = `operations/${await hash(`${actor.uid}:${operation}:${op}`)}`;
    return db.atomic(async (tx) => {
      const profile = await tx.get(`users/${actor.uid}`);
      const tombstone = await tx.get(`account_tombstones/${actor.uid}`);
      requireThat(
        profile && !profile.deleting && !tombstone,
        403,
        "account",
        "Tu cuenta no está disponible.",
      );
      const existing = await tx.get(path);
      if (existing) {
        requireThat(
          existing.fingerprint === fingerprint,
          409,
          "operation",
          "Esta operación ya se utilizó con otros datos.",
        );
        return existing.result;
      }
      const result = await work(tx, profile);
      tx.set(path, { uid: actor.uid, fingerprint, result, createdAt: now() });
      return result;
    });
  }
  async function event(tx, requestId, op, actor, action) {
    tx.set(`withdrawal_requests/${requestId}/events/${safeId(op)}`, {
      actorId: actor.uid,
      action,
      at: now(),
    });
  }
  async function call(actor, operation, data) {
    safeId(actor.uid);
    if (operation === "reservePhoto") {
      return mutate(actor, operation, data, async (tx) => {
        const path = `${actor.uid}/${safeId(data.id)}/${
          safeId(data.operationId)
        }.jpg`;
        requireThat(
          Number.isInteger(data.bytes) && data.bytes > 0 &&
            data.bytes <= 1048576,
          400,
          "photo",
          "Fotografía demasiado grande.",
        );
        const globalRef = "photo_budget/global",
          userRef = `photo_budget/${actor.uid}`;
        const global = await tx.get(globalRef), user = await tx.get(userRef);
        requireThat(
          (global?.bytes || 0) + data.bytes <= 700 * 1024 * 1024 &&
            (user?.count || 0) < 100,
          429,
          "quota",
          "Se alcanzó el límite de fotografías del piloto.",
        );
        tx.set(globalRef, { bytes: (global?.bytes || 0) + data.bytes });
        tx.set(userRef, {
          uid: actor.uid,
          count: (user?.count || 0) + 1,
          bytes: (user?.bytes || 0) + data.bytes,
        });
        tx.set(`photo_uploads/${await hash(path)}`, {
          uid: actor.uid,
          entryId: data.id,
          path,
          bytes: data.bytes,
          contentHash: data.contentHash,
          state: "pending",
          createdAt: now(),
        });
        return { path };
      });
    }
    if (operation === "publishEntry" || operation === "editEntry") {
      const id = safeId(data.id);
      return mutate(actor, operation, data, async (tx, profile) => {
        const ref = `wood_wastes/${id}`, existing = await tx.get(ref);
        if (operation === "editEntry") {
          requireThat(
            existing?.authorId === actor.uid,
            403,
            "owner",
            "Solo puedes editar tus publicaciones.",
          );
          requireThat(
            (existing.revision ?? 0) === data.expectedRevision,
            409,
            "revision",
            "La publicación cambió. Vuelve a abrirla.",
          );
          requireThat(
            ["HALLAZGO", "DISPONIBLE"].includes(existing.status),
            409,
            "reserved",
            "Cancela la reserva antes de editar.",
          );
        } else {requireThat(
            !existing,
            409,
            "exists",
            "La publicación ya existe.",
          );}
        const entry = validateEntry(data, profile);
        const imagePath = data.imagePath || existing?.imagePath || null;
        const image = imagePath
          ? await tx.get(`photo_uploads/${await hash(imagePath)}`)
          : null;
        requireThat(
          imagePath && image?.uid === actor.uid && image?.entryId === id &&
            image?.state === "ready",
          400,
          "photo",
          "Adjunta una fotografía antes de publicar.",
        );
        const item = {
          ...entry,
          id,
          schemaVersion: 3,
          revision: (existing?.revision ?? 0) + 1,
          authorId: actor.uid,
          authorName: profile.name,
          authorPhone: "",
          referenceAddress: "",
          imageUri: null,
          imagePath,
          status: entry.entryType === "HALLAZGO" ? "HALLAZGO" : "DISPONIBLE",
          reservedBy: null,
          reservedByName: null,
          completedBy: null,
          completedAt: null,
          createdAt: existing?.createdAt ?? now(),
          updatedAt: now(),
        };
        tx.set(ref, item);
        tx.set(`wood_wastes/${id}/events/${data.operationId}`, {
          actorId: actor.uid,
          action: existing ? "edit" : "publish",
          revision: item.revision,
          at: now(),
        });
        return { id, revision: item.revision };
      });
    }
    if (operation === "deactivateEntry") {
      return mutate(actor, operation, data, async (tx) => {
        const ref = `wood_wastes/${safeId(data.id)}`, item = await tx.get(ref);
        requireThat(
          item?.authorId === actor.uid,
          403,
          "owner",
          "Solo puedes retirar tus publicaciones.",
        );
        requireThat(
          ["HALLAZGO", "DISPONIBLE"].includes(item.status),
          409,
          "reserved",
          "Cancela la reserva antes de retirar la publicación.",
        );
        tx.set(ref, {
          ...item,
          status: "INACTIVO",
          revision: (item.revision ?? 0) + 1,
          updatedAt: now(),
        });
        return { ok: true };
      });
    }
    if (operation === "createWithdrawal") {
      return mutate(actor, operation, data, async (tx, profile) => {
        requireThat(
          ["RECEPTOR", "MIXTO"].includes(profile.role),
          403,
          "role",
          "Tu perfil no puede solicitar material.",
        );
        const wasteId = safeId(data.wasteId),
          item = await tx.get(`wood_wastes/${wasteId}`);
        requireThat(
          item?.status === "DISPONIBLE" && item.authorId !== actor.uid &&
            item.entryType !== "HALLAZGO",
          409,
          "unavailable",
          "Este material no está disponible para solicitarlo.",
        );
        const id = safeId(data.id), ref = `withdrawal_requests/${id}`;
        requireThat(
          !await tx.get(ref),
          409,
          "exists",
          "La solicitud ya existe.",
        );
        const claimRef = `withdrawal_claims/${await hash(
          `${wasteId}:${actor.uid}`,
        )}`;
        const claim = await tx.get(claimRef);
        requireThat(
          !claim?.active,
          409,
          "duplicate",
          "Ya tienes una solicitud activa para este material.",
        );
        const proposedDate = text(data.proposedDate, 10);
        requireThat(
          /^\d{4}-\d{2}-\d{2}$/.test(proposedDate) &&
            !isNaN(Date.parse(proposedDate)) &&
            new Date(proposedDate).toISOString().slice(0, 10) === proposedDate,
          400,
          "date",
          "Revisa la fecha propuesta.",
        );
        const request = {
          id,
          wasteId,
          authorId: item.authorId,
          wasteTitle: item.title,
          requesterId: actor.uid,
          requesterName: profile.name,
          requesterPhone: "",
          requesterRole: profile.role,
          intendedUse: text(data.intendedUse, 500),
          proposedDate,
          requestedQuantity: text(data.requestedQuantity, 100),
          message: text(data.message ?? "", 1000, true),
          status: "PENDIENTE",
          createdAt: now(),
          updatedAt: now(),
          passValidatedAt: null,
          authorConfirmedAt: null,
          requesterConfirmedAt: null,
          completedAt: null,
        };
        tx.set(ref, request);
        tx.set(claimRef, { active: true, requestId: id });
        await event(tx, id, data.operationId, actor, "request");
        return { id };
      });
    }
    if (operation === "transitionWithdrawal") {
      return mutate(actor, operation, data, async (tx) => {
        const requestId = safeId(data.requestId),
          ref = `withdrawal_requests/${requestId}`,
          request = await tx.get(ref);
        requireThat(
          participant(request, actor.uid),
          403,
          "participant",
          "Esta solicitud no pertenece a tu cuenta.",
        );
        const itemRef = `wood_wastes/${request.wasteId}`,
          item = await tx.get(itemRef);
        requireThat(
          item,
          404,
          "missing",
          "La publicación ya no está disponible.",
        );
        const owner = request.authorId === actor.uid,
          updated = { ...request, updatedAt: now() };
        if (data.action === "accept") {
          requireThat(
            owner && request.status === "PENDIENTE" &&
              item.status === "DISPONIBLE",
            409,
            "transition",
            "La solicitud ya no se puede aceptar.",
          );
          updated.status = "ACEPTADA";
          tx.set(itemRef, {
            ...item,
            status: "RESERVADO",
            reservedBy: request.requesterId,
            revision: (item.revision ?? 0) + 1,
            updatedAt: now(),
          });
        } else if (data.action === "reject") {
          requireThat(
            owner && request.status === "PENDIENTE",
            409,
            "transition",
            "La solicitud ya cambió.",
          );
          updated.status = "RECHAZADA";
        } else if (data.action === "cancel") {
          requireThat(
            ["PENDIENTE", "ACEPTADA"].includes(request.status),
            409,
            "transition",
            "Esta solicitud ya está cerrada.",
          );
          updated.status = "CANCELADA";
          updated.activePassHash = null;
          if (request.status === "ACEPTADA") {
            requireThat(
              item.status === "RESERVADO" &&
                item.reservedBy === request.requesterId,
              409,
              "transition",
              "La reserva ya cambió.",
            );
            tx.set(itemRef, {
              ...item,
              status: "DISPONIBLE",
              reservedBy: null,
              revision: (item.revision ?? 0) + 1,
              updatedAt: now(),
            });
          }
        } else if (data.action === "confirm" || data.action === "complete") {
          requireThat(
            request.status === "ACEPTADA" &&
              Number.isFinite(request.passValidatedAt) &&
              item.status === "RESERVADO" &&
              item.reservedBy === request.requesterId,
            409,
            "pass",
            "Valida el pase de retiro antes de confirmar.",
          );
          updated[owner ? "authorConfirmedAt" : "requesterConfirmedAt"] ??=
            now();
          if (updated.authorConfirmedAt && updated.requesterConfirmedAt) {
            updated.status = "COMPLETADA";
            updated.completedAt = now();
            updated.receipt = {
              id: requestId,
              title: item.title,
              quantity: item.quantityValue,
              unit: item.quantityUnit,
              authorName: item.authorName,
              requesterName: request.requesterName,
              completedAt: now(),
              label: "Comprobante de entrega",
            };
            tx.set(itemRef, {
              ...item,
              status: "APROVECHADO",
              completedBy: request.requesterId,
              completedAt: now(),
              revision: (item.revision ?? 0) + 1,
              updatedAt: now(),
            });
          }
        } else throw new Fault(400, "action", "Acción no válida.");
        if (["RECHAZADA", "CANCELADA", "COMPLETADA"].includes(updated.status)) {
          tx.set(
            `withdrawal_claims/${await hash(
              `${request.wasteId}:${request.requesterId}`,
            )}`,
            { active: false, requestId },
          );
        }
        tx.set(ref, updated);
        await event(tx, requestId, data.operationId, actor, data.action);
        return { ok: true, status: updated.status };
      });
    }
    if (operation === "issuePass") {
      const requestId = safeId(data.requestId),
        code = await passCode(
          `${actor.uid}:${requestId}:${safeId(data.operationId)}`,
        );
      const digest = await hash(`${requestId}:${code}`);
      const result = await mutate(actor, operation, data, async (tx) => {
        const ref = `withdrawal_requests/${requestId}`,
          request = await tx.get(ref);
        requireThat(
          request?.requesterId === actor.uid && request.status === "ACEPTADA" &&
            !request.passValidatedAt,
          403,
          "pass",
          "No puedes emitir este pase.",
        );
        requireThat(
          !request.passIssuedAt || now() - request.passIssuedAt >= 10000,
          429,
          "rate",
          "Espera unos segundos antes de emitir otro pase.",
        );
        const expiresAt = now() + 600000;
        tx.set(ref, {
          ...request,
          passExpiresAt: expiresAt,
          passIssuedAt: now(),
        });
        tx.set(`withdrawal_passes/${requestId}`, {
          hash: digest,
          expiresAt,
          attempts: 0,
          lockedUntil: 0,
        });
        return { requestId, expiresAt };
      });
      return { ...result, code, token: `${requestId}:${code}` };
    }
    if (operation === "redeemPass") {
      const requestId = safeId(data.requestId), code = text(data.code, 8);
      requireThat(
        /^\d{8}$/.test(code),
        400,
        "pass",
        "El código debe tener ocho dígitos.",
      );
      const digest = await hash(`${requestId}:${code}`);
      const result = await mutate(actor, operation, data, async (tx) => {
        const ref = `withdrawal_requests/${requestId}`,
          request = await tx.get(ref);
        const passRef = `withdrawal_passes/${requestId}`,
          pass = await tx.get(passRef);
        requireThat(
          request?.authorId === actor.uid,
          403,
          "pass",
          "Este pase no corresponde a una entrega tuya.",
        );
        requireThat(
          request.status === "ACEPTADA" && !request.passValidatedAt &&
            pass?.expiresAt > now(),
          409,
          "pass",
          "Pase vencido, utilizado o no disponible.",
        );
        requireThat(
          !(pass.lockedUntil > now()),
          429,
          "pass",
          "Demasiados intentos. Espera diez minutos.",
        );
        if (pass.hash !== digest) {
          const attempts = (pass.attempts ?? 0) + 1;
          tx.set(passRef, {
            ...pass,
            attempts,
            lockedUntil: attempts >= 5 ? now() + 600000 : 0,
          });
          return { invalid: true }; // Commit the failed attempt before returning the error.
        }
        tx.set(ref, { ...request, passValidatedAt: now(), updatedAt: now() });
        tx.set(passRef, { ...pass, hash: null, usedAt: now() });
        await event(tx, requestId, data.operationId, actor, "validate-pass");
        return { ok: true };
      });
      requireThat(
        !result.invalid,
        400,
        "pass",
        "Código incorrecto. Comprueba el pase de la otra persona.",
      );
      return result;
    }
    throw new Fault(404, "operation", "Operación no disponible.");
  }
  return { call };
}
