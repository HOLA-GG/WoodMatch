import { requireThat } from './domain.mjs';

export async function registerDevice(db, hash, actor, profile, data) {
  const token = typeof data.token === 'string' ? data.token.trim() : '';
  requireThat(token.length >= 20 && token.length <= 4096,400,'token','No se pudo registrar este dispositivo.');
  const path = 'notification_devices/' + await hash(token);
  await db.atomic(async tx => {
    const previous = await tx.get(path);
    requireThat(data.enabled !== false || !previous || previous.uid === actor.uid,403,'owner','El dispositivo pertenece a otra sesión.');
    tx.set(path,{ uid:actor.uid,token,enabled:data.enabled === true,updatedAt:Date.now() });
  });
  return {ok:true};
}

// Each publication/device pair has a stable delivery record. Tokens never reach other users.
export async function notifyMaterial(db, hash, send, publication) {
  if (!publication || publication.status !== 'DISPONIBLE') return;
  const devices = await db.list('notification_devices');
  for (let offset = 0; offset < devices.length; offset += 8) {
    await Promise.all(devices.slice(offset,offset+8).map(async device => {
      if (!device.enabled || device.uid === publication.authorId) return;
      const profile = await db.get('users/' + device.uid);
      if (!profile || profile.deleting || !['RECEPTOR','MIXTO'].includes(profile.role)
          || await db.get('account_tombstones/' + device.uid)) return;
      const id = await hash(publication.id + ':' + device.token), path = 'notification_deliveries/' + id;
      const claim = await db.atomic(async tx => {
        const row = await tx.get(path);
        if (row?.state === 'SENT' || (row?.state === 'SENDING' && row.at > Date.now()-120000)) return false;
        tx.set(path,{state:'SENDING',at:Date.now(),publicationId:publication.id});
        return true;
      });
      if (!claim) return;
      try {
        await send(device.token,{ title:'Nuevo material disponible',body:publication.title,publicationId:publication.id });
        await db.set(path,{state:'SENT',at:Date.now(),publicationId:publication.id});
      } catch {
        await db.set(path,{state:'PENDING',at:Date.now(),publicationId:publication.id});
      }
    }));
  }
}
