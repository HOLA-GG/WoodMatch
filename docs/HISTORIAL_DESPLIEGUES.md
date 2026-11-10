# Historial de desarrollo y despliegues

## 2026-11-09 · Inicio de sesión local y Edge Function v13

### Incidente y causa

El APK `2.3.0-pilot` (versionCode 8) autenticaba correctamente en Firebase, pero el inicio de sesión llamaba después a `clientAccess`. La versión 12 desplegada de `woodmatch` no admitía esa operación: respondía HTTP 400 y Android cerraba la sesión mostrando un mensaje genérico.

La corrección de `index.ts` ya estaba versionada en el commit `2674a8f131fc7431112fa254a10f870c2649cffe`, pero no se había aplicado al servidor. Compilar un APK no despliega automáticamente las Edge Functions.

### Cambios y evidencias

| Componente | Estado registrado |
|---|---|
| Android local | Firebase cliente y URL pública persistente; validación opcional `WOODMATCH_REQUIRE_CONNECTED=true` |
| Compilación | AGP 9.5.1, Gradle 9.6.0, daemon Java 25, compatibilidad Java de la app 21 |
| Memoria | Gradle 1536 MiB, Kotlin 1024 MiB, un trabajador, sin paralelismo; daemon inactivo hasta 10 minutos |
| Supabase `woodmatch` | Versión 12 → 13; únicamente tres líneas añadidas en `index.ts` |
| Pruebas Android | `:app:installDebug :app:testDebugUnitTest` aprobado; 1 min 29 s |
| Pruebas Edge | 10 aprobadas, 0 fallidas; incluyen `clientAccess`, sesiones inválidas y administradores web |
| Dispositivo físico | Z2450 / Android 14: inicio de sesión y pantalla principal verificados |
| Evidencia servidor | `clientAccess` HTTP 200 a las 2026-11-10 02:48:23 UTC (2026-11-09 21:48:23 Bogotá) |

La nueva versión conserva autenticación Firebase, comprobación de cuenta, perfil y restricciones de acceso. `verify_jwt=false` permanece porque el handler verifica los JWT de Firebase; no se habilitaron accesos anónimos.

La versión de despliegue de Supabase, la versión del APK y los commits Git son identificadores distintos. El campo histórico `version` de los logs del servidor todavía dice `2.0.0-pilot`; no es el número de despliegue de Supabase.

### Trazabilidad y reversión

El [registro de despliegue](deployments/2026-11-09-woodmatch-v13.json) contiene versiones, hashes reportados por Supabase y checksum del APK, sin credenciales ni datos de usuarios.

Se conservaron los bundles completos anterior y nuevo en `release-assets/deployments/woodmatch-v12.json` y `woodmatch-v13.json`. Son copias locales ignoradas por Git; conservarlas en almacenamiento privado antes de limpiar esa carpeta. Los secretos del servidor no forman parte de esos archivos y no se modificaron.

Para una reversión de emergencia autorizada, redesplegar los archivos del bundle anterior con su entrypoint y configuración. Supabase creará una nueva versión de despliegue; no disminuirá el contador a 12. Esa reversión recuperaría también el fallo de `clientAccess`, por lo que no es una solución al inicio de sesión del APK actual. No borrar datos ni restablecer el proyecto como mecanismo de reversión.

### Alcance pendiente

El despliegue conservó las dependencias existentes del servidor. Se detectaron diferencias previas respecto al repositorio en `firestore.mjs`, `cleanup.mjs` y `support.mjs`; deben revisarse antes de un despliegue general. No se sustituyeron esos módulos durante esta corrección.

Quedan pendientes la validación completa de fotografías, Gemini/Pl@ntNet, retiros, panel web y GitHub Actions. El inicio de sesión aprobado no demuestra esos otros flujos. Los commits de este trabajo son locales en `main`; publicar por push y validar CI corresponde al siguiente paso acordado.

No se incluyen en Git `google-services.json`, configuración personal de Gradle, claves del servidor, contraseñas, tokens de sesión ni certificados privados.

## 2026-11-09 · Revisión de interfaz y envíos repetidos

Se corrigieron alineación y recortes de navegación/contadores, se añadieron validaciones locales de solicitud y guardas inmediatas contra pulsaciones repetidas. El backend permanece en la versión 13.

Se aprobaron 43 pruebas locales de dominio/soporte/administración, incluidas ráfagas de 50 operaciones, y las pruebas unitarias Android. El APK final quedó instalado y revisado en el Z2450. El [informe de QA](QA_UI_Y_ENVIO_2026-11-09.md) registra alcance, límites, checksum y verificaciones físicas. Estas pruebas no sustituyen una prueba de carga real ni una auditoría de seguridad completa.
