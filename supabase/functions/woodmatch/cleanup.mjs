// Resumable erasure used by the API and the operator's recovery command.
export async function eraseAccount({ uid, db, identity, deletePhotos }) {
  await db.set(`account_tombstones/${uid}`, { createdAt: Date.now() });
  await identity("update", { localId: uid, disableUser: true });
  const profile = await db.get(`users/${uid}`);
  if (profile) await db.set(`users/${uid}`, { ...profile, deleting: true });
  async function drain(collection, field, handle) {
    while (true) {
      const rows = await db.list(collection, field, uid);
      if (!rows.length) break;
      for (const row of rows) await handle(row);
    }
  }
  for (const field of ["authorId", "requesterId"]) {
    await drain("withdrawal_requests", field, async (row) => {
      await db.atomic(async (tx) => {
        const request = await tx.get(row._path);
        const item = await tx.get(`wood_wastes/${request.wasteId}`);
        if (item?.reservedBy === uid && item.status === "RESERVADO") {
          tx.set(`wood_wastes/${item.id}`, {
            ...item,
            status: "DISPONIBLE",
            reservedBy: null,
            revision: (item.revision || 0) + 1,
          });
        }
        tx.set(row._path, {
          ...request,
          authorId: request.authorId === uid ? "deleted" : request.authorId,
          requesterId: request.requesterId === uid
            ? "deleted"
            : request.requesterId,
          requesterName: "",
          message: "",
          intendedUse: "",
          status: request.status === "COMPLETADA" ? "COMPLETADA" : "CANCELADA",
          receipt: request.receipt
            ? { ...request.receipt, authorName: "", requesterName: "" }
            : null,
        });
      });
      for (const event of await db.list(`${row._path}/events`)) {
        if (event.actorId === uid) {
          const { _path, ...fields } = event;
          await db.set(_path, { ...fields, actorId: "deleted" });
        }
      }
      await db.remove(`withdrawal_passes/${row.id}`);
    });
  }
  await drain("wood_wastes", "authorId", async (row) => {
    for (const event of await db.list(`${row._path}/events`)) {
      await db.remove(event._path);
    }
    await db.remove(row._path);
  });
  await drain("photo_uploads", "uid", async (row) => {
    await deletePhotos([row.path]);
    await db.atomic(async (tx) => {
      const global = await tx.get("photo_budget/global");
      const existing = await tx.get(row._path);
      if (existing) {
        tx.set("photo_budget/global", {
          bytes: Math.max(0, (global?.bytes || 0) - (existing.bytes || 0)),
        });
        tx.set(row._path, { ...existing, bytes: 0 });
      }
    });
    await db.remove(row._path);
  });
  await db.remove(`photo_budget/${uid}`);
  for (const collection of ["ai_quotas", "operations"]) {
    await drain(collection, "uid", (row) => db.remove(row._path));
  }
  await db.remove(`users/${uid}`);
  await identity("delete", { localId: uid });
  // Keep the tombstone so older client tokens cannot reopen access before expiring.
}
