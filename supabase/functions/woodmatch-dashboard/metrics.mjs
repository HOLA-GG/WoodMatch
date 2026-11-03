export const services = Array.from(
  { length: 15 },
  (_, i) => `ST-${String(i + 1).padStart(2, "0")}`,
);
const closed = (r) => ["RESUELTO", "CERRADO"].includes(r.status);
export function summarize(rows, from, to, service = "") {
  if (
    !Number.isFinite(from) || !Number.isFinite(to) || from >= to ||
    to - from > 366 * 86400000 || (service && !services.includes(service))
  ) throw new Error("range");
  const valid = rows.filter((r) =>
    Number.isFinite(r.createdAt) && r.createdAt >= from && r.createdAt < to
  );
  const cohort = valid.filter((r) => !service || r.serviceId === service);
  const incidents = cohort.filter((r) => r.type === "INCIDENCIA");
  const moderation = cohort.filter((r) => r.type === "MODERACION");
  const ratings = cohort.filter((r) =>
    r.type === "SATISFACCION" && Number.isInteger(r.rating) && r.rating >= 1 &&
    r.rating <= 5
  );
  const resolved = incidents.filter(closed).length;
  const reopened =
    incidents.filter((r) => r.history?.some((e) => e.action === "REABIERTO"))
      .length;
  return {
    source: "real",
    from,
    to,
    total: cohort.length,
    incidents: incidents.length,
    resolved,
    open: incidents.filter((r) => !closed(r)).length,
    urgent:
      incidents.filter((r) =>
        !closed(r) && ["ALTA", "CRITICA"].includes(r.priority)
      ).length,
    resolutionRate: incidents.length ? resolved / incidents.length * 100 : null,
    reopenRate: incidents.length ? reopened / incidents.length * 100 : null,
    reopened,
    satisfaction: ratings.length
      ? ratings.reduce((s, r) => s + r.rating, 0) / ratings.length
      : null,
    ratingCount: ratings.length,
    firstResponseMs: null,
    resolutionMs: null,
    moderation: {
      received: moderation.length,
      handled:
        moderation.filter((r) =>
          ["OCULTAR", "RESTAURAR"].includes(r.moderationAction)
        ).length,
    },
    byService: services.map((id) => ({
      id,
      count: cohort.filter((r) => r.serviceId === id).length,
    })),
    unclassified: cohort.filter((r) => !services.includes(r.serviceId)).length,
    cases: cohort.map((r) => ({
      id: r.id,
      type: r.type,
      serviceId: services.includes(r.serviceId) ? r.serviceId : null,
      status: r.status,
      priority: r.priority,
      createdAt: r.createdAt,
      updatedAt: r.updatedAt,
      history: (r.history || []).map((e) => ({ at: e.at, action: e.action })),
    })),
    limitations: [
      "Sin datos suficientes para tiempos de respuesta y resolución: historial limitado y sin evento de respuesta inequívoco.",
      "Reaperturas: mínimo observado en historial retenido; no tasa histórica completa.",
      "Sin telemetría de disponibilidad ni confirmación de entrega push. Asuntos y texto libre excluidos por privacidad.",
    ],
  };
}
export function createHandler(
  { verify, lookup, authorize, quota, read, audit, origins, clock = Date.now },
) {
  return async (request) => {
    const origin = request.headers.get("Origin") || "";
    const headers = {
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
      "Vary": "Origin",
    };
    if (!origins.includes(origin)) {
      return Response.json({ error: "Origen no permitido" }, {
        status: 403,
        headers,
      });
    }
    headers["Access-Control-Allow-Origin"] = origin;
    headers["Access-Control-Allow-Headers"] = "Authorization, Content-Type";
    headers["Access-Control-Allow-Methods"] = "GET, OPTIONS";
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers });
    }
    let status = 500;
    try {
      if (request.method !== "GET") {
        status = 405;
        throw Error();
      }
      const raw = request.headers.get("Authorization") || "";
      if (!/^Bearer [^ ]+$/.test(raw) || raw.length > 8192) {
        status = 401;
        throw Error();
      }
      let actor;
      try {
        actor = await verify(raw.slice(7));
      } catch {
        status = 401;
        throw Error();
      }
      if (
        !actor.sub || actor.firebase?.sign_in_provider === "anonymous" ||
        !Number.isFinite(actor.auth_time) || !Number.isFinite(actor.exp) ||
        actor.exp * 1000 <= clock()
      ) {
        status = 401;
        throw Error();
      }
      const account = await lookup(actor.sub);
      if (
        !account || account.disabled ||
        Number(actor.auth_time) < Number(account.validSince || 0)
      ) {
        status = 401;
        throw Error();
      }
      if (!authorize(actor.sub)) {
        status = 403;
        throw Error();
      }
      if (!await quota(actor.sub)) {
        status = 429;
        throw Error();
      }
      const url = new URL(request.url),
        from = Number(url.searchParams.get("from")),
        to = Number(url.searchParams.get("to"));
      if (
        !url.searchParams.has("from") || !url.searchParams.has("to") ||
        !Number.isFinite(from) || !Number.isFinite(to) || from >= to ||
        to - from > 366 * 86400000
      ) {
        status = 400;
        throw Error();
      }
      const service = url.searchParams.get("service") || "";
      if (service && !services.includes(service)) {
        status = 400;
        throw Error();
      }
      const rows = await read();
      const body = summarize(rows, from, to, service);
      status = 200;
      return Response.json(body, { headers });
    } catch {
      return Response.json({
        error: status === 401
          ? "Sesión no válida"
          : status === 403
          ? "Acceso denegado"
          : status === 429
          ? "Espera un minuto"
          : status === 400
          ? "Periodo no válido"
          : "Servicio no disponible",
      }, { status, headers });
    } finally {
      audit({ status, at: clock() });
    }
  };
}
