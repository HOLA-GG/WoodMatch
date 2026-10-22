'use strict';
function mayTransition(actor, item, request, action) {
  if (!actor || item.authorId !== actor || request.authorId !== actor || request.wasteId !== item.id) return false;
  if (action === 'accept') return item.status === 'DISPONIBLE' && request.status === 'PENDIENTE';
  if (action === 'reject') return request.status === 'PENDIENTE';
  if (action === 'complete') return item.status === 'RESERVADO' && item.reservedBy === request.requesterId && request.status === 'ACEPTADA' && Number.isFinite(request.passValidatedAt);
  return false;
}
function validPass(pass, uid, now) {
  return pass && pass.authorId === uid && !pass.usedAt && pass.expiresAt > now;
}
module.exports = {mayTransition, validPass};
