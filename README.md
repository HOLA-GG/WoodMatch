# WoodMatch · Piloto Android

Comunidad de recuperación de madera en Suba, Bogotá: hallazgos, podas, estibas, tablas y muebles; publicaciones con foto y ubicación confirmada; retiros con QR/código y confirmación de ambas personas.

Abre **esta carpeta raíz** en Android Studio. `Kotlin_App/` conserva el prototipo histórico.

- JDK 21, SDK Android 36 y Gradle Wrapper incluido.
- `bash gradlew --max-workers=2 assembleDebug testDebugUnitTest lintDebug`
- `bash gradlew --max-workers=2 assembleRelease bundleRelease lintRelease`
- APK de pruebas: `app/build/outputs/apk/debug/app-debug.apk`.
- Identidad independiente: `com.example.kotlinapp.pilot`; puede coexistir con la app anterior.
- Release y AAB requieren una firma custodiada antes de distribuirse como producción.

El piloto utiliza Firebase **Spark** para cuentas y datos, Supabase **Free** para fotos privadas y funciones, OSM para el mapa, y las cuotas gratuitas de Gemini/Pl@ntNet. **No se activa facturación.** Las funciones de `functions/` son históricas y ya no forman parte del despliegue activo.

La configuración y los secretos del entorno anterior no vienen con el código. Un APK sin configuración permite explorar especies y muestra claramente las funciones comunitarias pendientes de conexión.

Consulta [Configuración, pruebas y operación del piloto](docs/PILOTO.md), [Validación de esta entrega](docs/VALIDACION_PILOTO.md) y [notas de la prerelease](docs/PILOT_RELEASE_NOTES.md).
