# Administración web de WoodMatch

La administración se traslada al navegador. Android conserva los flujos comunitarios y **Ayuda y participación**, sin rutas de administración, asignación de roles administrativos por correo ni cuentas de demostración. La cuenta técnica Cookiestech continúa en Firebase Authentication; no necesita buzón real para iniciar sesión mediante correo y contraseña.

## Acceso y funciones

Dos identidades como máximo, configuradas por UID en el secreto servidor `WOODMATCH_ADMIN_UIDS`: operador Cookiestech y propietario. Ningún rol enviado por el navegador, nombre del perfil o campo editable concede acceso. El frontend no contiene las identidades autorizadas ni contraseñas.

- Casos: consulta, respuesta, prioridad, estado y reapertura con motivo.
- Moderación: revisar publicaciones, ocultar/restaurar con motivo y alerta al autor. Material reservado o entregado no admite estas transiciones.
- Cuentas: corregir nombre sin modificar rol, teléfono ni campos privados; alertar al titular; eliminación con motivo, confirmación del identificador y autenticación de los últimos cinco minutos.
- Auditoría: actor, operación, destino, motivo y estado, accesibles únicamente por el backend administrativo.
- Apariencia: verano, primavera, otoño e invierno; modos claro, oscuro y del sistema. Colores copiados de Color.kt y Theme.kt, sin paletas aproximadas.

## Integridad y seguridad

Función dedicada `woodmatch-admin`. Comprueba firma RS256, emisor y audiencia Firebase, caducidad, cuenta habilitada, revocación y UID en lista privada. CORS exacto, cuerpo máximo 16 KiB y límite transaccional de 30 consultas por minuto/administrador. Un solo registro de cuota por administrador evita crecimiento ilimitado.

Las modificaciones de caso, nombre y publicación se comprueban por versión y se registran junto con avisos/auditoría en una transacción. Corregir el nombre usa updateMask y conserva los timestamps del perfil. No se concede `admin=true` al cliente ni acceso general Firestore desde la web.

Eliminar una cuenta primero crea tombstone y deshabilita la identidad; después retira publicaciones/fotos, anonimiza entregas y casos propios, elimina avisos y dispositivos, y finalmente elimina la identidad. Una operación fallida conserva auditoría STARTED y puede reintentarse con el mismo identificador hasta COMPLETED. Las cuentas administradoras y la propia cuenta están protegidas frente a esta acción. No se borraron usuarios reales para probar esta función.

Las alertas se guardan en la bandeja existente de Android y quedan disponibles para el mantenimiento FCM. La entrega push depende de su configuración y del dispositivo; no se presenta una escritura en Firestore como entrega confirmada.

La función antigua `woodmatch` no construye la marca interna `webAdmin`; el cambio de servidor propuesto cierra las operaciones de operador en el endpoint comunitario, incluso desde un APK antiguo. **Aplicar ese cierre solo después de validar acceso al panel web**, para evitar interrumpir la atención.

## Datos y transición

No hay copia o recreación de casos. La consola de Firestore mostró dos documentos en support_cases en la revisión de esta fase; no se alteraron. El dato debe contrastarse de nuevo al abrir el panel real.

La app web WoodMatch Dashboard ya fue registrada en el proyecto existente. La sesión Google de Firebase Console no constituye una cuenta en Firebase Authentication: el correo indicado por el propietario no aparece todavía en la lista de usuarios. Debe completarse su alta segura y después registrar su UID en servidor junto con Cookiestech. No se guardó la contraseña compartida en la conversación.

## Despliegue y cierre

1. Aprobar CI web y CI Android del PR de migración.
2. Completar alta y acceso de las dos cuentas sin transmitir contraseñas por chat.
3. Establecer `WOODMATCH_ADMIN_UIDS` (exactamente dos UID) y `DASHBOARD_ORIGINS` (orígenes Hosting exactos) como secretos de la función. Reutilizar FIREBASE_PROJECT_ID/FIREBASE_SERVICE_ACCOUNT únicamente dentro del servidor.
4. Publicar función dedicada y verificar acceso permitido/denegado y datos reales sin ejecutar eliminaciones de usuarios reales como prueba.
5. Construir `dashboard/` con config.json pública de la app web registrada. La configuración Firebase cliente no es una cuenta de servicio y no incluye credenciales privadas.
6. Publicar únicamente Hosting clásico, con `npx -y firebase-tools@latest deploy --only hosting --config dashboard/firebase.json --project woodmatch-piloto`, desde CLI autenticada y tras verificar el sitio destino. No usar App Hosting ni cambiar el plan de facturación.
7. Comprobar login, sesión, consultas, tema y vista móvil en la URL publicada; después cerrar los permisos del endpoint antiguo y distribuir el APK nuevo.

Estado de publicación: no declarar el sistema operativo hasta completar todos los pasos anteriores. La CLI local no tiene cuenta autorizada y Cloud Shell devuelve Site Unavailable en este navegador; disponer de sesión de consola no habilita automáticamente la CLI.

## Pruebas

`backend-tests/admin.test.mjs`: permisos, motivos, concurrencia/idempotencia, primera respuesta durable, conservación de timestamps al corregir nombre, moderación de reservas, protección de cuentas y eliminación reanudable.

`backend-tests/admin-edge.test.ts`: firma/token, cuenta revocada/deshabilitada, lista privada y CORS mediante fixture firmado.

`dashboard/smoke.cjs`: login, consulta y actualización con motivo en fronteras de red simuladas, paleta exacta, móvil y limpieza al salir. Las capturas automatizadas contienen fixtures; la aplicación publicada no incorpora un modo demo ni accesos de prueba.
