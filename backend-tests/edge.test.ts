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
let providerStatus = 200;
let providerErrorCode = "UNKNOWN";
let providerFailure = "";
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
  if (url.endsWith(":batchGet")) {
    const body = JSON.parse(String(init?.body));
    return Response.json(
      body.documents.map((name: string) =>
        quotaCount
          ? {
            found: {
              name,
              fields: encode({ count: quotaCount }).mapValue.fields,
            },
          }
          : { missing: name }
      ),
    );
  }
  if (url.endsWith(":beginTransaction")) {
    return Response.json({ transaction: "test-transaction" });
  }
  if (url.endsWith(":commit") || url.endsWith(":rollback")) {
    return Response.json({});
  }
  if (url.includes("generativelanguage.googleapis.com")) {
    if (providerFailure === "timeout") throw new DOMException("fixture-key", "TimeoutError");
    if (providerFailure === "network") throw new Error("fixture-key/private-personal-query");
    if (providerFailure === "invalid-response") return new Response("fixture-key");
    if (providerStatus !== 200) {
      return Response.json({ error: { status: providerErrorCode, message: "fixture-key/private-personal-query" } }, { status: providerStatus });
    }
    if (providerQuota) return Response.json({ error: "rate" }, { status: 429 });
    const body = JSON.parse(String(init?.body));
    if (!body.systemInstruction) throw new Error("Missing instruction");
    return Response.json({
      candidates: [{
        content: {
          parts: [{
            text: body.generationConfig?.responseMimeType === "application/json"
              ? JSON.stringify({
                title: "Tablas recuperables",
                material: "Tablas",
                condition: "Secas, requieren revisión",
                description: "Madera recuperable visible.",
                quantityValue: 2,
                quantityUnit: "unidades",
                confidence: "media",
              })
              : "Orientación de prueba",
          }],
        },
      }],
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
  quotaCount = 6;
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

Deno.test("evidence analysis accepts only a bounded JPG and returns bounded suggestions", async () => {
  const auth = await token();
  const jpg = btoa("\u00ff\u00d8\u00ff\u0000evidence");
  const response = await call("analyzeEvidence", { image: jpg }, auth);
  equal(response.status, 200);
  const payload = await response.json();
  equal(payload.data.material, "Tablas");
  equal(payload.data.quantityUnit, "unidades");
  equal(payload.data.quantityValue, 2);
  equal(
    (await call("analyzeEvidence", { image: btoa("not-a-jpeg") }, auth)).status,
    400,
  );
});

Deno.test("provider errors expose safe actionable codes without upstream private content", async () => {
  const auth = await token();
  const cases = [
    [401, "UNAUTHENTICATED", "provider_credentials", 503],
    [403, "PERMISSION_DENIED", "provider_credentials", 503],
    [404, "NOT_FOUND", "provider_model", 503],
    [429, "RESOURCE_EXHAUSTED", "provider_quota", 429],
    [400, "INVALID_ARGUMENT", "provider_configuration", 503],
    [503, "UNAVAILABLE", "provider_unavailable", 503],
    [400, "fixture-key", "provider_configuration", 503],
  ] as const;
  try {
    for (const [httpStatus, upstreamCode, expectedCode, expectedStatus] of cases) {
      providerStatus = httpStatus;
      providerErrorCode = upstreamCode;
      const response = await call("botanicalAdvice", { prompt: "consulta" }, auth);
      equal(response.status, expectedStatus);
      const body = await response.json();
      equal(body.error.code, expectedCode);
      const lastLog = JSON.parse(logs.at(-1)!);
      equal(lastLog.provider, "gemini");
      equal(lastLog.providerStatus, httpStatus);
      equal(lastLog.providerCode, upstreamCode === "fixture-key" ? "UNKNOWN" : upstreamCode);
      if (JSON.stringify(body).includes("fixture-key")) throw new Error("Provider body leaked");
    }
  } finally {
    providerStatus = 200;
    providerErrorCode = "UNKNOWN";
  }
  for (const log of logs) {
    if (log.includes("fixture-key") || log.includes("private-personal-query") ||
      log.includes("generativelanguage.googleapis.com")) throw new Error("Sensitive provider log");
  }
});
Deno.test("provider network, timeout and invalid JSON responses are classified safely", async () => {
  const auth = await token();
  try {
    for (const [failure, expectedCode, expectedStatus] of [
      ["network", "provider_network", 503],
      ["timeout", "provider_timeout", 504],
      ["invalid-response", "provider_response", 503],
    ] as const) {
      providerFailure = failure;
      const response = await call("botanicalAdvice", { prompt: "consulta" }, auth);
      equal(response.status, expectedStatus);
      equal((await response.json()).error.code, expectedCode);
    }
  } finally {
    providerFailure = "";
  }
});


