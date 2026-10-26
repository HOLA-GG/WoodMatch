import test, { after, before } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  getDocs,
  limit,
  query,
  serverTimestamp,
  setDoc,
  where,
  updateDoc,
} from "firebase/firestore";
import { Firestore } from "../supabase/functions/woodmatch/firestore.mjs";
import { createDomain } from "../supabase/functions/woodmatch/domain.mjs";
import { createHash } from "node:crypto";
let env;
const project = "demo-woodmatch-pilot";
before(async () => {
  env = await initializeTestEnvironment({
    projectId: project,
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: await readFile(
        new URL("../firestore.rules", import.meta.url),
        "utf8",
      ),
    },
  });
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    for (
      const [uid, role] of [["author", "GENERADOR"], ["receiver", "RECEPTOR"], [
        "stranger",
        "MIXTO",
      ]]
    ) await setDoc(doc(db, "users", uid), { uid, name: uid, role });
    await setDoc(doc(db, "wood_wastes", "active"), {
      schemaVersion: 3,
      status: "DISPONIBLE",
      authorId: "author",
      completedBy: null,
    });
    await setDoc(doc(db, "wood_wastes", "legacy"), {
      schemaVersion: 2,
      status: "DISPONIBLE",
      authorId: "author",
      completedBy: null,
    });
    await setDoc(doc(db, "wood_wastes", "closed"), {
      schemaVersion: 3,
      status: "APROVECHADO",
      authorId: "author",
      completedBy: "receiver",
    });
    await setDoc(doc(db, "withdrawal_requests", "receipt"), {
      authorId: "author",
      requesterId: "receiver",
      status: "COMPLETADA",
    });
    await setDoc(doc(db, "withdrawal_passes", "receipt"), { hash: "private" });
  });
});
after(async () => {
  await env?.cleanup();
});
const user = (uid) =>
  env.authenticatedContext(uid, { firebase: { sign_in_provider: "password" } })
    .firestore();
test("community reads require a real account and support the actual Android query", async () => {
  await assertFails(
    getDoc(
      doc(env.unauthenticatedContext().firestore(), "wood_wastes", "active"),
    ),
  );
  const anon = env.authenticatedContext("anon", {
    firebase: { sign_in_provider: "anonymous" },
  }).firestore();
  await assertFails(getDoc(doc(anon, "wood_wastes", "active")));
  const rows = await assertSucceeds(
    getDocs(
      query(
        collection(user("stranger"), "wood_wastes"),
        where("schemaVersion", "in", [2, 3]),
        where("status", "in", ["HALLAZGO", "DISPONIBLE", "RESERVADO"]),
        limit(100),
      ),
    ),
  );
  assert.equal(rows.size, 2);
});
test("completed items and receipts are private to the two participants", async () => {
  for (const uid of ["author", "receiver"]) {
    await assertSucceeds(getDoc(doc(user(uid), "wood_wastes", "closed")));
    await assertSucceeds(
      getDoc(doc(user(uid), "withdrawal_requests", "receipt")),
    );
  }
  await assertFails(getDoc(doc(user("stranger"), "wood_wastes", "closed")));
  await assertFails(
    getDoc(doc(user("stranger"), "withdrawal_requests", "receipt")),
  );
});
test("all client mutations and reads of pass secrets are denied", async () => {
  await assertFails(
    setDoc(doc(user("author"), "wood_wastes", "direct"), {
      schemaVersion: 3,
      status: "DISPONIBLE",
      authorId: "author",
    }),
  );
  await assertFails(
    setDoc(doc(user("author"), "withdrawal_requests", "receipt"), {
      status: "COMPLETADA",
    }),
  );
  await assertFails(
    getDoc(doc(user("author"), "withdrawal_passes", "receipt")),
  );
  await assertFails(
    getDoc(doc(user("receiver"), "withdrawal_passes", "receipt")),
  );
});
const registration = (uid, role = "MIXTO") => ({
  uid,
  name: "Persona de prueba",
  email: `${uid}@example.com`,
  phone: "+573001234567",
  dob: "",
  city: "Bogotá",
  gender: "",
  role,
  sectorSuba: "Suba Centro",
  privacyVersion: "2026-PILOTO-2",
  privacyAcceptedAt: serverTimestamp(),
});
test("registration allows all three business roles with valid private profile and consent", async () => {
  for (const [index, role] of ["GENERADOR", "RECEPTOR", "MIXTO"].entries()) {
    const uid = `registered_${index}`;
    await assertSucceeds(setDoc(doc(user(uid), "users", uid), registration(uid, role)));
    const snapshot = await assertSucceeds(getDoc(doc(user(uid), "users", uid)));
    assert.equal(snapshot.data().role, role);
  }
});
test("registration denies privileged roles, wrong UID, unknown schema and missing required fields", async () => {
  const invalid = [
    (p) => ({ ...p, role: "ADMIN" }),
    (p) => ({ ...p, role: "admin" }),
    (p) => ({ ...p, role: "user" }),
    (p) => ({ ...p, role: "INVITADO" }),
    (p) => ({ ...p, role: 2 }),
    (p) => ({ ...p, uid: "someone_else" }),
    (p) => { const { uid, ...rest } = p; return rest; },
    (p) => { const { name, ...rest } = p; return rest; },
    (p) => { const { role, ...rest } = p; return rest; },
    (p) => ({ ...p, isAdmin: true }),
    (p) => ({ ...p, arbitrary: "schema pollution" }),
    (p) => ({ ...p, updatedAt: serverTimestamp() }),
    (p) => ({ ...p, deleting: false }),
    (p) => ({ ...p, privacyVersion: "old" }),
    (p) => ({ ...p, privacyAcceptedAt: new Date(0) }),
    (p) => { const { privacyAcceptedAt, ...rest } = p; return rest; },
  ];
  for (const [index, makeInvalid] of invalid.entries()) {
    const uid = `bad_schema_${index}`;
    await assertFails(setDoc(doc(user(uid), "users", uid), makeInvalid(registration(uid))));
  }
});
test("profile validators reject malformed and oversized fields on create", async () => {
  const invalidFields = [
    { name: "" }, { name: "x".repeat(121) }, { name: 1 },
    { phone: "" }, { phone: 573001234567 }, { phone: "+57300123456" },
    { phone: "+583001234567" }, { phone: "+5730012345678" },
    { email: "" }, { email: "not-an-email" }, { email: 42 },
    { email: "x".repeat(244) + "@example.com" },
    { dob: "x".repeat(33) }, { dob: [] },
    { city: "x".repeat(121) }, { city: 5 },
    { gender: "x".repeat(41) }, { gender: false },
    { sectorSuba: "x".repeat(121) }, { sectorSuba: {} },
    { privacyVersion: "x".repeat(41) },
  ];
  for (const [index, fields] of invalidFields.entries()) {
    const uid = `bad_fields_${index}`;
    await assertFails(setDoc(doc(user(uid), "users", uid), { ...registration(uid), ...fields }));
  }
});
test("legacy profiles can update name and phone but no other registered data or role", async () => {
  const ref = doc(user("author"), "users", "author");
  // Legacy fixtures intentionally contain only uid/name/role.
  await assertSucceeds(updateDoc(ref, {
    name: "Nombre actualizado",
    phone: "+573101234567",
    updatedAt: serverTimestamp(),
  }));
  await assertSucceeds(updateDoc(ref, { name: "Solo nombre", updatedAt: serverTimestamp() }));
  for (const role of ["RECEPTOR", "MIXTO", "ADMIN", "admin"]) {
    await assertFails(updateDoc(ref, { role, updatedAt: serverTimestamp() }));
  }
  const forbidden = [
    { uid: "receiver" }, { email: "changed@example.com" }, { city: "Otra ciudad" },
    { sectorSuba: "Otro sector" }, { privacyVersion: "new" },
    { privacyAcceptedAt: serverTimestamp() }, { deleting: true }, { arbitrary: "extra" },
  ];
  for (const fields of forbidden) {
    await assertFails(updateDoc(ref, { ...fields, updatedAt: serverTimestamp() }));
  }
  await assertFails(updateDoc(ref, { name: "Sin timestamp" }));
  await assertFails(updateDoc(ref, { name: "Timestamp falso", updatedAt: new Date(0) }));
  await assertFails(deleteDoc(ref));
  const snapshot = await getDoc(ref);
  assert.equal(snapshot.data().role, "GENERADOR");
  assert.equal(snapshot.data().uid, "author");
});
test("resulting profile schema is validated on updates and required fields cannot be removed", async () => {
  const ref = doc(user("registered_2"), "users", "registered_2");
  for (const fields of [
    { name: "" }, { name: "x".repeat(121) }, { name: 17 },
    { phone: "" }, { phone: "not-a-phone" }, { phone: 573001234567 },
    { phone: "x".repeat(10000) },
    { name: deleteField() }, { uid: deleteField() }, { role: deleteField() },
    { phone: deleteField() },
  ]) {
    await assertFails(updateDoc(ref, { ...fields, updatedAt: serverTimestamp() }));
  }
  // A server-seeded profile with malformed optional data cannot bypass validation on update.
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users", "malformed_legacy"), {
      uid: "malformed_legacy", name: "Legacy", role: "RECEPTOR", email: 42,
    });
  });
  await assertFails(updateDoc(doc(user("malformed_legacy"), "users", "malformed_legacy"), {
    name: "Changed", updatedAt: serverTimestamp(),
  }));
});
test("private profiles reject other users, anonymous access and broad queries", async () => {
  await assertFails(getDoc(doc(user("stranger"), "users", "author")));
  await assertFails(updateDoc(doc(user("stranger"), "users", "author"), {
    name: "Hijacked", updatedAt: serverTimestamp(),
  }));
  await assertFails(setDoc(doc(user("stranger"), "users", "not_the_stranger"), registration("not_the_stranger")));
  await assertFails(getDocs(collection(user("stranger"), "users")));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), "users", "author")));
  const anon = env.authenticatedContext("author", { firebase: { sign_in_provider: "anonymous" } }).firestore();
  await assertFails(getDoc(doc(anon, "users", "author")));
  await assertFails(updateDoc(doc(anon, "users", "author"), { name: "Anon", updatedAt: serverTimestamp() }));
  // Preserve the pre-existing explicit admin custom-claim read exception; never document role.
  const adminDb = env.authenticatedContext("trusted_admin", {
    admin: true, firebase: { sign_in_provider: "password" },
  }).firestore();
  await assertSucceeds(getDoc(doc(adminDb, "users", "author")));
});
test("tombstoned accounts cannot edit their private profile", async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users", "erasing_user"), {
      uid: "erasing_user", name: "Pending deletion", role: "RECEPTOR",
    });
    await setDoc(doc(context.firestore(), "account_tombstones", "erasing_user"), {
      createdAt: Date.now(),
    });
  });
  await assertFails(updateDoc(doc(user("erasing_user"), "users", "erasing_user"), {
    name: "Reopened", updatedAt: serverTimestamp(),
  }));
});
test("REST adapter and domain retry concurrent transactions against Firestore", async () => {
  const db = new Firestore(
    project,
    async () => "owner",
    "http://127.0.0.1:8085/v1",
  );
  const hash = async (s) => createHash("sha256").update(s).digest("hex");
  const domain = createDomain({ db, hash, passCode: async () => "12345678" });
  await db.set("wood_wastes/concurrent", {
    id: "concurrent",
    schemaVersion: 3,
    entryType: "OFERTA",
    title: "Tablas",
    authorId: "author",
    status: "DISPONIBLE",
    revision: 1,
  });
  for (const id of ["r1", "r2"]) {
    await db.set(`withdrawal_requests/${id}`, {
      id,
      wasteId: "concurrent",
      authorId: "author",
      requesterId: id === "r1" ? "receiver" : "stranger",
      status: "PENDIENTE",
    });
  }
  const results = await Promise.allSettled(
    ["r1", "r2"].map((id) =>
      domain.call({ uid: "author" }, "transitionWithdrawal", {
        operationId: `accept_${id}`,
        requestId: id,
        action: "accept",
      })
    ),
  );
  assert.equal(
    results.filter((r) => r.status === "fulfilled").length,
    1,
    JSON.stringify(
      results.map((r) =>
        r.status === "fulfilled"
          ? "ok"
          : { status: r.reason.status, message: r.reason.message }
      ),
    ),
  );
  assert.equal((await db.get("wood_wastes/concurrent")).status, "RESERVADO");
});

