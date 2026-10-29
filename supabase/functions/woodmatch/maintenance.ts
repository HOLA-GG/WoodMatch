import { importPKCS8, SignJWT } from "npm:jose@5.9.6";
import { Firestore } from "./firestore.mjs";
import { runMaintenance } from "./maintenance.mjs";
const env=(name:string)=>Deno.env.get(name)||"";
const project=env("FIREBASE_PROJECT_ID");
let cachedToken: {value:string;until:number}|null=null;
async function accessToken() {
  if(cachedToken && cachedToken.until>Date.now()) return cachedToken.value;
  const key=JSON.parse(env("FIREBASE_SERVICE_ACCOUNT"));
  if(key.project_id!==project) throw new Error("Project mismatch");
  const assertion=await new SignJWT({scope:"https://www.googleapis.com/auth/cloud-platform"})
    .setProtectedHeader({alg:"RS256"}).setIssuer(key.client_email).setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt().setExpirationTime("1h").sign(await importPKCS8(key.private_key,"RS256"));
  const response=await fetch("https://oauth2.googleapis.com/token",{method:"POST",body:new URLSearchParams({grant_type:"urn:ietf:params:oauth:grant-type:jwt-bearer",assertion})});
  if(!response.ok) throw new Error("Credentials unavailable");
  const value=(await response.json()).access_token;
  cachedToken={value,until:Date.now()+3300000};
  return value;
}
const db=new Firestore(project,accessToken);
const hash=async(value:string)=>Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256",new TextEncoder().encode(value)))).map(x=>x.toString(16).padStart(2,"0")).join("");
// Gateway JWT verification stays enabled. This fixed maintenance entrypoint accepts
// no user-selected operations, document IDs or times and returns no private records.
Deno.serve(async request=>{
  if(request.method!=="POST") return new Response("",{status:405});
  try {
    const result=await runMaintenance(db,hash,async(token:string,note:any)=>{
      const response=await fetch(`https://fcm.googleapis.com/v1/projects/${project}/messages:send`,{method:"POST",headers:{Authorization:`Bearer ${await accessToken()}`,"Content-Type":"application/json"},
        body:JSON.stringify({message:{token,data:{title:note.title,body:note.body,requestId:note.requestId||"",caseId:note.caseId||"",notificationId:note.id,recipientId:note.uid},android:{priority:"high"}}})});
      if(!response.ok) throw new Error("Delivery pending");
    });
    return Response.json(result);
  } catch {return Response.json({ok:false},{status:503});}
});
