import test from "node:test";
import assert from "node:assert/strict";
import { adminOperation } from "../supabase/functions/woodmatch-admin/operations.mjs";
const actor = { uid: "operator", webAdmin: true, auth_time: 10 };
function fixture() {
  const rows = new Map([["users/user", {
    uid: "user",
    name: "Nombre",
    privacyAcceptedAt: { timestampValue: "2026-01-01T00:00:00Z" },
  }], ["support_cases/case", {
    id: "case",
    type: "INCIDENCIA",
    reporterId: "user",
    status: "NUEVO",
    priority: "MEDIA",
    updatedAt: 1,
    history: [],
  }], ["wood_wastes/item", {
    id: "item",
    authorId: "user",
    status: "DISPONIBLE",
    revision: 1,
  }]]);
  const db = {
    get: async (p) => rows.get(p),
    set: async (p, v) => rows.set(p, v),
    all: async (c) =>
      [...rows].filter(([p]) => p.startsWith(c + "/")).map(([, r]) => r),
    profiles: async () => [{ ...rows.get("users/user"), version: "v1" }],
    atomic: async (work) => {
      const writes = [];
      const result = await work({
        get: async (p) => rows.get(p),
        set: (p, v) => writes.push([p, v]),
        patch: (p, v, version) => {
          assert.equal(version, "v1");
          writes.push([p, { ...rows.get(p), ...v }]);
        },
      });
      for (const [p, v] of writes) rows.set(p, v);
      return result;
    },
  };
  return {
    rows,
    dep: {
      db,
      identity: async () => ({ users: [{ localId: "user" }] }),
      erase: async () => {},
      isAdministrator: async (uid) => uid === "operator",
      clock: () => 11000,
    },
  };
}
const data = (id, extra = {}) => ({
  id,
  operationId: "op",
  reason: "Motivo administrativo documentado",
  ...extra,
});
test("untrusted actor, missing reason and role-only identity cannot administer", async () => {
  const { dep } = fixture();
  await assert.rejects(() =>
    adminOperation(dep, { uid: "x", admin: true }, "users", {})
  );
  await assert.rejects(() =>
    adminOperation(dep, actor, "notifyUser", data("user", { reason: "" }))
  );
});
test("case response is audited, version checked, notifies once and records durable first response", async () => {
  const { rows, dep } = fixture(),
    d = data("case", {
      status: "RESUELTO",
      priority: "ALTA",
      response: "Solución registrada",
      expectedUpdatedAt: 1,
    });
  await adminOperation(dep, actor, "updateSupportCase", d);
  await adminOperation(dep, actor, "updateSupportCase", d);
  assert.equal(rows.get("support_cases/case").firstResponseAt, 11000);
  assert.equal(rows.get("administration_audit/op").state, "COMPLETED");
  assert.equal(
    [...rows.keys()].filter((p) => p.startsWith("notifications/")).length,
    1,
  );
  await assert.rejects(() =>
    adminOperation(dep, actor, "updateSupportCase", {
      ...d,
      operationId: "other",
    })
  );
});
test("rename patches only name and preserves timestamp profile fields with audit", async () => {
  const { rows, dep } = fixture();
  await adminOperation(
    dep,
    actor,
    "renameUser",
    data("user", { name: "Nombre permitido", version: "v1" }),
  );
  assert.equal(rows.get("users/user").name, "Nombre permitido");
  assert.equal(
    rows.get("users/user").privacyAcceptedAt.timestampValue,
    "2026-01-01T00:00:00Z",
  );
  assert.equal(rows.get("administration_audit/op").operation, "renameUser");
});
test("publication moderation rejects reserved state and creates reasoned event and author alert", async () => {
  const { rows, dep } = fixture();
  rows.get("wood_wastes/item").status = "RESERVADO";
  await assert.rejects(() =>
    adminOperation(
      dep,
      actor,
      "moderatePublication",
      data("item", { action: "OCULTAR", expectedRevision: 1 }),
    )
  );
  rows.get("wood_wastes/item").status = "DISPONIBLE";
  await adminOperation(
    dep,
    actor,
    "moderatePublication",
    data("item", { action: "OCULTAR", expectedRevision: 1 }),
  );
  assert.equal(rows.get("wood_wastes/item").status, "MODERADO");
  assert.equal(
    rows.get("wood_wastes/item/events/op").reason,
    "Motivo administrativo documentado",
  );
});
test("administrative accounts, stale login and wrong confirmation cannot be deleted", async () => {
  const { dep } = fixture();
  for (
    const [a, id, confirmation] of [[actor, "operator", "operator"], [
      { ...actor, auth_time: -500 },
      "user",
      "user",
    ], [actor, "user", "wrong"]]
  ) {
    await assert.rejects(() =>
      adminOperation(dep, a, "deleteUser", data(id, { confirmation }))
    );
  }
});
test("deletion failure retains STARTED audit and same command can resume to COMPLETED", async () => {
  const { rows, dep } = fixture();
  let fail = true;
  dep.erase = async () => {
    if (fail) throw Error("cleanup retry");
  };
  const d = data("user", { confirmation: "user" });
  await assert.rejects(() => adminOperation(dep, actor, "deleteUser", d));
  assert.equal(rows.get("administration_audit/op").state, "STARTED");
  fail = false;
  await adminOperation(dep, actor, "deleteUser", d);
  assert.equal(rows.get("administration_audit/op").state, "COMPLETED");
});
test("reopen requires resolved state, current revision and auditable reason", async () => {
  const { rows, dep } = fixture();
  rows.get("support_cases/case").status = "CERRADO";
  await adminOperation(
    dep,
    actor,
    "reopenCase",
    data("case", { expectedUpdatedAt: 1 }),
  );
  assert.equal(rows.get("support_cases/case").status, "EN_PROGRESO");
  assert.equal(
    rows.get("support_cases/case").history.at(-1).action,
    "REABIERTO",
  );
});
