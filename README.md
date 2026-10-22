# WoodMatch · Android

Aplicación de prueba para publicar y encontrar residuos leñosos urbanos en Suba, Bogotá, y coordinar retiros entre generadores y receptores.

Abre **esta carpeta raíz**, no `Kotlin_App` (prototipo histórico). Usa JDK 21, Android SDK 36 y el Gradle Wrapper incluido.

## Ejecutar y comprobar

```bash
./gradlew --max-workers=2 assembleDebug testDebugUnitTest lintDebug
./gradlew --max-workers=2 assembleRelease
```

- APK de prueba: `app/build/outputs/apk/debug/app-debug.apk`.
- Release ofuscada con R8 y recursos reducidos: `app/build/outputs/apk/release/` (sin firma productiva).
- Mapa R8: `app/build/outputs/mapping/release/mapping.txt`. Custodiarlo para interpretar errores; no distribuirlo públicamente.
- Invitado: permite consultar especies; no simula una cuenta autenticada ni operaciones de retiro.
- Publicaciones, fotografías compartidas y retiros: requieren Firebase, conexión, cuenta y backend desplegado. Una falta de configuración muestra un error; no se presenta un guardado ficticio.

## Conectar Firebase y servicios

Coloca la configuración Android del proyecto de pruebas en `app/google-services.json` (ignorada por Git). Configura Authentication, Firestore, Storage y App Check. Las funciones de `functions/` usan Node 22. Los secretos de PlantNet y Gemini se configuran en Secret Manager; no se incorporan desde `.env` al APK.

Consulta [Operación, seguridad y publicación](docs/OPERACION_Y_SEGURIDAD.md) para configuración, pruebas, QR, firma, privacidad y bloqueos pendientes. El script `scripts/deploy-firebase.sh` exige un proyecto explícito y configuración previa; no se ha ejecutado contra un proyecto remoto en esta entrega.

## Privacidad y límites de la versión

El aviso dentro de la app es de prueba. `Cookietech@woodmatch.org` es un contacto ficticio pendiente de sustitución. Utiliza datos ficticios hasta definir el responsable real, la política de tratamiento/retención y los canales de derechos. La app no acredita identidad física mediante QR ni certifica especies, calidad de madera o cumplimiento jurídico.

El historial nuevo corresponde a publicaciones y solicitudes en el backend. Los reportes locales del prototipo anterior no se migran automáticamente. Las publicaciones antiguas sin `schemaVersion=2` quedan fuera del catálogo nuevo para evitar exponer datos que no hayan sido revisados.
