# Protocolos de implementación · Firebase y Supabase para WoodMatch 2.0

**Corte de verificación:** 24 de octubre de 2026 (hora de Bogotá). **Alcance:** piloto académico con Firebase Spark y Supabase Free, sin facturación. Este protocolo corresponde a la app Android de la raíz del repositorio; `Kotlin_App/` y `functions/` son históricos. Se complementa con [PILOTO.md](PILOTO.md) y [VALIDACION_PILOTO.md](VALIDACION_PILOTO.md).

## 1. Arquitectura y estado comprobado

| Componente | Responsabilidad | Estado comprobado |
| --- | --- | --- |
| Firebase Authentication | Identidad por correo y contraseña; emite ID tokens | Proyecto Firebase y proveedor todavía no verificados en este entorno. |
| Cloud Firestore | Perfiles, publicaciones, solicitudes, operaciones y cuotas; lecturas Android regidas por `firestore.rules` | Reglas e índices están en Git; no se verificó su despliegue remoto. |
| Supabase Edge Function `woodmatch` | Verifica el token de Firebase y la cuenta; ejecuta todas las operaciones de escritura, IA y fotos | Activa, versión 1, en el proyecto `usdbewyttzuudundxyvk`; falta una prueba autenticada de extremo a extremo. |
| Supabase Storage | Bucket privado `woodmatch-photos`, JPEG de hasta 1 MiB, accesible mediante la función | Bucket creado, `public=false`, límite de 1 048 576 bytes. |
| Android | Lee Firestore con reglas; llama a la Edge Function; configura Firebase con `google-services.json` | El código usa `applicationId=com.example.kotlinapp.pilot`; no se verificó un APK conectado a estos servicios. |

URL de la función: `https://usdbewyttzuudundxyvk.supabase.co/functions/v1/woodmatch`. Es configuración pública del cliente, **no** una clave secreta.

En Supabase consta la migración remota `20261024015210_create_private_woodmatch_photos_bucket`. El repositorio contiene `supabase/migrations/202610230001_private_photos.sql`. **No ejecutar `supabase db push` sin conciliar este desfase**: podría aplicar de nuevo la definición del bucket o dejar dos historiales para una misma intención. El bucket remoto tiene los atributos principales esperados, pero se debe comprobar también `allowed_mime_types` antes de dar por cerrada la conciliación.

## 2. Protocolo Firebase

### 2.1 Identificar el proyecto correcto

1. En una estación administrada por el responsable, autenticar el CLI y listar proyectos: `npx -y firebase-tools@latest login` y `npx -y firebase-tools@latest projects:list`.
2. Registrar en la bitácora privada el **Project ID**, plan Spark, región de Firestore, persona responsable y fecha. Si se reutiliza un proyecto del APK anterior, confirmar expresamente que corresponde a WoodMatch antes de alterar reglas. No crear un segundo proyecto ni cambiar el plan por deducción.
3. Habilitar Authentication con correo/contraseña y crear la base Firestore en la región elegida. Mantener Cloud Functions y Firebase Storage sin activar para este piloto.
4. Registrar la app Android con el paquete exacto `com.example.kotlinapp.pilot`. Recuperar su configuración mediante `npx -y firebase-tools@latest apps:sdkconfig ANDROID APP_ID --project PROJECT_ID` y guardarla en `app/google-services.json`. Verificar que `project_info.project_id` coincida con PROJECT_ID y que el cliente Android del JSON corresponda al paquete piloto. El archivo está ignorado por Git; no versionarlo.
5. Crear una cuenta de servicio dedicada para la Edge Function, con los permisos mínimos que permiten las llamadas actuales a Firestore y Firebase Authentication. Revisar y probar los roles propuestos en [PILOTO.md](PILOTO.md) (`roles/datastore.user` y `roles/firebaseauth.admin`) en ese proyecto. Descargar la clave únicamente a un almacén privado; no ponerla en Git, el APK ni mensajes.

### 2.2 Validar y publicar reglas

1. Ejecutar las pruebas de reglas con Firebase Emulator en un proyecto `demo-*`. La integración está en `functions/rules.integration.test.js` y el flujo de CI en `.github/workflows/pilot.yml`. Su ejecución local anterior quedó bloqueada por la descarga del emulador; no convertir ese bloqueo en aprobación.
2. Revisar `firestore.rules` e `firestore.indexes.json` contra el proyecto escogido. En particular, confirmar lectura sólo por usuarios autorizados y denegación de escrituras directas de publicaciones y solicitudes.
3. Publicar **únicamente** Firestore rules e indexes, con el Project ID explícito: `npx -y firebase-tools@latest deploy --project PROJECT_ID --only firestore:rules,firestore:indexes`. El script `scripts/deploy-pilot.sh` también incluye pasos Supabase y se reserva hasta conciliar sus migraciones.
4. En dos cuentas de prueba, confirmar que el titular lee su perfil, que otra cuenta no lo lee y que una escritura directa a `wood_wastes` o `withdrawal_requests` falla. Confirmar que la consulta real del catálogo usa el índice desplegado. Registrar capturas de respuesta sin tokens ni datos personales.

**Salida Firebase:** Project ID y paquete coinciden; Authentication y Firestore operan; pruebas de reglas aprobadas; despliegue registrado; las cuatro comprobaciones de acceso pasan. Si falla una, detener la distribución del APK conectado.

## 3. Protocolo Supabase

### 3.1 Conciliar lo que ya existe

1. Confirmar que el proyecto `usdbewyttzuudundxyvk` pertenece al equipo y seguirá en Free.
2. Comparar la migración remota `20261024015210_create_private_woodmatch_photos_bucket` con `supabase/migrations/202610230001_private_photos.sql`. Verificar id, `public=false`, límite de 1 MiB y `allowed_mime_types=['image/jpeg']`. Registrar cuál migración será la fuente de verdad del repositorio; **no borrar ni reescribir el historial remoto sin una decisión de mantenimiento explícita**.
3. Inspeccionar la versión desplegada de `woodmatch` y compararla con los cuatro archivos de `supabase/functions/woodmatch/`. La versión activa por sí sola no demuestra que el código desplegado sea idéntico a `main`.
4. Mantener `verify_jwt=false` en el gateway **porque el código de la función verifica por sí mismo los JWT RS256 de Firebase, su emisor, audiencia, proveedor y estado de cuenta**. Esta excepción no habilita acceso anónimo a las operaciones. Probar que las peticiones sin token, con token inválido y con token de otro proyecto obtienen 401.

### 3.2 Secretos y despliegue

1. En un archivo privado fuera del repositorio, preparar `FIREBASE_PROJECT_ID`, `FIREBASE_SERVICE_ACCOUNT` (JSON de una sola línea), `PASS_SECRET` aleatorio de al menos 32 caracteres, `GEMINI_SERVER_KEY`, `GEMINI_MODEL` y `PLANTNET_SERVER_KEY`. La función obtiene `SUPABASE_URL` y `SUPABASE_SERVICE_ROLE_KEY` de su entorno; ninguna clave administrativa va al APK. Rotar cualquier clave que haya estado en historial público.
2. Configurar los secretos con el CLI autenticado: `supabase secrets set --env-file /ruta/privada/piloto.env --project-ref usdbewyttzuudundxyvk`. Comprobar nombres y presencia sin imprimir valores.
3. Tras comparar fuentes, desplegar sólo la función si hay diferencia: `supabase functions deploy woodmatch --project-ref usdbewyttzuudundxyvk --no-verify-jwt`. No ejecutar el paso `db push` del script combinado hasta resolver la divergencia de migraciones.
4. Probar la función con una cuenta Firebase de prueba y después con otra. Registrar código HTTP, operación y `correlationId`; ocultar tokens, fotos y cuerpos privados. Una respuesta 503 de `setup` indica que aún falta configuración, no éxito de implementación.
5. Comprobar foto JPEG válida, rechazo de formato/tamaño, URL firmada de duración corta, acceso denegado a fotos ajenas, límite de cuota y limpieza de archivos tras borrado de cuenta. No exponer el bucket como público para salvar una prueba fallida.

**Salida Supabase:** migración conciliada; código desplegado corresponde al repositorio; secretos presentes; rechazo de tokens inválidos; operaciones autenticadas y fotos privadas probadas. La presencia del bucket y de la función solamente cubre una parte de esta salida.

## 4. Unión Android y aceptación

1. Compilar de nuevo con el JSON Firebase correcto y `WOODMATCH_API_URL=https://usdbewyttzuudundxyvk.supabase.co/functions/v1/woodmatch`. Añadir configuración después de instalar un APK no cambia ese APK.
2. Instalar la misma versión en dos teléfonos o perfiles independientes. Crear cuentas de prueba con roles generador y receptor. Verificar registro, reinicio de sesión y lectura de perfiles.
3. Publicar una oferta con foto y ubicación confirmada; verla desde la segunda cuenta. Editarla y confirmar que el cambio se refleja. Ejecutar solicitud, aceptación, QR/código, validación y las dos confirmaciones; comprobar el comprobante y la retirada del catálogo.
4. Ensayar doble aceptación concurrente, pase vencido y reemitido, cinco códigos erróneos, pérdida de conexión, permisos denegados y cambio de cuenta. Para IA, comprobar respuesta real o degradación controlada y cuotas sin exponer claves.
5. Guardar en la bitácora: versión/commit, Project ID Firebase, referencia Supabase, fecha, modelos/Android, resultado de cada paso y `correlationId` de errores saneados. El criterio es evidencia de dos dispositivos y servicios reales, no una compilación exitosa.

**Si falla:** suspender la distribución conectada, conservar el APK de prueba anterior y sus identificadores, revisar logs por `correlationId`, corregir en rama y repetir el caso. Nunca ampliar reglas de Firestore ni hacer público Storage como solución rápida.

## 5. Pendientes externos precisos

Para cerrar la implementación remota faltan confirmar el Project ID Firebase y acceso autorizado al proyecto, registrar la app Android, proveer la cuenta de servicio y secretos de proveedores, conciliar el historial de migraciones, ejecutar pruebas de reglas y realizar la aceptación entre dos teléfonos. Hasta entonces el estado correcto es **infraestructura Supabase parcial, integración integral sin verificar**.

Referencias oficiales consultadas: [Firebase Security Rules](https://firebase.google.com/docs/rules/manage-deploy), [seguridad de Firestore](https://firebase.google.com/docs/firestore/security/get-started), [configuración de funciones Supabase](https://supabase.com/docs/guides/functions/function-configuration), [secretos Supabase](https://supabase.com/docs/guides/functions/secrets), [seguridad de Edge Functions](https://supabase.com/docs/guides/functions/auth).
