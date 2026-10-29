import { requireThat, participant, safeId } from './domain.mjs';
export const RESPONSE_WINDOW = 72 * 60 * 60 * 1000;
export const pickupDeadline = date => Date.parse(date + 'T23:59:59-05:00') + 24 * 60 * 60 * 1000;
export function validFutureDate(date, now) {
  requireThat(typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date)
    && Number.isFinite(Date.parse(date)) && new Date(date).toISOString().slice(0,10) === date
    && Date.parse(date + 'T23:59:59-05:00') >= now,400,'date','Elige una fecha de recolección vigente.');
  return date;
}
export const movement = (row, action, reason, actorId, at) => [...(row.timeline || []).slice(-99),{action,reason,actorId,at}];
export async function enqueueNotice(tx, hash, uid, key, title, body, fields={}, at=Date.now()) {
  const id = await hash(uid + ':' + key);
  tx.set('notifications/' + id,{id,uid,title,body,...fields,createdAt:at,read:false,deliveryState:'PENDING'});
}
export async function notifyParticipants(tx, hash, row, key, actorId, title, body, at) {
  for (const uid of [row.authorId,row.requesterId]) if (uid !== actorId)
    await enqueueNotice(tx,hash,uid,key,title,body,{requestId:row.id},at);
}
export async function releaseReservation(tx,hash,row,now,reason,status='VENCIDA',actorId='system') {
  const item = await tx.get('wood_wastes/' + row.wasteId);
  if(row.status === 'ACEPTADA' && item?.status === 'RESERVADO' && item.reservedBy === row.requesterId)
    tx.set('wood_wastes/' + row.wasteId,{...item,status:'DISPONIBLE',reservedBy:null,revision:(item.revision||0)+1,updatedAt:now});
  tx.set('withdrawal_claims/' + await hash(row.wasteId + ':' + row.requesterId),{active:false,requestId:row.id});
  tx.set('withdrawal_passes/' + row.id,{hash:null,expiresAt:0,attempts:0,lockedUntil:0});
  const updated={...row,status,updatedAt:now,closeReason:reason,activePassHash:null,passExpiresAt:0,
    timeline:movement(row,status,reason,actorId,now)};
  tx.set('withdrawal_requests/' + row.id,updated);
  await notifyParticipants(tx,hash,row,row.id+':'+status+':'+now,actorId,status==='VENCIDA'?'Solicitud vencida':'Reserva cancelada',reason,now);
  return updated;
}
export async function expireRequest(db,hash,id,now=Date.now()) {
  return db.atomic(async tx=>{
    const row=await tx.get('withdrawal_requests/'+id);
    if(!row || !['PENDIENTE','ACEPTADA'].includes(row.status)) return false;
    // An already validated physical handover is never automatically relisted.
    if(row.passValidatedAt) return false;
    const deadline=row.status==='PENDIENTE' ? row.responseExpiresAt || row.createdAt+RESPONSE_WINDOW : row.reservationExpiresAt || pickupDeadline(row.proposedDate);
    if(deadline <= now) {await releaseReservation(tx,hash,row,now,'Venció el plazo sin completar la respuesta o recolección.');return true;}
    const pending=row.reschedule;
    if(pending?.state==='PENDIENTE' && pending.expiresAt<=now) {
      if(pending.requires==='REQUESTER') {await releaseReservation(tx,hash,row,now,'La nueva fecha propuesta no recibió respuesta en el plazo indicado.');return true;}
      tx.set('withdrawal_requests/'+id,{...row,reschedule:{...pending,state:'VENCIDA'},updatedAt:now,timeline:movement(row,'REPROGRAMACION_VENCIDA','Se conserva la fecha anterior.','system',now)});
      await notifyParticipants(tx,hash,row,id+':reschedule-expired:'+pending.expiresAt,'system','Propuesta de fecha vencida','Se mantiene el acuerdo de recolección anterior.',now);
    }
    const reminderKey='reminder:' + deadline;
    if(deadline>now && deadline-now <= 24*60*60*1000 && row.reminderKey!==reminderKey) {
      // Do not overwrite a reschedule update in this same transaction.
      if(pending?.state==='PENDIENTE' && pending.expiresAt<=now) return false;
      tx.set('withdrawal_requests/'+id,{...row,reminderKey});
      await notifyParticipants(tx,hash,row,id+reminderKey,'system','El plazo vence pronto','Revisa la solicitud y su fecha límite en Mi actividad.',now);
    }
    return false;
  });
}
export async function reschedule(tx,hash,actor,operation,data,now) {
  const id=safeId(data.requestId), row=await tx.get('withdrawal_requests/'+id);
  requireThat(participant(row,actor.uid),403,'participant','Esta solicitud no pertenece a tu cuenta.');
  requireThat(row.status==='ACEPTADA' && !row.passValidatedAt,409,'state','Solo puedes reprogramar una reserva antes de validar la entrega.');
  const reason=typeof data.reason==='string'?data.reason.trim().slice(0,1000):'';
  requireThat(reason,400,'reason','Explica el motivo.');
  const owner=actor.uid===row.authorId;
  if(operation==='proposeReschedule') {
    const date=validFutureDate(data.proposedDate,now);
    requireThat(date!==row.proposedDate,400,'date','Propón una fecha distinta a la acordada.');
    requireThat(!row.reschedule || row.reschedule.state!=='PENDIENTE' || (owner && row.reschedule.requires==='OWNER'),409,'proposal','Ya hay una propuesta pendiente de respuesta.');
    const expiresAt=Math.min(now+RESPONSE_WINDOW,row.reservationExpiresAt || pickupDeadline(row.proposedDate));
    tx.set('withdrawal_requests/'+id,{...row,reschedule:{proposedBy:actor.uid,proposedDate:date,reason,requires:owner?'REQUESTER':'OWNER',expiresAt,state:'PENDIENTE'},updatedAt:now,timeline:movement(row,'PROPUESTA_FECHA',reason+' · '+date,actor.uid,now)});
    await notifyParticipants(tx,hash,row,data.operationId,actor.uid,'Propuesta de nueva fecha',date+' · '+reason,now);
    return {ok:true};
  }
  const proposal=row.reschedule;
  requireThat(proposal?.state==='PENDIENTE' && proposal.expiresAt>now && (proposal.requires==='OWNER'?owner:!owner),409,'proposal','Esta propuesta ya cambió o no te corresponde responder.');
  const action=String(data.action).toLowerCase();
  requireThat(['accept','decline'].includes(action),400,'action','Selecciona aceptar o cancelar.');
  if(action==='decline') {await releaseReservation(tx,hash,row,now,reason,'CANCELADA',actor.uid);return {ok:true,status:'CANCELADA'};}
  tx.set('withdrawal_passes/'+id,{hash:null,expiresAt:0,attempts:0,lockedUntil:0});
  tx.set('withdrawal_requests/'+id,{...row,proposedDate:proposal.proposedDate,reservationExpiresAt:pickupDeadline(proposal.proposedDate),reschedule:{...proposal,state:'ACEPTADA'},passIssuedAt:null,passExpiresAt:0,activePassHash:null,reminderKey:null,updatedAt:now,timeline:movement(row,'FECHA_ACEPTADA',reason+' · '+proposal.proposedDate,actor.uid,now)});
  await notifyParticipants(tx,hash,row,data.operationId,actor.uid,'Nueva fecha confirmada',proposal.proposedDate+'. El pase anterior ya no es válido; emite uno nuevo al realizar la entrega.',now);
  return {ok:true,status:'ACEPTADA'};
}
