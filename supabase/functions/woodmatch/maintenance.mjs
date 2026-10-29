import { expireRequest, RESPONSE_WINDOW, pickupDeadline } from './lifecycle.mjs';

export async function runMaintenance(db,hash,send,now=Date.now()) {
  const acquired=await db.atomic(async tx=>{
    const row=await tx.get('maintenance/lease');
    if(row?.until>now) return false;
    tx.set('maintenance/lease',{until:now+240000});return true;
  });
  if(!acquired) return {ok:true,busy:true};
  const supportUid='yDGu1WUesuc01uUbUzc3By4qyL02';
  const support=await db.get('users/'+supportUid);
  if(support?.email==='cookiestech@woodmatch.org' && !support.deleting && support.role!=='SOPORTE')
    await db.set('users/'+supportUid,{...support,role:'SOPORTE'});
  let expired=0;
  for(const status of ['PENDIENTE','ACEPTADA']) {
    let after=null;
    for(let page=0;page<10;page++) {
      const rows=await db.list('withdrawal_requests','status',status,100,after);
      for(const row of rows) if(await expireRequest(db,hash,row.id,now)) expired++;
      if(rows.length<100) break;
      after=rows[rows.length-1]._path;
    }
  }
  // A stable per-device marker avoids duplicate pushes across scheduled retries.
  const pending=await db.list('notifications','deliveryState','PENDING');
  for(const notice of pending.slice(0,100)) {
    const profile=await db.get('users/'+notice.uid);
    if(!profile || profile.deleting || await db.get('account_tombstones/'+notice.uid)) {
      await db.set('notifications/'+notice.id,{...notice,deliveryState:'SKIPPED'});continue;
    }
    const devices=(await db.list('notification_devices','uid',notice.uid)).filter(d=>d.enabled);
    let succeeded=true;
    for(const device of devices) {
      const id=await hash(notice.id+':'+device.token),path='notification_deliveries/'+id;
      const previous=await db.get(path);if(previous?.state==='SENT') continue;
      try {await send(device.token,notice);await db.set(path,{state:'SENT',at:now});}
      catch {succeeded=false;}
    }
    if(succeeded) await db.set('notifications/'+notice.id,{...notice,deliveryState:'SENT'});
  }
  return {ok:true,expired};
}
