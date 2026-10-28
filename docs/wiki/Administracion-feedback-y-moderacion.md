# Administración, feedback y moderación

## Estado de administración

La aplicación incluye el modelo `ADMIN`, una ruta y una pantalla inicial de usuarios. No basta asignar el texto `ADMIN` en Firestore: la autorización administrativa debe depender de un **custom claim** firmado por Firebase Admin y reglas que lo verifiquen.

## Diseño seguro requerido

- Una operación de backend protegida por Firebase Admin asigna o revoca el claim, tras una comprobación de operador autorizada.
- Firestore permite listar/editar perfiles solo cuando `request.auth.token.admin == true`.
- El cliente no puede autoasignarse ADMIN ni cambiar roles propios.
- El administrador inicia sesión nuevamente para renovar el token después de recibir/revocar el claim.
- El primer administrador se aprovisiona por procedimiento privado y auditado; no por un endpoint público ni por un campo enviado por Android.

## Módulo de feedback

Cada reporte de feedback debe contener: `id`, autor, categoría, texto limitado, captura opcional con consentimiento, versión de app, fecha, estado y notas internas. Estados recomendados: **Nuevo**, **En revisión**, **En progreso**, **Resuelto**, **Descartado**.

La consola de administración debe permitir filtrar por estado/categoría, asignar responsable, añadir nota interna, cambiar estado y consultar métricas agregadas. No debe mostrar secretos, tokens, ubicaciones exactas no necesarias ni registros completos de diagnóstico.

## Moderación

La moderación puede ocultar una publicación, bloquear temporalmente una cuenta o solicitar corrección. Cada decisión registra operador, motivo, fecha y transición. No elimina datos sin proceso de retención/aprobación.
