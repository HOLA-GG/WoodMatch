# Roles y ciclo de aprovechamiento

| Rol | Puede hacer |
|---|---|
| Generador | Ofrecer madera/residuos, atender solicitudes y validar pase. |
| Receptor | Buscar y solicitar material, emitir pase y confirmar retiro. |
| Mixto | Reúne las acciones de generador y receptor. |
| Administrador | Gestión interna únicamente cuando posee claim administrativo de Firebase y backend de administración activo. |

## Flujo de material

1. Una persona registra un hallazgo o una oferta con foto y ubicación confirmada.
2. El receptor solicita una oferta disponible; los hallazgos no se reservan.
3. El generador acepta una solicitud; la publicación pasa a reservada de forma atómica.
4. El receptor emite un QR/código temporal; vence en diez minutos y uno nuevo invalida el anterior.
5. El generador valida el pase.
6. Ambas partes confirman la entrega. Se crea comprobante y la publicación deja el catálogo comunitario.
7. Cancelar una solicitud libera la reserva cuando corresponde.

Una publicación reservada no se edita. Solo la persona autora puede convertir un hallazgo en oferta y debe confirmar autorización.
