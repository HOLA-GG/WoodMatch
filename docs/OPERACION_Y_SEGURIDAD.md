# WoodMatch 1.1: operación, seguridad y publicación

Esta entrega prepara código y artefactos de prueba. No representa un despliegue Firebase, publicación en Play ni certificación de seguridad o cumplimiento legal. El contacto `Cookietech@woodmatch.org` es un ejemplo solicitado por el equipo: no se ha verificado que exista ni que sea atendido. Mientras el aviso conserve ese contacto y la indicación de prototipo, usar exclusivamente datos ficticios.

## Cambios incluidos

- Autenticación Firebase con rechazo de errores, sin acceso por contraseña simulada ni rol administrador inferido del correo. Inicio sin sesión y modo invitado sin operaciones protegidas.
- Los cambios de roles requieren administración de servidor; el administrador debe usar un custom claim `admin` gestionado por un operador de confianza. No autoasignarlo desde la aplicación.
- Publicación confirmada por Firestore; fotografías importadas a almacenamiento privado, limitadas a 15 MB de entrada, decodificadas con muestreo, recomprimidas sin metadatos EXIF y subidas a Storage antes de confirmar publicación. La cámara externa usa FileProvider sin permiso de cámara propio ni acceso general a la galería. Se retiraron los permisos de ubicación del manifiesto: el flujo activo usa selección manual de sector, no GPS. No se ha reproducido el incidente del APK anterior en un dispositivo.
- Reglas de mínimo privilegio: perfiles privados; solicitudes legibles por sus participantes; tokens inaccesibles al cliente; cambios de solicitudes y reservas exclusivamente mediante servidor. El catálogo compartido requiere una cuenta y guarda referencias de sector, sin teléfono o domicilio del autor. Solo se leen publicaciones con schemaVersion=2: los documentos antiguos no se migran automáticamente. Antes de migrarlos, respaldar con acceso restringido y eliminar teléfonos, domicilios y coordenadas precisas de la copia pública.
- “Mi actividad” reúne publicaciones propias, solicitudes e historial remoto. Se puede desactivar material disponible. El circuito antiguo de reportes locales sigue separado; no se ha migrado automáticamente. Las solicitudes antiguas bajo subcolecciones tampoco se migran a la nueva colección raíz; revisar y migrar con privilegios de servidor si existen datos previos.
- Retiro: el receptor solicita, el generador acepta/rechaza; el servidor reserva atómicamente. El receptor emite el QR, el generador lo valida y después confirma la entrega. Tokens aleatorios de 256 bits, almacenados como hash, válidos diez minutos, un solo uso y reemplazables; emitir otro invalida el anterior. La operación no certifica identidad física ni autoriza acceso a predios por sí sola.
- Funciones de IA con secretos del servidor, límites de entrada/salida, tiempo de espera, autenticación, App Check y cuota compartida de 20 consultas por cuenta/día. Las funciones de IA deben configurarse y probarse con proveedores reales antes de aceptarlas como operativas. Se conserva orientación local etiquetada como tal.
- Release con R8 y reducción de recursos. No hay secretos de Gemini/PlantNet en BuildConfig; los antiguos campos están vacíos. La ofuscación no vuelve invulnerable al cliente; el control de acceso y los secretos permanecen en servidor.
- App Check: Play Integrity en release y proveedor de depuración solamente en debug. No es una garantía contra toda manipulación ni reemplaza autorización.
- Exportación de datos y eliminación de cuenta mediante servidor, con confirmación y autenticación reciente para eliminar. Una eliminación incompleta deja la cuenta deshabilitada y marcada para que el operador complete la limpieza. Se conserva una marca técnica mínima de revocación (UID y expiresAt) para impedir el uso de tokens aún válidos tras borrar la cuenta; configurar TTL en account_tombstones.expiresAt, con expiración a dos horas y eliminación eventual del proveedor. Declarar esta retención técnica en la política productiva. Las reglas de Storage consultan Firestore y requieren habilitar la integración de permisos entre servicios al desplegarlas.
- Visores web con bloqueo de tráfico mixto, cancelación de errores TLS, acceso local restringido y construcción de los popups con texto en vez de insertar HTML aportado por usuarios.

## Configuración que falta para operar entre teléfonos

1. Definir un proyecto Firebase de pruebas y su región, revisar facturación/costos de Functions y Storage y habilitar Authentication por correo/contraseña, Firestore y Storage. No usar datos reales para la validación inicial.
2. Registrar la aplicación Android `com.example.kotlinapp`. Descargar su configuración cliente en `app/google-services.json`. No subir el archivo a Git. No añadir credenciales de cuenta de servicio al APK.
3. Registrar App Check para la aplicación. Para debug, registrar el token de depuración únicamente en un proyecto de pruebas. Para release, configurar Play Integrity y las huellas del certificado de firma; validar el canal real de distribución. No desactivar App Check en producción para “hacer que funcione”.
4. Usar Node 22 para las funciones y ejecutar `npm ci --prefix functions`. El Node 22 de pruebas está disponible en `/workspace/.woodmatch-tools/node22/bin`; anteponer ese directorio al PATH. Las pruebas de política se ejecutan con Node 22; el runtime remoto aún requiere validación tras el despliegue. El lockfile fija las dependencias descargadas.
5. Proporcionar los secretos con Secret Manager: `firebase functions:secrets:set GEMINI_SERVER_KEY --project PROYECTO` y `firebase functions:secrets:set PLANTNET_SERVER_KEY --project PROYECTO`. Los comandos solicitan los valores de manera interactiva; no incluirlos en comandos guardados, chats ni archivos versionados. Un `.env` de la app no conecta de forma segura estas APIs.
6. Opcionalmente definir `GEMINI_MODEL` en la configuración de funciones; valor inicial `gemini-2.5-flash`. Confirmar disponibilidad, costos y condiciones del modelo y endpoints de Pl@ntNet en las cuentas reales. La integración remota no se ha ejercitado con claves reales en esta entrega.
7. Desplegar reglas y funciones en el proyecto de pruebas mediante el script `scripts/deploy-firebase.sh`. Este script requiere `FIREBASE_PROJECT`, que es un identificador no secreto. La autenticación del CLI debe configurarse fuera del repositorio.
8. Volver a compilar el APK después de añadir `google-services.json` e instalar esa nueva compilación: un APK generado sin el JSON no se conecta por añadir archivos `.env` posteriormente en el ordenador. Crear dos cuentas de prueba, una generadora y otra receptora. Publicar desde un teléfono y comprobar recepción de datos y foto en otro. Ejecutar el recorrido completo de solicitud, aceptación, emisión, escaneo y cierre. Probar doble aceptación y doble escaneo simultáneos, QR vencido/reemitido, permisos denegados, pérdida de conexión y cambio de cuenta.

Las Functions usan `us-central1`, ubicación predeterminada, y el cliente llama a esa misma región. Elegir región y actualizar ambos lados antes del despliegue si el equipo acuerda otra; documentar las transmisiones internacionales correspondientes.

## Pruebas locales

En este entorno cloud:

```bash
source /workspace/.woodmatch-tools/activate.sh
cd /workspace/WoodMatch
./gradlew --no-daemon --max-workers=2 assembleDebug testDebugUnitTest lintDebug assembleRelease
npm ci --prefix functions
node --test functions/auth.test.js functions/policy.test.js
functions/node_modules/.bin/firebase emulators:exec --project demo-woodmatch --only firestore 'cd functions && node --test rules.integration.test.js'
```

El último comando usa un proyecto ficticio y no debe apuntar al proyecto productivo. Las pruebas de reglas requieren descargar el emulador desde `storage.googleapis.com`; esa descarga fue rechazada por la red del entorno actual. No considerar esas pruebas aprobadas hasta ejecutarlas. Las pruebas de política no sustituyen pruebas de Functions, reglas y concurrencia reales.

Artefactos: APK debug en `app/build/outputs/apk/debug/app-debug.apk`; release sin firma en `app/build/outputs/apk/release/`; mapa R8 en `app/build/outputs/mapping/release/mapping.txt`. Custodiar el mapa fuera de la distribución pública y usarlo para interpretar trazas de producción.

## Firma y publicación

No se inventó ni se usó una clave de firma productiva. `assembleRelease` produce un APK sin firma; no es instalable hasta firmarlo con la identidad del propietario. El APK debug sí se firma para pruebas. Configurar una clave de carga custodiada y Play App Signing, o el canal de distribución acordado. Conservar el `applicationId` y certificado de la aplicación instalada si se desea actualizarla sin desinstalar. No es posible prometer compatibilidad con un APK anterior sin conocer su firma.

Antes de distribución pública: sustituir aviso y contacto ficticio por información verificada; resolver los bloqueos de pruebas; revisar la ficha de seguridad de datos de Play, política de privacidad publicada y mecanismo de eliminación de cuenta fuera de la app cuando el canal lo exija; validar accesibilidad y el funcionamiento de cámara/escáner en dispositivos de referencia. No publicar credenciales de firma, tokens de descarga ni mapas de ofuscación junto al APK.

## Privacidad y jurisdicción colombiana

Marco de referencia para que el responsable valide el piloto: [Ley 1581 de 2012](https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=49981), su reglamentación compilada en el Decreto 1074 de 2015 y las orientaciones de la [Superintendencia de Industria y Comercio](https://www.sic.gov.co/tema/proteccion-de-datos-personales). Estas referencias no constituyen un dictamen jurídico ni una revisión exhaustiva de modificaciones posteriores.

Antes de tratar datos reales se deben definir: identidad y domicilio del responsable, canal atendido para consultas/reclamos, finalidades, datos obligatorios/opcionales, autorización demostrable cuando proceda, política de retención/eliminación y respaldo, encargados y regiones, transmisión/transferencia internacional, atención de derechos y procedimiento de incidentes. La inscripción en RNBD depende de la naturaleza y condiciones del responsable; no se presume obligatoria para cualquier prototipo. Si el producto incorpora comercialización, menores de edad u otros tratamientos, se requiere un análisis específico adicional. Los permisos Android no sustituyen la autorización y transparencia exigibles en protección de datos.

El texto de prueba explica cámara, fotos, visibilidad, datos de cuenta, recomendaciones orientativas y QR. Storage usa enlaces de descarga con token: quien reciba el enlace puede ver la foto; no usar fotografías privadas ni datos sensibles. La política productiva debe reflejar esta característica, o cambiar el mecanismo de descarga a uno que exija autorización por cada acceso.

La eliminación automatizada no sustituye una política de retención legal. Si se debe conservar determinada evidencia, diseñar y documentar la excepción antes de habilitar uso real, sin afirmar que se elimina lo que se retiene. El responsable debe contar con un procedimiento para recuperar eliminaciones fallidas de cuentas deshabilitadas, limpiar archivos huérfanos y atender correcciones de datos.

## Límites conocidos

No se ha desplegado infraestructura ni probado el flujo con Firebase real, proveedor IA real o dispositivo Android. No hay garantía de resistencia total a ingeniería inversa. La edición completa de publicaciones, cancelación y reprogramación de retiros, migración de reportes locales antiguos y una auditoría de accesibilidad visual no forman parte de las funciones cerradas de esta entrega. Un backend/configuración ausente debe producir un mensaje recuperable, no una sesión o guardado simulado.
