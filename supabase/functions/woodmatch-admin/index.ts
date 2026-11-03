import {
  createRemoteJWKSet,
  importPKCS8,
  jwtVerify,
  SignJWT,
} from "npm:jose@5.9.6";
import { decode, Firestore } from "../woodmatch/firestore.mjs";
import { summarize } from "../woodmatch-dashboard/metrics.mjs";
import { adminOperation } from "./operations.mjs";
import { eraseAccount } from "../woodmatch/cleanup.mjs";
const env = (n: string) => Deno.env.get(n) || "";
const project = env("FIREBASE_PROJECT_ID");
const jwks = createRemoteJWKSet(
  new URL(
    "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com",
  ),
);
let cache: { token: string; until: number } | null = null;
async function accessToken() {
  if (cache && cache.until > Date.now()) return cache.token;
  const account = JSON.parse(env("FIREBASE_SERVICE_ACCOUNT"));
  if (!project || account.project_id !== project) throw Error("configuration");
  const jwt = await new SignJWT({
    scope:
      "https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/identitytoolkit",
  })
    .setProtectedHeader({ alg: "RS256" }).setIssuer(account.client_email)
    .setAudience("https://oauth2.googleapis.com/token").setIssuedAt()
    .setExpirationTime("1h")
    .sign(await importPKCS8(account.private_key, "RS256"));
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    signal: AbortSignal.timeout(15000),
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  if (!res.ok) throw Error("oauth");
  const body = await res.json();
  cache = { token: body.access_token, until: Date.now() + 3300000 };
  return cache.token;
}
const db = new Firestore(project, accessToken);

async function identity(action: string, body: object) {
  const res = await fetch(
    `https://identitytoolkit.googleapis.com/v1/projects/${project}/accounts:${action}`,
    {
      method: "POST",
      signal: AbortSignal.timeout(20000),
      headers: {
        Authorization: `Bearer ${await accessToken()}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    },
  );
  if (!res.ok) {
    throw Object.assign(
      Error("No se pudo completar la operación de identidad."),
      { status: 503 },
    );
  }
  return res.json();
}
async function all(collection: string) {
  const rows: any[] = [];
  let after = null;
  for (let page = 0; page < 20; page++) {
    const batch = await db.list(collection, null, null, 500, after);
    rows.push(...batch);
    if (batch.length < 500) return rows;
    after = batch.at(-1)._path;
  }
  throw Object.assign(Error("La consulta excede la capacidad configurada."), {
    status: 503,
  });
}
async function profiles() {
  const rows = await db.request(`${db.root}:runQuery`, {
    structuredQuery: { from: [{ collectionId: "users" }], limit: 501 },
  });
  const docs = rows.filter((r: any) => r.document);
  if (docs.length > 500) {
    throw Object.assign(Error("La consulta excede la capacidad configurada."), {
      status: 503,
    });
  }
  return docs.map((r: any) => ({
    ...decode({ mapValue: { fields: r.document.fields } }),
    version: r.document.updateTime,
  }));
}
async function isAdministrator(uid: string) {
  const ids = env("WOODMATCH_ADMIN_UIDS").split(",").filter(Boolean);
  return ids.length <= 2 && ids.includes(uid);
}
async function erase(uid: string) {
  await eraseAccount({
    uid,
    db,
    identity,
    deletePhotos: async (paths: string[]) => {
      const response = await fetch(
        `${env("SUPABASE_URL")}/storage/v1/object/woodmatch-photos`,
        {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${env("SUPABASE_SERVICE_ROLE_KEY")}`,
            apikey: env("SUPABASE_SERVICE_ROLE_KEY"),
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ prefixes: paths }),
        },
      );
      if (!response.ok) {
        throw Object.assign(
          Error("No se completó la eliminación de fotografías."),
          { status: 503 },
        );
      }
    },
  });
}
Deno.serve(async (request) => {
  const origin = request.headers.get("Origin") || "",
    allowed = env("DASHBOARD_ORIGINS").split(",").filter(Boolean);
  const headers = {
    "Content-Type": "application/json",
    "Cache-Control": "no-store",
    "Vary": "Origin",
    "Access-Control-Allow-Origin": allowed.includes(origin) ? origin : "",
    "Access-Control-Allow-Headers": "Authorization, Content-Type",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
  };
  let status = 500;
  try {
    if (!allowed.includes(origin)) {
      status = 403;
      throw Error("Origen no permitido.");
    }
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers });
    }
    if (request.method !== "POST") {
      status = 405;
      throw Error("Método no disponible.");
    }
    const bearer = request.headers.get("Authorization") || "";
    if (!/^Bearer [^ ]+$/.test(bearer) || bearer.length > 8192) {
      status = 401;
      throw Error("Inicia sesión.");
    }
    let payload;
    try {
      payload = (await jwtVerify(bearer.slice(7), jwks, {
        issuer: `https://securetoken.google.com/${project}`,
        audience: project,
        algorithms: ["RS256"],
      })).payload;
    } catch {
      status = 401;
      throw Error("Sesión vencida. Vuelve a iniciar sesión.");
    }
    if (
      !payload.sub || !Number.isFinite(payload.exp) ||
      !Number.isFinite(payload.auth_time) ||
      (payload.firebase as any)?.sign_in_provider === "anonymous"
    ) {
      status = 401;
      throw Error("Sesión no válida.");
    }
    const account = (await identity("lookup", { localId: [payload.sub] })).users
      ?.[0];
    if (
      !account || account.disabled ||
      Number(payload.auth_time) < Number(account.validSince || 0)
    ) {
      status = 401;
      throw Error("Sesión revocada.");
    }
    if (!await isAdministrator(payload.sub)) {
      status = 403;
      throw Error("Tu cuenta no tiene acceso al centro de administración.");
    }
    const now = Date.now();
    await db.atomic(async (tx: any) => {
      // One bounded quota document per administrator, without perpetual minute-row growth.
      const path = "administration_limits/" + payload.sub,
        row = await tx.get(path),
        window = Math.floor(now / 60000),
        count = row?.window === window ? row.count : 0;
      if (count >= 30) {
        throw Object.assign(
          Error("Espera un minuto antes de volver a consultar."),
          { status: 429 },
        );
      }
      tx.set(path, { window, count: count + 1 });
    });
    const reader = request.body?.getReader();
    if (!reader) {
      status = 400;
      throw Error("Faltan datos.");
    }
    let length = 0;
    const chunks: Uint8Array[] = [];
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      length += value.length;
      if (length > 16384) {
        await reader.cancel();
        status = 413;
        throw Error("Solicitud demasiado grande.");
      }
      chunks.push(value);
    }
    const bytes = new Uint8Array(length);
    let offset = 0;
    for (const c of chunks) {
      bytes.set(c, offset);
      offset += c.length;
    }
    let input;
    try {
      input = JSON.parse(new TextDecoder().decode(bytes));
    } catch {
      status = 400;
      throw Error("Solicitud no válida.");
    }
    if (
      !input || typeof input.operation !== "string" || !input.data ||
      typeof input.data !== "object" || Array.isArray(input.data)
    ) {
      status = 400;
      throw Error("Solicitud no válida.");
    }
    let result;
    if (input.operation === "summary") {
      try {
        result = summarize(
          await all("support_cases"),
          input.data.from,
          input.data.to,
          input.data.service || "",
        );
      } catch (error) {
        if ((error as Error).message === "range") {
          throw Object.assign(Error("Selecciona un periodo válido."), {
            status: 400,
          });
        }
        throw error;
      }
    } else {result = await adminOperation(
        {
          db: Object.assign(db, { all, profiles }),
          identity,
          erase,
          isAdministrator,
        },
        { uid: payload.sub, auth_time: payload.auth_time, webAdmin: true },
        input.operation,
        input.data,
      );}
    status = 200;
    return Response.json({ data: result }, { headers });
  } catch (error) {
    const e = error as any;
    status = e.status === "FAILED_PRECONDITION"
      ? 409
      : typeof e.status === "number"
      ? e.status
      : status;
    return Response.json({
      error: status === 500 ? "No se pudo completar la operación." : e.message,
    }, { status, headers });
  } finally {
    console.log(
      JSON.stringify({ component: "administration", status, at: Date.now() }),
    );
  }
});
