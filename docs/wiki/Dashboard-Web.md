# Dashboard web — gestión TI

> **Dashboard TI actual:** consola privada de administración web con incidentes, moderación, usuarios, alertas, auditoría y KPIs; sin modo demo en producción. Requiere Firebase Authentication y `woodmatch-admin` desplegada en Supabase. Hosting clásico todavía no tiene publicación verificada. La cuenta operativa solo puede ver y modificar información si su UID está autorizado en la lista privada del backend.

> Las definiciones de KPI abajo son útiles, pero las secciones sobre el backend lector `woodmatch-dashboard` corresponden a la fase anterior del PR #15. La referencia **vigente para desplegar** es [Administración Web](Administracion-Web.md) y [Publicación en Firebase Hosting](../PUBLICAR_DASHBOARD_HOSTING.md). La versión actual no incorpora datos ficticios en operación: los fixtures son exclusivamente pruebas automatizadas.

## Acceso y arquitectura

Frontend `dashboard/`: español, responsive, Firebase Authentication, sin selector demo. Endpoint dedicado vigente `woodmatch-admin`: valida firma RS256, issuer, audience, caducidad y sesión Firebase; consulta cuenta para revocación/deshabilitación y comprueba allowlist **en servidor**. Sin lectura Firestore directa desde el navegador. Las dos cuentas autorizadas pueden operar las funciones administrativas habilitadas en `woodmatch-admin`, bajo auditoría y verificaciones de permisos.

El backend minimiza la exposición según la operación y exige autorización a nivel servidor. Los datos personales y la respuesta detallada permanecen protegidos: solo pueden consultarse y editarse desde la cuenta administrativa autorizada.

La consola administra consultas reales a Firestore mediante una función protegida de Supabase. Para costos, límites y limpieza de cuotas, consultar los componentes de backend y el reporte de migración vigente.

## Contrato KPI

Cohorte = casos creados en [desde, hasta), UTC, máximo 366 días. Estado = instantánea actual; **no reconstrucción al cierre histórico**. Filtro de servicio usa únicamente `serviceId` explícito; los casos actuales sin ese campo se muestran Sin clasificar. No inferir servicio por texto.

Propietarios: Juan valida fuente y cálculo; Yeison revisa definición, evidencia y reporte semanal/mensual. Denominador cero y dato ausente se muestran Sin datos suficientes.

| KPI / objetivo | Numerador / denominador y fórmula | Fuente | Periodo / faltantes |
|---|---|---|---|
| Casos por servicio: demanda | Conteo por `serviceId`; sin denominador | support_cases.createdAt, serviceId | Cohorte; no clasificados visibles |
| Incidencias por estado/prioridad: cola | Conteo INCIDENCIA por status/priority | support_cases | Cohorte; no confundir con feedback |
| Resolución: seguimiento | INCIDENCIA en RESUELTO/CERRADO / INCIDENCIA en cohorte ×100 | Estado actual | Cohorte; cero denominador = sin datos |
| Reapertura observada: recurrencia | Incidencias con REABIERTO retenido / incidencias cohorte ×100 | history | Mínimo observado; historial recortado a 50 eventos |
| Primera respuesta: atención | Promedio primera respuesta inequívoca − creación | Evento no disponible inequívocamente | Sin datos suficientes; no usar updatedAt |
| Resolución temporal: duración | Promedio resolución válida − creación | Historial potencialmente incompleto | Sin datos suficientes |
| Satisfacción: percepción | Suma enteros 1–5 / cantidad válida; mostrar n | SATISFACCION.rating | Cohorte; omitir inválidos |
| Moderación: seguimiento | MODERACION con acción OCULTAR/RESTAURAR / total reportes ×100; pendiente = total − atendidos | moderationAction | Cohorte; no inferir del estado del caso |
| Operación, push, disponibilidad | Numerador y denominador de eventos confirmados | Telemetría pendiente de validar | Sin datos suficientes; no sustituir por fixtures reales |
| ST-01 a ST-15: cobertura | Conteo de casos explícitamente clasificados, no disponibilidad | serviceId | Cero casos no implica servicio saludable |

Metas de atención: propuestas académicas del catálogo, no SLA. Escalamiento: Cookiestech clasifica; Juan investiga fallo técnico; Yeison reproduce y registra evidencia; ambos aceptan cierre. No se inventan horarios de guardia.

## Publicación vigente de la consola administrativa

1. Asegurar que las dos identidades aprobadas puedan iniciar sesión Firebase y que `WOODMATCH_ADMIN_UIDS` y `DASHBOARD_ORIGINS` estén configurados en Supabase. La función `woodmatch-admin` debe rechazar sesiones no autorizadas.
2. Preparar `dashboard/public/config.json` usando `dashboard/config.example.json`, con la **configuración pública** de la aplicación Firebase Web y la URL HTTPS de `woodmatch-admin`. La plantilla del repositorio fue corregida para evitar el endpoint antiguo.
3. Revisar el [procedimiento de Hosting](../PUBLICAR_DASHBOARD_HOSTING.md). El ZIP compilado preparado por Work permite autenticarse con Firebase CLI y publicar **solo Hosting**, sin introducir credenciales privadas en el navegador.
4. Verificar en el dominio real el ingreso de Cookiestech y del propietario, el rechazo de una tercera cuenta, consultas de casos existentes, cambios de estado con motivo y protección de la sesión. No crear métricas simuladas ni eliminar usuarios reales durante pruebas de producción.
5. Completar el traspaso de permisos del endpoint comunitario y la distribución del APK actualizado **solo después** de validar la administración web real, para no interrumpir el soporte.

**Estado:** la función Supabase está desplegada; la publicación real del frontend y la aceptación de ambas cuentas permanecen pendientes de evidencia. Las capturas de `docs/dashboard-desktop.png` y `docs/dashboard-mobile.png` son capturas **de pruebas**, no cifras productivas.

## Operación, recuperación y FAQ

- Acceso denegado: confirmar cuenta habilitada, proyecto, UID en secreto de servidor y dominio; no ampliar roles en frontend.
- Sesión caducada: salir y volver a entrar. Al salir o fallar la petición se borran métricas reales de pantalla; ninguna respuesta tardía debe restaurarlas.
- Sin datos: comprobar periodo, fuente y casos sin serviceId; no reemplazar por datos demo automáticamente.
- Proveedor no responde: mostrar error, conservar evidencia del código HTTP sin cuerpos privados; revisar logs y cuotas con acceso autorizado.
- Recuperación: volver a revisión anterior de función y Hosting, sin alterar Android. Exportación Firestore privada y restauración en proyecto de prueba deben ensayarse antes del piloto; no están certificadas aquí. Álbum Android requiere ZIP local independiente.
- Privacidad: publicar solo capturas con fixtures, sin identificadores reales, sesiones, QR vigente o ubicaciones sensibles. No archivar exportaciones de producción en Git.

Pruebas de teléfono, login real y Hosting se registran como pendientes hasta obtener evidencia; tests con mocks no sustituyen aceptación en proyecto real.

Dependencias fijadas con lockfile: Firebase 13.2.0 y Vite 8.3.5; override gRPC 1.14.7 para corregir el aviso transitivo detectado por npm audit. El cliente importa exclusivamente Firebase App/Auth.
