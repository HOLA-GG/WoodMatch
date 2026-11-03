import {
  createRemoteJWKSet,
  importPKCS8,
  jwtVerify,
  SignJWT,
} from "npm:jose@5.9.6";
import { Firestore } from "../woodmatch/firestore.mjs";
import { createHandler } from "./metrics.mjs";
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
Deno.serve(createHandler({
  origins: env("DASHBOARD_ORIGINS").split(",").filter(Boolean),
  verify: async (token: string) =>
    (await jwtVerify(token, jwks, {
      issuer: `https://securetoken.google.com/${project}`,
      audience: project,
      algorithms: ["RS256"],
    })).payload,
  lookup: async (uid: string) => {
    const response = await fetch(
      `https://identitytoolkit.googleapis.com/v1/projects/${project}/accounts:lookup`,
      {
        method: "POST",
        signal: AbortSignal.timeout(15000),
        headers: {
          Authorization: `Bearer ${await accessToken()}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ localId: [uid] }),
      },
    );
    if (!response.ok) throw Error("identity");
    return (await response.json()).users?.[0];
  },
  authorize: (uid: string) =>
    env("DASHBOARD_READER_UIDS").split(",").filter(Boolean).includes(uid),
  quota: async (uid: string) =>
    db.atomic(async (tx: any) => {
      const minute = Math.floor(Date.now() / 60000),
        path = `dashboard_limits/${uid}_${minute}`;
      const row = await tx.get(path);
      if ((row?.count || 0) >= 10) return false;
      tx.set(path, {
        count: (row?.count || 0) + 1,
        expiresAt: Date.now() + 86400000,
      });
      return true;
    }),
  read: async () => {
    const rows: any[] = [];
    let after = null;
    for (let page = 0; page < 20; page++) {
      const batch = await db.list("support_cases", null, null, 500, after);
      rows.push(...batch);
      if (batch.length < 500) return rows;
      after = batch.at(-1)._path;
    }
    // Never publish a silently truncated KPI as complete.
    throw Error("capacity");
  },
  audit: (event: unknown) =>
    console.log(JSON.stringify({ component: "dashboard", ...event as object })),
}));
