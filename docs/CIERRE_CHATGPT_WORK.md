# Cierre de trabajo — WoodMatch Android y administración web

**Fecha:** 3 de noviembre de 2026 (Colombia).  
**Repositorio:** https://github.com/HOLA-GG/WoodMatch  
**Estado:** implementación integrada en main; activación completa del panel pendiente de autenticación/configuración de despliegue. Este documento actualiza el estado de los informes previos.

## Integración confirmada

| PR | Contenido | Estado | Commit de merge |
|---|---|---|---|
| [#14](https://github.com/HOLA-GG/WoodMatch/pull/14) | Auditoría documental, trazabilidad, evidencias y contrato KPI | Merged en main | 6b5c999fa35788f9a36d5ea9034b037511c8ea04 |
| [#15](https://github.com/HOLA-GG/WoodMatch/pull/15) | Dashboard, backend lector, pruebas y CI web | Merged en main | ce5eae8a4fc0cbd59baa31f43505517e8968e793 |
| [#16](https://github.com/HOLA-GG/WoodMatch/pull/16) | Administración web operativa y retirada de operador Android | Merged en main | 02e59009cff3b8461961562f23bca9912f7ec043 |

La base funcional integrada es 02e5900. Los tres PR tenían checks aprobados antes de fusionarse. Los informes de fases anteriores son históricos cuando sus pendientes fueron resueltos por esta fase.

## Qué se realizó

### Documentación y calidad

- Control documental, índice de versiones, matriz requisito/servicio/pantalla/prueba/evidencia y acciones correctivas ST-01 a ST-15.
- Inventario técnico de las doce capturas existentes, con dimensiones y límites; guía exacta de pruebas pendientes de teléfono.
- Definiciones KPI: cohorte, fórmulas, fuentes, denominadores, datos faltantes y limitaciones. No se inventan tiempos de respuesta, disponibilidad ni recepción push.
- Doce diagramas Mermaid renderizados en GitHub: ocho anteriores y cuatro secuencias nuevas de reservas, QR, notificaciones y soporte.
- Manual administrativo, transición, privacidad, operación, riesgos, regresión, respaldo y recuperación. Wiki nativa y docs/wiki se distinguen; no se asumió sincronización automática.

### Aplicación Android

- Eliminados SupportHomeScreen.kt y AdminUsersScreen.kt del módulo vigente, así como rutas, menú y navegación de administración.
- Retirados perfiles locales de demostración y detección del operador por UID/correo incrustado en el APK.
- Ayuda y participación permanece para usuarios: crear y consultar sus casos, leer respuestas y reabrir con motivo.
- Mejorados encabezado centrado, alineación y tarjeta con degradado de la paleta Material vigente.
- Comprobación clientAccess preparada para rechazar cuentas administrativas en el canal comunitario cuando se active la configuración del servidor. El APK instalado en un teléfono no cambia por fusionar código: requiere instalar la nueva compilación.

### Dashboard web

- Interfaz privada en español, adaptable a escritorio y móvil, sin modo demo ni cuentas de prueba dentro del producto.
- Resumen KPI, bandeja de casos, respuestas, prioridad, estados y reapertura con motivo.
- Moderación de publicaciones con control de revisión; no permite ocultar material reservado o entregado.
- Gestión de nombres, alertas a usuarios y eliminación de cuentas con motivo, confirmación y autenticación reciente.
- Auditoría administrativa y controles de concurrencia/idempotencia.
- Verano, primavera, otoño e invierno con colores tomados literalmente de Color.kt y Theme.kt; apariencia clara, oscura o del sistema.
- Sesión Firebase en memoria; las preferencias de tema se guardan localmente. Al salir se eliminan los datos privados de pantalla.

### Backend y protección

- Función dedicada woodmatch-admin con firma RS256, emisor/audiencia Firebase, caducidad, revocación, cuenta habilitada y lista privada de hasta dos administradores.
- CORS exacto, cuerpo máximo de 16 KiB, cuota transaccional y registros sin contraseñas ni tokens.
- Modificaciones de casos/nombres/publicaciones con comprobación de versión, auditoría y avisos transaccionales.
- Corrección de nombre mediante updateMask, conservando timestamps y demás datos del perfil.
- Eliminación reanudable: tombstone y bloqueo, limpieza de publicaciones/fotos, anonimización de entregas/casos, eliminación de avisos/dispositivos y eliminación de identidad al final. Protege ambas cuentas administrativas y la propia cuenta.
- No se ejecutó una eliminación de usuario real como prueba.

## Qué está aplicado fuera del repositorio

| Elemento | Estado comprobado |
|---|---|
| App web Firebase WoodMatch Dashboard | Registrada en el proyecto existente, sin cambiar el plan Spark |
| Cuenta del propietario en Firebase Authentication | Alta confirmada tras la acción del usuario; el UID se mantiene fuera de este informe |
| Cookiestech | Identidad existente; no necesita buzón real para autenticación por correo/contraseña |
| Supabase woodmatch-admin | Versión 1 desplegada, ACTIVE; solicitud sin origen autorizado devuelve HTTP 403 |
| Lista de dos administradores y CORS de producción | Pendiente de configurar/verificar; el servicio permanece cerrado por defecto |
| Firebase Hosting | Sin publicación confirmada; consola muestra Comenzar y la CLI local no tiene cuenta autorizada |
| Cloud Shell | Devolvió Site Unavailable en este navegador |
| Función comunitaria woodmatch activa | Conservada; no se ha desplegado todavía el cierre del acceso administrativo antiguo |
| Casos reales | Dos documentos observados en support_cases, conservados sin copia, recreación ni eliminación |

Una sesión abierta en Firebase Console no autentica automáticamente Firebase CLI ni Supabase Dashboard. Las herramientas conectadas permiten desplegar la función Supabase, pero no ofrecen una operación para establecer sus secretos; el Dashboard de Supabase solicita iniciar sesión.

## Validación y resultados

| Validación | Resultado |
|---|---|
| Node: dominio, soporte, KPI y administración | 39 aprobadas, 0 fallidas |
| Edge: backend existente, lector y administrador | 16 aprobadas, 0 fallidas; claves y proveedores de prueba |
| Tipos Deno y compilación Vite | Aprobados |
| Smoke web | Login, consultas, búsqueda, detalle, actualización con motivo, paleta exacta, móvil y salida aprobados con red interceptada |
| npm audit del dashboard | 0 vulnerabilidades reportadas |
| Enlaces/imágenes wiki, antes del cierre | 127 comprobados, 0 ausentes |
| [CI web del PR #16](https://github.com/HOLA-GG/WoodMatch/actions/runs/41739871047) | Aprobado |
| [CI general del PR #16](https://github.com/HOLA-GG/WoodMatch/actions/runs/41739870982) | Aprobado: reglas, Edge, Android debug/release/bundle, pruebas y lint |
| Pruebas físicas del teléfono y acceso completo al Hosting real | Pendientes; no sustituidas por mocks |

Las capturas automatizadas [escritorio](dashboard-desktop.png) y [móvil](dashboard-mobile.png) contienen exclusivamente fixtures de pruebas. Esos fixtures no forman parte del código publicado al navegador y no representan las cifras de producción.

## Pendientes exactos para dejar el panel operativo

1. Configurar en Supabase los secretos WOODMATCH_ADMIN_UIDS, con los dos UID reales confirmados (Cookiestech y propietario), y DASHBOARD_ORIGINS con los orígenes HTTPS exactos del sitio. No publicar UID en frontend, repositorio o capturas.
2. Probar login de ambas cuentas y acceso denegado a un tercero en woodmatch-admin. Conservar los casos existentes; no crear resultados ficticios como evidencia real.
3. Autenticar Firebase CLI o un canal de despliegue autorizado. Preparar dashboard/public/config.json con la configuración pública de la app web registrada y el endpoint woodmatch-admin. Nunca introducir cuenta de servicio ni contraseña en ese archivo.
4. Ejecutar npm ci --prefix dashboard, npm --prefix dashboard run check y npm --prefix dashboard run build. Publicar únicamente Hosting clásico con npx -y firebase-tools@latest deploy --only hosting --config dashboard/firebase.json --project woodmatch-piloto, tras verificar el sitio destino. No usar despliegue general que pueda cambiar reglas u otros servicios.
5. Comprobar la URL publicada, consultas reales, temas, móvil, sesión y rechazo de terceros. No declarar producción activa solo porque exista un registro de app o una función desplegada.
6. Solo después, desplegar el cambio del endpoint comunitario woodmatch que retira administración y valida clientAccess; distribuir el APK nuevo. Este orden evita interrumpir la atención durante la transición.
7. Incorporar evidencias físicas pendientes y anexos finales anonimizados de auditorías, no disponibles en el encargo inicial.

La autorización del usuario ya cubre la migración y el despliegue cuando funcionen las validaciones. Los pendientes anteriores son de autenticación/configuración y evidencia, no una nueva solicitud de autorización genérica.

## Rutas para continuar

- [Administración web y transición](wiki/Administracion-Web.md)
- [Control de calidad y evidencias](wiki/Control-de-calidad-y-evidencias.md)
- [Informe técnico de migración](ADMINISTRATION_MIGRATION_REPORT.md)
- [Informe de las fases anteriores](DASHBOARD_EXECUTION_REPORT.md)
- [Centro de documentación](wiki/Home.md)
