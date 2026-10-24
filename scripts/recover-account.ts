// Operator recovery only. Defaults to a read-only preview; never clears a tombstone.
import { importPKCS8, SignJWT } from "npm:jose@5.9.6";
import { Firestore } from "../supabase/functions/woodmatch/firestore.mjs";
import { eraseAccount } from "../supabase/functions/woodmatch/cleanup.mjs";
import { safeId } from "../supabase/functions/woodmatch/domain.mjs";
const uid = safeId(Deno.args[0]);
const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT") || "{}");
const project = Deno.env.get("FIREBASE_PROJECT_ID");
if (!project || account.project_id !== project) {
  throw new Error("Configura el proyecto y la cuenta de servicio del piloto.");
}
let token = "";
async function access() {
  if (token) return token;
  const assertion = await new SignJWT({
    scope: "https://www.googleapis.com/auth/cloud-platform",
  }).setProtectedHeader({ alg: "RS256" }).setIssuer(account.client_email)
    .setAudience("https://oauth2.googleapis.com/token").setIssuedAt()
    .setExpirationTime("1h").sign(
      await importPKCS8(account.private_key, "RS256"),
    );
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!res.ok) throw new Error("No se pudo autorizar el mantenimiento.");
  token = (await res.json()).access_token;
  return token;
}
const db = new Firestore(project, access);
if (!await db.get(`account_tombstones/${uid}`)) {
  throw new Error(
    "La cuenta no tiene una eliminación pendiente. No se modificó.",
  );
}
const publications = await db.list("wood_wastes", "authorId", uid),
  photos = await db.list("photo_uploads", "uid", uid);
console.log(
  JSON.stringify({
    pendingPublications: publications.length,
    pendingPhotos: photos.length,
    apply: Deno.args.includes("--apply"),
  }),
);
if (!Deno.args.includes("--apply")) Deno.exit(0);
await eraseAccount({
  uid,
  db,
  identity: async (action: string, body: object) => {
    const res = await fetch(
      `https://identitytoolkit.googleapis.com/v1/projects/${project}/accounts:${action}`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${await access()}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      },
    );
    const result = await res.json();
    if (!res.ok && result.error?.message !== "USER_NOT_FOUND") {
      throw new Error("No se completó la eliminación de identidad.");
    }
    return result;
  },
  deletePhotos: async (paths: string[]) => {
    const res = await fetch(
      `${Deno.env.get("SUPABASE_URL")}/storage/v1/object/woodmatch-photos`,
      {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ prefixes: paths }),
      },
    );
    if (!res.ok) {
      throw new Error("No se completó la eliminación de fotografías.");
    }
  },
});
console.log(
  "Eliminación completada. El bloqueo de tokens antiguos se conserva.",
);
