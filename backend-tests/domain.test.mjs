import test from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import {
  canReadItem,
  createDomain,
  PRIVACY_VERSION,
  validateEntry,
} from "../supabase/functions/woodmatch/domain.mjs";
import { decode, encode } from "../supabase/functions/woodmatch/firestore.mjs";

const hash = async (value) => createHash("sha256").update(value).digest("hex");
class MemoryDB {
  docs = new Map();
  queue = Promise.resolve();
  async atomic(work) {
    let release;
    const before = this.queue;
    this.queue = new Promise((r) => release = r);
    await before;
    const copy = structuredClone(this.docs);
    try {
      const result = await work({
        get: async (path) => structuredClone(copy.get(path) || null),
        set: (path, data) => copy.set(path, structuredClone(data)),
      });
      this.docs = copy;
      return result;
    } finally {
      release();
    }
  }
}
async function fixture() {
  let clock = 1800000000000;
  let sequence = 0;
  const db = new MemoryDB();
  const actors = {
    owner: { uid: "owner" },
    receiver: { uid: "receiver" },
    other: { uid: "other" },
  };
  db.docs.set("users/owner", { name: "Oferente", role: "GENERADOR" });
  db.docs.set("users/receiver", { name: "Receptor", role: "RECEPTOR" });
  db.docs.set("users/other", { name: "Otra persona", role: "RECEPTOR" });
  const domain = createDomain({
    db,
    hash,
    passCode: async (input) =>
      (parseInt((await hash(input)).slice(0, 8), 16) % 100000000).toString()
        .padStart(8, "0"),
    now: () => clock,
  });
  const entry = {
    id: "wood",
    entryType: "OFERTA",
    title: "Estibas recuperables",
    description: "",
    wasteType: "Estibas",
    category: "ESTANDAR",
    materialState: "Limpio",
    quantityValue: 2,
    quantityUnit: "unidades",
    subaSector: "Suba Centro",
    exactLatitude: 4.745,
    exactLongitude: -74.085,
    locationConfirmed: true,
    exactLocationConsent: true,
    authorizedToOffer: true,
    imagePath: "owner/wood/photo.jpg",
  };
  db.docs.set(`photo_uploads/${await hash(entry.imagePath)}`, {
    uid: "owner",
    entryId: "wood",
    state: "ready",
  });
  const call = (who, op, data = {}) =>
    domain.call(actors[who], op, { operationId: `op_${++sequence}`, ...data });
  const publish = () => call("owner", "publishEntry", entry);
  const request = (who = "receiver", id = "request") =>
    call(who, "createWithdrawal", {
      id,
      wasteId: "wood",
      intendedUse: "Construir un banco",
      proposedDate: "2026-11-10",
      requestedQuantity: "2 unidades",
      message: "",
    });
  const accept = () =>
    call("owner", "transitionWithdrawal", {
      requestId: "request",
      action: "accept",
    });
  const pass = () => call("receiver", "issuePass", { requestId: "request" });
  return {
    db,
    entry,
    call,
    publish,
    request,
    accept,
    pass,
    tick: (ms) => clock += ms,
  };
}

test("serializer preserves numbers, nested receipts, arrays and nulls", () => {
  const value = {
    version: 3,
    quantity: 1.5,
    receipt: { completedAt: 1800000000000 },
    flags: [true, null, "a"],
  };
  assert.deepEqual(decode(encode(value)), value);
});
test("condition does not infer availability; explicit hallazgo is not an offer", async () => {
  const f = await fixture();
  await f.call("receiver", "publishEntry", {
    ...f.entry,
    id: "found",
    imagePath: "receiver/found/photo.jpg",
    entryType: "HALLAZGO",
    materialState: "Dañado",
  }).then(() => assert.fail(), (e) => assert.equal(e.code, "photo"));
  assert.equal(
    validateEntry({
      ...f.entry,
      entryType: "HALLAZGO",
      materialState: "Dañado",
    }, { role: "RECEPTOR" }).quantityValue,
    0,
  );
  await f.publish();
  assert.equal(f.db.docs.get("wood_wastes/wood").status, "DISPONIBLE");
});
test("offer requires server role, real selected point and exact-location consent", async () => {
  const f = await fixture();
  assert.throws(() => validateEntry(f.entry, { role: "RECEPTOR" }), {
    code: "role",
  });
  for (
    const change of [
      { locationConfirmed: false },
      { exactLocationConsent: false },
      { exactLatitude: 0 },
      { quantityValue: NaN },
      { authorizedToOffer: false },
    ]
  ) {
    assert.throws(() =>
      validateEntry({ ...f.entry, ...change }, { role: "GENERADOR" })
    );
  }
});
test("publishing and editing are idempotent; changed payload with same op is rejected", async () => {
  const f = await fixture();
  const payload = { ...f.entry, operationId: "fixed" };
  assert.deepEqual(
    await f.call("owner", "publishEntry", payload),
    await f.call("owner", "publishEntry", payload),
  );
  assert.equal(f.db.docs.get("wood_wastes/wood").revision, 1);
  await assert.rejects(
    f.call("owner", "publishEntry", { ...payload, title: "Otra publicación" }),
    { code: "operation" },
  );
  await f.call("owner", "editEntry", {
    ...f.entry,
    expectedRevision: 1,
    title: "Estibas actualizadas",
  });
  await assert.rejects(
    f.call("owner", "editEntry", { ...f.entry, expectedRevision: 1 }),
    { code: "revision" },
  );
});
test("only author edits or converts; duplicate active requests are rejected", async () => {
  const f = await fixture();
  await f.publish();
  await assert.rejects(
    f.call("receiver", "editEntry", { ...f.entry, expectedRevision: 1 }),
    { code: "owner" },
  );
  await f.request();
  await assert.rejects(f.request("receiver", "duplicate"), {
    code: "duplicate",
  });
  await assert.rejects(
    f.call("owner", "createWithdrawal", { id: "own", wasteId: "wood" }),
  );
});
test("two concurrent acceptances produce exactly one reservation", async () => {
  const f = await fixture();
  await f.publish();
  await f.request();
  await f.request("other", "request2");
  const results = await Promise.allSettled([
    f.accept(),
    f.call("owner", "transitionWithdrawal", {
      requestId: "request2",
      action: "accept",
    }),
  ]);
  assert.equal(results.filter((x) => x.status === "fulfilled").length, 1);
  assert.equal(f.db.docs.get("wood_wastes/wood").status, "RESERVADO");
  await assert.rejects(
    f.call("owner", "editEntry", { ...f.entry, expectedRevision: 2 }),
    { code: "reserved" },
  );
});
test("hallazgo converts in-place only after role and authorization checks", async () => {
  const f = await fixture();
  await f.call("owner", "publishEntry", { ...f.entry, entryType: "HALLAZGO" });
  await assert.rejects(f.request(), { code: "unavailable" });
  await f.call("owner", "editEntry", { ...f.entry, expectedRevision: 1 });
  assert.equal(f.db.docs.get("wood_wastes/wood").entryType, "OFERTA");
  assert.equal(f.db.docs.get("wood_wastes/wood").revision, 2);
});
test("QR and code validate the same request; both participants must confirm", async () => {
  const f = await fixture();
  await f.publish();
  await f.request();
  await f.accept();
  const pass = await f.pass();
  assert.equal(pass.token, `request:${pass.code}`);
  assert.equal(pass.code.length, 8);
  await assert.rejects(
    f.call("receiver", "redeemPass", { requestId: "request", code: pass.code }),
    { code: "pass" },
  );
  await assert.rejects(
    f.call("owner", "transitionWithdrawal", {
      requestId: "request",
      action: "confirm",
    }),
    { code: "pass" },
  );
  await f.call("owner", "redeemPass", {
    requestId: "request",
    code: pass.code,
  });
  await assert.rejects(
    f.call("owner", "redeemPass", { requestId: "request", code: pass.code }),
    { code: "pass" },
  );
  await f.call("owner", "transitionWithdrawal", {
    requestId: "request",
    action: "confirm",
  });
  assert.equal(f.db.docs.get("wood_wastes/wood").status, "RESERVADO");
  await assert.rejects(
    f.call("other", "transitionWithdrawal", {
      requestId: "request",
      action: "confirm",
    }),
    { code: "participant" },
  );
  const data = {
    requestId: "request",
    action: "confirm",
    operationId: "final",
  };
  await f.call("receiver", "transitionWithdrawal", data);
  await f.call("receiver", "transitionWithdrawal", data);
  const item = f.db.docs.get("wood_wastes/wood"),
    request = f.db.docs.get("withdrawal_requests/request");
  assert.equal(item.status, "APROVECHADO");
  assert.equal(request.status, "COMPLETADA");
  assert.equal(request.receipt.label, "Comprobante de entrega");
  assert.equal(canReadItem(item, "other"), false);
  assert.equal(canReadItem(item, "owner"), true);
  assert.equal(canReadItem(item, "receiver"), true);
});
test("new pass revokes the old one and expired passes fail", async () => {
  const f = await fixture();
  await f.publish();
  await f.request();
  await f.accept();
  const one = await f.pass();
  f.tick(10001);
  const two = await f.pass();
  await assert.rejects(
    f.call("owner", "redeemPass", { requestId: "request", code: one.code }),
    { code: "pass" },
  );
  f.tick(600001);
  await assert.rejects(
    f.call("owner", "redeemPass", { requestId: "request", code: two.code }),
    { code: "pass" },
  );
});
test("five bad attempts are committed and lock validation", async () => {
  const f = await fixture();
  await f.publish();
  await f.request();
  await f.accept();
  const pass = await f.pass();
  const bad = pass.code === "00000000" ? "00000001" : "00000000";
  for (let i = 0; i < 5; i++) {
    await assert.rejects(
      f.call("owner", "redeemPass", { requestId: "request", code: bad }),
      { code: "pass" },
    );
  }
  assert.equal(f.db.docs.get("withdrawal_passes/request").attempts, 5);
  await assert.rejects(
    f.call("owner", "redeemPass", { requestId: "request", code: pass.code }),
    { status: 429 },
  );
});
test("cancellation releases the reservation and invalidates the pass", async () => {
  const f = await fixture();
  await f.publish();
  await f.request();
  await f.accept();
  const pass = await f.pass();
  await f.call("receiver", "transitionWithdrawal", {
    requestId: "request",
    action: "cancel",
  });
  assert.equal(f.db.docs.get("wood_wastes/wood").status, "DISPONIBLE");
  await assert.rejects(
    f.call("owner", "redeemPass", { requestId: "request", code: pass.code }),
    { code: "pass" },
  );
  await f.request("receiver", "new_request");
});
test("account tombstones prevent all later mutations", async () => {
  const f = await fixture();
  f.db.docs.set("account_tombstones/owner", { createdAt: 1 });
  await assert.rejects(f.publish(), { code: "account" });
});
test("registered consent version matches the deployed rule and Android constant", async () => {
  const fs = await import("node:fs/promises");
  const rules = await fs.readFile(
    new URL("../firestore.rules", import.meta.url),
    "utf8",
  );
  const android = await fs.readFile(
    new URL(
      "../app/src/main/kotlin/com/example/kotlinapp/ui/components/PrivacyNotice.kt",
      import.meta.url,
    ),
    "utf8",
  );
  assert.ok(rules.includes(PRIVACY_VERSION));
  assert.ok(android.includes(PRIVACY_VERSION));
});

test("upload reservation and publication may share a client operation ID", async () => {
  const f = await fixture();
  const op = "same_client_op";
  const reservation = await f.call("owner", "reservePhoto", {
    id: "wood",
    operationId: op,
    bytes: 500,
    contentHash: "abc",
  });
  assert.deepEqual(
    reservation,
    await f.call("owner", "reservePhoto", {
      id: "wood",
      operationId: op,
      bytes: 500,
      contentHash: "abc",
    }),
  );
  assert.equal(f.db.docs.get("photo_budget/global").bytes, 500);
  const path = reservation.path, photoKey = `photo_uploads/${await hash(path)}`;
  await assert.rejects(
    f.call("owner", "publishEntry", {
      ...f.entry,
      imagePath: path,
      operationId: op,
    }),
    { code: "photo" },
  );
  f.db.docs.set(photoKey, { ...f.db.docs.get(photoKey), state: "ready" });
  await f.call("owner", "publishEntry", {
    ...f.entry,
    imagePath: path,
    operationId: op,
  });
  assert.equal(f.db.docs.get("wood_wastes/wood").revision, 1);
});
test("global storage budget prevents concurrent over-allocation", async () => {
  const f = await fixture();
  f.db.docs.set("photo_budget/global", { bytes: 700 * 1024 * 1024 - 500 });
  const attempts = await Promise.allSettled(
    [1, 2].map((n) =>
      f.call("owner", "reservePhoto", {
        id: `photo_${n}`,
        bytes: 400,
        contentHash: "test",
      })
    ),
  );
  assert.equal(attempts.filter((r) => r.status === "fulfilled").length, 1);
});

test("erasure is resumable, anonymizes receipts and removes nested events and photos", async () => {
  const { eraseAccount } = await import(
    "../supabase/functions/woodmatch/cleanup.mjs"
  );
  const docs = new Map([
    ["users/removed", { name: "Persona", role: "MIXTO" }],
    ["wood_wastes/own", { id: "own", authorId: "removed" }],
    ["wood_wastes/own/events/e", { actorId: "removed" }],
    ["wood_wastes/other", {
      id: "other",
      authorId: "other",
      status: "RESERVADO",
      reservedBy: "removed",
      revision: 1,
    }],
    ["withdrawal_requests/r", {
      id: "r",
      wasteId: "other",
      authorId: "other",
      requesterId: "removed",
      status: "COMPLETADA",
      receipt: { authorName: "Otra", requesterName: "Persona" },
    }],
    ["withdrawal_requests/r/events/e", {
      actorId: "removed",
      action: "confirm",
    }],
    ["photo_uploads/p", {
      uid: "removed",
      path: "private/photo.jpg",
      bytes: 50,
    }],
    ["photo_budget/global", { bytes: 100 }],
    ["photo_budget/removed", { bytes: 50 }],
    ["operations/op", { uid: "removed" }],
  ]);
  const db = {
    get: async (p) => docs.get(p) || null,
    set: async (p, v) => docs.set(p, v),
    remove: async (p) => docs.delete(p),
    list: async (c, field, value) =>
      [...docs].filter(([p, v]) =>
        p.startsWith(c + "/") &&
        p.split("/").length === c.split("/").length + 1 &&
        (!field || v[field] === value)
      ).map(([p, v]) => ({ ...v, _path: p })),
    atomic: async (fn) =>
      fn({
        get: async (p) => docs.get(p) || null,
        set: (p, v) => docs.set(p, v),
      }),
  };
  let failed = true;
  const args = {
    uid: "removed",
    db,
    identity: async () => ({}),
    deletePhotos: async () => {
      if (failed) throw new Error("network");
    },
  };
  await assert.rejects(eraseAccount(args));
  assert.ok(docs.has("account_tombstones/removed"));
  failed = false;
  await eraseAccount(args);
  await eraseAccount(args);
  assert.equal(docs.has("users/removed"), false);
  assert.equal(docs.has("wood_wastes/own/events/e"), false);
  assert.equal(docs.get("withdrawal_requests/r").requesterId, "deleted");
  assert.equal(docs.get("withdrawal_requests/r").receipt.requesterName, "");
  assert.equal(docs.get("withdrawal_requests/r/events/e").actorId, "deleted");
  assert.equal(docs.has("operations/op"), false);
  assert.equal(docs.get("photo_budget/global").bytes, 50);
});
