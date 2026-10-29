import { requireThat, safeId } from './domain.mjs';

// Scoped operator: never grants Firebase's broad admin claim or account access.
export const supportOperator = (actor) => actor.uid === 'yDGu1WUesuc01uUbUzc3By4qyL02'
  && actor.email === 'cookiestech@woodmatch.org';
const text = (value, max) => typeof value === 'string' ? value.trim().slice(0, max) : '';
export async function supportCall(db, actor, operation, data) {
  if (operation === 'supportAccess') return { operator: supportOperator(actor) };
  if (operation === 'createSupportCase') {
    const id = safeId(data.id), type = text(data.type, 32);
    const subject = text(data.subject, 120), detail = text(data.detail, 1500);
    requireThat(['INCIDENCIA','FEEDBACK','OPINION','SATISFACCION','MODERACION'].includes(type) && subject && detail, 400, 'case', 'Completa el tipo, asunto y detalle.');
    const publicationId = type === 'MODERACION' ? safeId(data.publicationId) : '';
    const rating = type === 'SATISFACCION' ? Number(data.rating) : null;
    requireThat(type !== 'SATISFACCION' || (Number.isInteger(rating) && rating >= 1 && rating <= 5), 400, 'rating', 'Selecciona una calificación de 1 a 5.');
    return db.atomic(async tx => {
      const existing = await tx.get(`support_cases/${id}`);
      if (existing) {
        requireThat(existing.reporterId === actor.uid, 403, 'owner', 'No puedes acceder a este caso.');
        return { id, status: existing.status };
      }
      const publication = publicationId ? await tx.get(`wood_wastes/${publicationId}`) : null;
      requireThat(!publicationId || publication, 404, 'publication', 'La publicación ya no está disponible.');
      const quotaPath = `support_limits/${actor.uid}_${new Date().toISOString().slice(0,10)}`;
      const quota = await tx.get(quotaPath);
      requireThat((quota?.count || 0) < 10,429,'limit','Ya enviaste diez casos hoy. Consulta el seguimiento de tus casos.');
      const now = Date.now();
      tx.set(quotaPath,{count:(quota?.count || 0)+1,updatedAt:now});
      tx.set(`support_cases/${id}`, { id, type, subject, detail, rating, publicationId,
        publicationTitle: publication?.title || '', reporterId: actor.uid,
        status: 'NUEVO', priority: ['INCIDENCIA','MODERACION'].includes(type) ? 'MEDIA' : 'BAJA',
        response: '', createdAt: now, updatedAt: now,
        history: [{ at: now, action: 'CREADO', actorId: actor.uid }] });
      return { id, status: 'NUEVO' };
    });
  }
  if (operation === 'mySupportCases' || operation === 'adminSupportCases') {
    requireThat(operation !== 'adminSupportCases' || supportOperator(actor), 403, 'support', 'Acceso exclusivo de soporte.');
    const rows = operation === 'mySupportCases' ? await db.list('support_cases','reporterId',actor.uid) : await db.list('support_cases');
    return { cases: rows.map(({ _path, ...row }) => row).sort((a,b) => b.updatedAt - a.updatedAt) };
  }
  requireThat(supportOperator(actor), 403, 'support', 'Acceso exclusivo de soporte.');
  const id = safeId(data.id);
  if (operation === 'updateSupportCase') {
    const status = text(data.status,32), priority = text(data.priority,16), response = text(data.response,1500);
    requireThat(['NUEVO','EN_REVISION','EN_PROGRESO','RESUELTO','CERRADO'].includes(status)
      && ['BAJA','MEDIA','ALTA','CRITICA'].includes(priority),400,'status','Selecciona un estado y prioridad válidos.');
    requireThat(!['RESUELTO','CERRADO'].includes(status) || response,400,'response','Explica la solución antes de cerrar.');
    return db.atomic(async tx => {
      const row = await tx.get(`support_cases/${id}`);
      requireThat(row,404,'missing','El caso no existe.');
      requireThat(row.updatedAt === data.expectedUpdatedAt,409,'revision','Otro cambio actualizó el caso. Recarga la bandeja.');
      const now = Date.now();
      tx.set(`support_cases/${id}`, { ...row, status, priority, response, updatedAt: now,
        history: [...(row.history || []).slice(-49),{at:now,action:status,actorId:actor.uid}] });
      return {ok:true};
    });
  }
  requireThat(operation === 'moderateEntry',400,'operation','Operación no válida.');
  const reason = text(data.reason,500), action = data.action;
  requireThat(reason && ['OCULTAR','RESTAURAR'].includes(action),400,'reason','Indica acción y motivo.');
  const caseId = safeId(data.caseId);
  return db.atomic(async tx => {
    const item = await tx.get(`wood_wastes/${id}`), row = await tx.get(`support_cases/${caseId}`);
    requireThat(item && row?.publicationId === id,404,'missing','El reporte no corresponde a esta publicación.');
    requireThat(action === 'OCULTAR' ? ['HALLAZGO','DISPONIBLE'].includes(item.status) : item.status === 'MODERADO',409,'state','El estado actual impide esta acción; revisa reservas y recolecciones.');
    const now = Date.now();
    tx.set(`wood_wastes/${id}`,{...item,status:action === 'OCULTAR' ? 'MODERADO' : item.moderationPreviousStatus,
      moderationPreviousStatus:action === 'OCULTAR' ? item.status : item.moderationPreviousStatus,
      moderationReason:reason,moderatedBy:actor.uid,moderatedAt:now,updatedAt:now,revision:(item.revision||0)+1});
    tx.set(`support_cases/${caseId}`,{...row,moderationAction:action,updatedAt:now,
      history:[...(row.history||[]).slice(-49),{at:now,action,reason,actorId:actor.uid}]});
    tx.set(`wood_wastes/${id}/events/${crypto.randomUUID()}`,{at:now,action,reason,actorId:actor.uid});
    return {ok:true};
  });
}
