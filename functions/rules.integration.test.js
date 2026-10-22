const {test} = require('node:test');
const fs = require('node:fs');
const {initializeTestEnvironment, assertFails, assertSucceeds} = require('@firebase/rules-unit-testing');
const {doc, setDoc, updateDoc, getDoc} = require('firebase/firestore');
test('Firestore rejects unauthorized lifecycle changes and private profile reads', {skip: !process.env.FIRESTORE_EMULATOR_HOST}, async () => {
 const env=await initializeTestEnvironment({projectId:'demo-woodmatch',firestore:{rules:fs.readFileSync('../firestore.rules','utf8')}});
 try {
  await env.withSecurityRulesDisabled(async ctx => {
   const db=ctx.firestore();
   await setDoc(doc(db,'users','owner'),{uid:'owner',role:'GENERADOR'});
   await setDoc(doc(db,'users','other'),{uid:'other',role:'RECEPTOR'});
   await setDoc(doc(db,'wood_wastes','w1'),{id:'w1',schemaVersion:2,authorId:'owner',status:'DISPONIBLE'});
   await setDoc(doc(db,'withdrawal_requests','r1'),{authorId:'owner',requesterId:'receiver',status:'PENDIENTE'});
  });
  const owner=env.authenticatedContext('owner',{firebase:{sign_in_provider:'password'}}).firestore();
  const stranger=env.authenticatedContext('other',{firebase:{sign_in_provider:'password'}}).firestore();
  const guest=env.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(owner,'wood_wastes','w1')));
  await assertFails(getDoc(doc(guest,'wood_wastes','w1')));
  await assertFails(getDoc(doc(stranger,'users','owner')));
  await assertFails(getDoc(doc(stranger,'withdrawal_requests','r1')));
  await assertSucceeds(getDoc(doc(owner,'users','owner')));
  await assertFails(updateDoc(doc(stranger,'wood_wastes','w1'),{status:'RESERVADO',reservedBy:'other'}));
  await assertFails(updateDoc(doc(owner,'users','owner'),{role:'ADMIN'}));
  await assertFails(setDoc(doc(stranger,'withdrawal_passes','forged'),{authorId:'other'}));
  await assertFails(updateDoc(doc(owner,'withdrawal_requests','r1'),{status:'ACEPTADA'}));
  await assertSucceeds(updateDoc(doc(owner,'wood_wastes','w1'),{status:'INACTIVO',updatedAt:1}));
 } finally { await env.cleanup(); }
});
