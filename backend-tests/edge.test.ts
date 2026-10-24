import {
  exportJWK,
  exportPKCS8,
  generateKeyPair,
  SignJWT,
} from "npm:jose@5.9.6";
import { encode } from "../supabase/functions/woodmatch/firestore.mjs";

const { privateKey, publicKey } = await generateKeyPair("RS256", {
  extractable: true,
});
const jwk = {
  ...await exportJWK(publicKey),
  kid: "test-key",
  alg: "RS256",
  use: "sig",
};
Deno.env.set("FIREBASE_PROJECT_ID", "demo-woodmatch-pilot");
Deno.env.set(
  "FIREBASE_SERVICE_ACCOUNT",
  JSON.stringify({
    project_id: "demo-woodmatch-pilot",
    client_email: "fixture@example.invalid",
    private_key: await exportPKCS8(privateKey),
  }),
);
Deno.env.set("PASS_SECRET", "fixture-pass-secret-at-least-32-characters");
Deno.env.set("GEMINI_SERVER_KEY", "fixture-key");
const logs: string[] = [];
const realLog = console.log;
console.log = (value) => logs.push(String(value));
const realFetch = globalThis.fetch;
let disabled = false, revoked = false, providerQuota = false;
let quotaCount = 0;
globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
  const url = String(input instanceof Request ? input.url : input);
  if (url.includes("service_accounts/v1/jwk")) {
    return Response.json({ keys: [jwk] });
  }
  if (url === "https://oauth2.googleapis.com/token") {
    return Response.json({ access_token: "test-access-token" });
  }
  if (url.includes("accounts:lookup")) {
    return Response.json({
      users: [{
        localId: "fixture",
        disabled,
        validSince: revoked ? String(Math.floor(Date.now() / 1000) + 100) : "0",
      }],
    });
  }
  if (url.endsWith("/users/fixture")) {
    return Response.json({
      fields:
        encode({ uid: "fixture", name: "Fixture", role: "MIXTO" }).mapValue
          .fields,
    });
  }
  if (url.includes("/account_tombstones/")) {
    return new Response("{}", { status: 404 });
  }
  if (url.includes("/ai_quotas/")) {
    return quotaCount
      ? Response.json({ fields: encode({ count: quotaCount }).mapValue.fields })
      : new Response("{}", { status: 404 });
  }
  if (url.endsWith(":beginTransaction")) {
    return Response.json({ transaction: "test-transaction" });
  }
  if (url.endsWith(":commit") || url.endsWith(":rollback")) {
    return Response.json({});
  }
  if (url.includes("generativelanguage.googleapis.com")) {
    if (providerQuota) return Response.json({ error: "rate" }, { status: 429 });
    const body = JSON.parse(String(init?.body));
    if (!body.systemInstruction) throw new Error("Missing instruction");
    return Response.json({
      candidates: [{ content: { parts: [{ text: "Orientación de prueba" }] } }],
    });
  }
  throw new Error("Unexpected fixture endpoint");
}) as typeof fetch;
let handler: (request: Request) => Promise<Response>;
const realServe = Deno.serve;
Object.defineProperty(Deno, "serve", {
  value: (fn: typeof handler) => {
    handler = fn;
    return {};
  },
  configurable: true,
});
await import("../supabase/functions/woodmatch/index.ts");
Object.defineProperty(Deno, "serve", { value: realServe, configurable: true });

async function token(
  options: { issuer?: string; anonymous?: boolean; expired?: boolean } = {},
) {
  return new SignJWT({
    auth_time: Math.floor(Date.now() / 1000),
    firebase: {
      sign_in_provider: options.anonymous ? "anonymous" : "password",
    },
  })
    .setProtectedHeader({ alg: "RS256", kid: "test-key" }).setSubject("fixture")
    .setIssuer(
      options.issuer || "https://securetoken.google.com/demo-woodmatch-pilot",
    )
    .setAudience("demo-woodmatch-pilot").setIssuedAt().setExpirationTime(
      options.expired ? Math.floor(Date.now() / 1000) - 60 : "5m",
    ).sign(privateKey);
}
async function call(
  operation: string,
  data: unknown = {},
  authorization?: string,
) {
  return handler(
    new Request("https://example.invalid/woodmatch", {
      method: "POST",
      headers: authorization
        ? { Authorization: `Bearer ${authorization}` }
        : {},
      body: JSON.stringify({ operation, data }),
    }),
  );
}
function equal(actual: unknown, expected: unknown) {
  if (actual !== expected) {
    throw new Error(`Expected ${expected}; received ${actual}`);
  }
}
Deno.test("unauthenticated, forged, expired, wrong-project and anonymous tokens are rejected", async () => {
  equal((await call("botanicalAdvice")).status, 401);
  equal((await call("botanicalAdvice", {}, "invalid")).status, 401);
  for (
    const options of [{ issuer: "https://securetoken.google.com/wrong" }, {
      expired: true,
    }, { anonymous: true }]
  ) {
    equal(
      (await call("botanicalAdvice", {}, await token(options))).status,
      401,
    );
  }
});
Deno.test("disabled or revoked accounts cannot reach provider or data operations", async () => {
  disabled = true;
  equal(
    (await call("botanicalAdvice", { prompt: "consulta" }, await token()))
      .status,
    401,
  );
  disabled = false;
  revoked = true;
  equal(
    (await call("botanicalAdvice", { prompt: "consulta" }, await token()))
      .status,
    401,
  );
  revoked = false;
});
Deno.test("server advice requires authentication and maps quota errors to useful responses", async () => {
  const auth = await token();
  const res = await call("botanicalAdvice", { prompt: "consulta" }, auth);
  equal(res.status, 200);
  equal((await res.json()).data.text, "Orientación de prueba");
  quotaCount = 5;
  equal(
    (await call("botanicalAdvice", { prompt: "consulta" }, auth)).status,
    429,
  );
  quotaCount = 0;
  providerQuota = true;
  equal(
    (await call("botanicalAdvice", { prompt: "consulta" }, auth)).status,
    429,
  );
  providerQuota = false;
});
Deno.test("unknown operations and private content do not leak into structured logs", async () => {
  const secret = "private-personal-query";
  equal((await call(secret, {}, await token())).status, 400);
  for (const log of logs) {
    const row = JSON.parse(log);
    equal(typeof row.correlationId, "string");
    if (
      log.includes(secret) || log.includes("fixture-key") ||
      log.includes("test-access-token")
    ) throw new Error("Sensitive log");
  }
});
Deno.test("payload validation rejects oversized advice and invalid photos before provider calls", async () => {
  const auth = await token();
  equal(
    (await call("botanicalAdvice", { prompt: "x".repeat(2001) }, auth)).status,
    400,
  );
  equal(
    (await call("uploadPhoto", {
      id: "photo",
      operationId: "upload",
      image: btoa("not-a-jpeg"),
    }, auth)).status,
    400,
  );
});
