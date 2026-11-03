import {
  exportJWK,
  exportPKCS8,
  generateKeyPair,
  SignJWT,
} from "npm:jose@5.9.6";

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
const logs: string[] = [];
console.log = (value) => logs.push(String(value));
let disabled = false, revoked = false;
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
  if (url.endsWith(":runQuery")) return Response.json([]);
  if (url.endsWith(":batchGet")) {
    const body = JSON.parse(String(init?.body));
    return Response.json(
      body.documents.map((name: string) => ({ missing: name })),
    );
  }
  if (url.endsWith(":beginTransaction")) {
    return Response.json({ transaction: "test-transaction" });
  }
  if (url.endsWith(":commit") || url.endsWith(":rollback")) {
    return Response.json({});
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
Deno.env.set("WOODMATCH_ADMIN_UIDS", "fixture");
Deno.env.set("DASHBOARD_ORIGINS", "https://dashboard.example.invalid");
await import("../supabase/functions/woodmatch-admin/index.ts");
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
  auth?: string,
  origin = "https://dashboard.example.invalid",
) {
  return handler(
    new Request("https://example.invalid/admin", {
      method: "POST",
      body: JSON.stringify({
        operation: "summary",
        data: { from: 1, to: 200 },
      }),
      headers: {
        Origin: origin,
        ...(auth ? { Authorization: `Bearer ${auth}` } : {}),
      },
    }),
  );
}
function equal(actual: unknown, expected: unknown) {
  if (actual !== expected) {
    throw Error(`Expected ${expected}; received ${actual}`);
  }
}
Deno.test("admin verifies signed Firebase JWT and returns a minimized empty real cohort", async () => {
  const response = await call(await token());
  equal(response.status, 200);
  equal((await response.json()).data.source, "real");
});
Deno.test("admin rejects missing, forged, expired, wrong-project and anonymous tokens", async () => {
  equal((await call()).status, 401);
  equal((await call("forged")).status, 401);
  for (
    const options of [{ expired: true }, {
      issuer: "https://securetoken.google.com/wrong",
    }, { anonymous: true }]
  ) equal((await call(await token(options))).status, 401);
});
Deno.test("admin rejects disabled and revoked accounts before returning data", async () => {
  try {
    disabled = true;
    equal((await call(await token())).status, 401);
    disabled = false;
    revoked = true;
    equal((await call(await token())).status, 401);
  } finally {
    disabled = false;
    revoked = false;
  }
});
Deno.test("admin enforces server allowlist and exact CORS origin", async () => {
  const auth = await token();
  Deno.env.set("WOODMATCH_ADMIN_UIDS", "");
  try {
    equal((await call(auth)).status, 403);
  } finally {
    Deno.env.set("WOODMATCH_ADMIN_UIDS", "fixture");
  }
  equal((await call(auth, "https://evil.invalid")).status, 403);
});
