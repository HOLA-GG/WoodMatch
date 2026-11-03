# Dashboard web — gestión TI

Estado: propuesta en revisión, **sin despliegue**. [Control y evidencias](Control-de-calidad-y-evidencias.md). [Catálogo ST-01 a ST-15](Catalogo-de-servicios-TI.md).

## Acceso y arquitectura

Frontend `dashboard/`: español, responsive, Firebase Authentication; demo separada con fixtures. Endpoint dedicado `woodmatch-dashboard`: valida firma RS256, issuer, audience, caducidad y sesión Firebase; consulta cuenta para revocación/deshabilitación y comprueba allowlist **en servidor**. Sin lectura Firestore directa desde el navegador. Cookiestech recibe solo acceso lector si se autoriza explícitamente; no obtiene administración global ni acciones de moderación web.

Respuesta minimizada: identificador de caso, tipo, servicio si existe, estado, prioridad, fechas y acciones sin actor ni motivo. Se excluyen asunto, respuesta y detalle porque texto libre puede contener datos privados. El detalle completo sigue en Android para usuarios autorizados.

La función lee páginas de 500 casos, máximo 10 000; si alcanza el límite falla sin presentar métricas parciales. Riesgo pendiente: coste/latencia de barrido; antes de escalar implementar consulta temporal indexada o agregados. Rate limit transaccional: 10 solicitudes por UID/minuto. Logs solo componente, estado y fecha, sin token, correo o UID; revisar y configurar retención en consola. Programar TTL para `dashboard_limits.expiresAt` con tipo timestamp compatible antes de uso continuado; el adaptador actual guarda milisegundos, por lo que se requiere limpieza programada si no se migra tipo. No afirmar TTL activo.

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

## Instalación y vista previa

1. `npm ci --prefix dashboard`; `npm --prefix dashboard run check`; `npm --prefix dashboard run build`.
2. Para pruebas visuales: `cd dashboard && npx playwright install chromium && npm run test:web`. Luego `npm --prefix dashboard run dev` desde la raíz. Demo arranca sin cuentas, sin backend y con rótulo permanente.
3. Para real: copiar `dashboard/config.example.json` a `dashboard/public/config.json` (ignorado por Git); usar configuración **pública** de app web registrada en el Firebase existente, endpoint HTTPS exacto de Supabase. No usar JSON de cuenta de servicio, claves privadas ni UID en frontend.
4. Secretos de función: `FIREBASE_PROJECT_ID`, `FIREBASE_SERVICE_ACCOUNT` (solo servidor), `DASHBOARD_READER_UIDS` (confirmar las tres identidades reales sin publicarlas), `DASHBOARD_ORIGINS` (orígenes exactos, sin comodines). Reutilizar proyecto existente; no asumir que configuración Android registra una app web.
5. Antes de conexión real verificar IAM mínimo para Firestore/lookup, dominio autorizado Firebase, CORS, allowlist, revocación, latencia y limpieza de cuotas. Ejecutar pruebas con cuenta permitida y cuenta ajena; no subir registros privados.
6. Tras aprobación de entorno de prueba: desplegar función dedicada y canal Firebase Hosting de vista previa con `firebase hosting:channel:deploy dashboard-review --config dashboard/firebase.json --project <proyecto-verificado>`. Configuración aislada evita cambiar hosting/reglas existentes. Confirmar sitio destino antes del comando. No se ejecutó este paso.
7. Producción requiere autorización expresa y revisión del PR; nunca usar `firebase deploy` sin alcance. No hay workflow de despliegue automático.

## Operación, recuperación y FAQ

- Acceso denegado: confirmar cuenta habilitada, proyecto, UID en secreto de servidor y dominio; no ampliar roles en frontend.
- Sesión caducada: salir y volver a entrar. Al salir o fallar la petición se borran métricas reales de pantalla; ninguna respuesta tardía debe restaurarlas.
- Sin datos: comprobar periodo, fuente y casos sin serviceId; no reemplazar por datos demo automáticamente.
- Proveedor no responde: mostrar error, conservar evidencia del código HTTP sin cuerpos privados; revisar logs y cuotas con acceso autorizado.
- Recuperación: volver a revisión anterior de función y Hosting, sin alterar Android. Exportación Firestore privada y restauración en proyecto de prueba deben ensayarse antes del piloto; no están certificadas aquí. Álbum Android requiere ZIP local independiente.
- Privacidad: publicar solo capturas con fixtures, sin identificadores reales, sesiones, QR vigente o ubicaciones sensibles. No archivar exportaciones de producción en Git.

Pruebas de teléfono, login real y Hosting se registran como pendientes hasta obtener evidencia; tests con mocks no sustituyen aceptación en proyecto real.

Dependencias fijadas con lockfile: Firebase 13.2.0 y Vite 8.3.5; override gRPC 1.14.7 para corregir el aviso transitivo detectado por npm audit. El cliente importa exclusivamente Firebase App/Auth.
