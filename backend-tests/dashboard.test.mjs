import test from "node:test";
import assert from "node:assert/strict";
import {
  createHandler,
  summarize,
} from "../supabase/functions/woodmatch-dashboard/metrics.mjs";
const rows = [
  {
    id: "1",
    type: "INCIDENCIA",
    status: "RESUELTO",
    priority: "ALTA",
    createdAt: 100,
    reporterId: "PRIVATE",
    subject: "PRIVATE",
    history: [{
      at: 110,
      action: "REABIERTO",
      actorId: "PRIVATE",
      reason: "PRIVATE",
    }],
  },
  {
    id: "2",
    type: "INCIDENCIA",
    status: "NUEVO",
    priority: "CRITICA",
    serviceId: "ST-04",
    createdAt: 150,
  },
  { id: "3", type: "SATISFACCION", rating: 5, createdAt: 100 },
  { id: "4", type: "MODERACION", moderationAction: "OCULTAR", createdAt: 100 },
  { id: "5", type: "INCIDENCIA", status: "CERRADO", createdAt: 200 },
];
test("cohort excludes exclusive end; incidents separate from ratings/moderation; minimization", () => {
  const m = summarize(rows, 90, 200);
  assert.equal(m.total, 4);
  assert.equal(m.resolutionRate, 50);
  assert.equal(m.reopenRate, 50);
  assert.equal(m.urgent, 1);
  assert.equal(m.satisfaction, 5);
  assert.equal(m.ratingCount, 1);
  assert.equal(m.moderation.handled, 1);
  assert.equal(m.firstResponseMs, null);
  assert.ok(!JSON.stringify(m).includes("PRIVATE"));
  assert.equal(summarize(rows, 90, 200, "ST-04").total, 1);
});
test("empty cohort and invalid ranges never imply 100 percent", () => {
  assert.equal(summarize([], 1, 2).resolutionRate, null);
  assert.throws(() => summarize([], 2, 1));
  assert.throws(() => summarize([], 1, 2, "ST-99"));
});
const origin = "https://dashboard.example.invalid";
const deps = {
  origins: [origin],
  clock: () => 1000,
  verify: async () => ({
    sub: "reader",
    auth_time: 1,
    exp: 9999,
    firebase: { sign_in_provider: "password" },
  }),
  lookup: async () => ({ validSince: 0 }),
  authorize: (uid) => uid === "reader",
  quota: async () => true,
  read: async () => rows,
  audit: () => {},
};
const req = (extra = {}, method = "GET", url = "?from=90&to=200") =>
  new Request("https://api.example.invalid/" + url, {
    method,
    headers: { Origin: origin, Authorization: "Bearer fixture", ...extra },
  });
test("protected endpoint denies no token, wrong origin, revoked/disabled/anonymous/expired and non-allowlisted users", async () => {
  for (
    const [change, request, status] of [
      [{}, req({ Authorization: "" }), 401],
      [{}, req({ Origin: "https://evil.invalid" }), 403],
      [
        {
          verify: async () => {
            throw Error();
          },
        },
        req(),
        401,
      ],
      [
        { verify: async () => ({ sub: "reader", auth_time: 1, exp: 0 }) },
        req(),
        401,
      ],
      [
        {
          verify: async () => ({
            sub: "reader",
            auth_time: 1,
            exp: 9999,
            firebase: { sign_in_provider: "anonymous" },
          }),
        },
        req(),
        401,
      ],
      [{ lookup: async () => ({ disabled: true }) }, req(), 401],
      [{ lookup: async () => ({ validSince: 2 }) }, req(), 401],
      [{ authorize: () => false }, req(), 403],
      [{ quota: async () => false }, req(), 429],
      [{}, req({}, "POST"), 405],
      [{}, req({}, "GET", "?from=2&to=1"), 400],
    ]
  ) {
    assert.equal(
      (await createHandler({ ...deps, ...change })(request)).status,
      status,
    );
  }
});
test("success is read-only, no-store and logs exclude identity and token", async () => {
  const audit = [];
  const response = await createHandler({
    ...deps,
    audit: (e) => audit.push(e),
  })(req());
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("Cache-Control"), "no-store");
  assert.equal((await response.json()).total, 4);
  assert.deepEqual(Object.keys(audit[0]).sort(), ["at", "status"]);
});
