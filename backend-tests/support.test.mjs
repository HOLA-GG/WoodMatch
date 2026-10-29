import test from 'node:test';
import assert from 'node:assert/strict';
import { supportCall, supportOperator } from '../supabase/functions/woodmatch/support.mjs';
import { registerDevice, notifyMaterial } from '../supabase/functions/woodmatch/notifications.mjs';

const admin={uid:'yDGu1WUesuc01uUbUzc3By4qyL02',email:'cookiestech@woodmatch.org'};
const user={uid:'receiver',email:'receiver@example.org'};
function fixture() {
  const rows=new Map([['wood_wastes/entry',{id:'entry',title:'Mesa',status:'DISPONIBLE',revision:1}],['users/receiver',{role:'RECEPTOR'}]]);
  const db={get:async p=>rows.get(p),set:async(p,v)=>rows.set(p,v),list:async(c,f,v)=>[...rows].filter(([p,row])=>p.startsWith(c+'/')&&(!f||row[f]===v)).map(([,row])=>row),atomic:async work=>{
    const writes=[]; const result=await work({get:async p=>rows.get(p),set:(p,v)=>writes.push([p,v])}); writes.forEach(([p,v])=>rows.set(p,v)); return result;
  }};
  return {db,rows};
}
test('support operator requires exact verified identity; admin flag and matching email alone do not grant access',async()=>{
  assert.equal(supportOperator({...user,admin:true}),false);
  assert.equal(supportOperator({...user,email:admin.email}),false);
  const {db}=fixture();
  await assert.rejects(()=>supportCall(db,user,'adminSupportCases',{}));
  assert.equal((await supportCall(db,admin,'supportAccess',{})).operator,true);
});
test('reports are idempotent, private and tracked through resolution',async()=>{
  const {db,rows}=fixture();
  const data={id:'report',type:'MODERACION',subject:'Foto incorrecta',detail:'La foto no corresponde',publicationId:'entry'};
  await supportCall(db,user,'createSupportCase',data);
  await supportCall(db,user,'createSupportCase',data);
  assert.equal(rows.get('support_cases/report').history.length,1);
  assert.equal((await supportCall(db,{uid:'other'},'mySupportCases',{})).cases.length,0);
  await assert.rejects(()=>supportCall(db,admin,'updateSupportCase',{id:'report',status:'CERRADO',priority:'MEDIA',response:''}));
  const row=rows.get('support_cases/report');
  await supportCall(db,admin,'updateSupportCase',{id:'report',status:'RESUELTO',priority:'ALTA',response:'Revisamos el contenido',expectedUpdatedAt:row.updatedAt});
  assert.equal(rows.get('support_cases/report').status,'RESUELTO');
});
test('moderation requires linked report; blocks reserved entries; restores original state with audit',async()=>{
  const {db,rows}=fixture();
  rows.set('support_cases/report',{publicationId:'entry',history:[]});
  const data={id:'entry',caseId:'report',reason:'Imagen irrelevante',action:'OCULTAR'};
  await assert.rejects(()=>supportCall(db,user,'moderateEntry',data));
  await supportCall(db,admin,'moderateEntry',data);
  assert.equal(rows.get('wood_wastes/entry').status,'MODERADO');
  await supportCall(db,admin,'moderateEntry',{...data,action:'RESTAURAR'});
  assert.equal(rows.get('wood_wastes/entry').status,'DISPONIBLE');
  rows.get('wood_wastes/entry').status='RESERVADO';
  await assert.rejects(()=>supportCall(db,admin,'moderateEntry',data));
});
test('notification delivery checks recipient role and deduplicates publication/device',async()=>{
  const {db,rows}=fixture(); const hash=async v=>v.replaceAll(':','_');
  await registerDevice(db,hash,user,{role:'RECEPTOR'},{token:'fixture-token-long-enough',enabled:true});
  const material={id:'entry',title:'Mesa',status:'DISPONIBLE',authorId:'author'}; let sent=0;
  await notifyMaterial(db,hash,async()=>{sent++},material);
  await notifyMaterial(db,hash,async()=>{sent++},material);
  assert.equal(sent,1);
  rows.get('users/receiver').role='GENERADOR';
  await notifyMaterial(db,hash,async()=>{sent++},{...material,id:'another'});
  assert.equal(sent,1);
});
