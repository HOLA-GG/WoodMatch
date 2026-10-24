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
  doc,
  getDoc,
  getDocs,
  limit,
  query,
  serverTimestamp,
  setDoc,
  where,
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
test("registration consent and role changes cannot grant admin", async () => {
  const db = user("new_user");
  const payload = {
    uid: "new_user",
    name: "Persona",
    role: "MIXTO",
    privacyVersion: "2026-PILOTO-2",
    privacyAcceptedAt: serverTimestamp(),
  };
  await assertSucceeds(setDoc(doc(db, "users", "new_user"), payload));
  await assertFails(
    setDoc(doc(db, "users", "new_user"), {
      ...payload,
      role: "ADMIN",
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(getDoc(doc(user("stranger"), "users", "author")));
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
  assert.equal(results.filter((r) => r.status === "fulfilled").length, 1);
  assert.equal((await db.get("wood_wastes/concurrent")).status, "RESERVADO");
});
