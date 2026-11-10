# Desarrollo local conectado

Abre la raíz WoodMatch en Android Studio y selecciona el teléfono físico.

## Configuración persistente

- `app/google-services.json`: configuración cliente de Firebase para `com.example.kotlinapp.pilot`, ignorada por Git. Debe corresponder al mismo proyecto que valida el backend.
- En `~/.gradle/gradle.properties`, conserva las propiedades personales:

```properties
WOODMATCH_API_URL=https://REFERENCIA.supabase.co/functions/v1/woodmatch
WOODMATCH_REQUIRE_CONNECTED=true
```

La URL es pública. No copies credenciales administrativas ni claves de proveedores a Android. La cuenta de servicio Firebase, la clave privilegiada de Supabase, PASS_SECRET y las claves de Gemini/Pl@ntNet permanecen en el servidor.

La app lee `WOODMATCH_API_URL` como propiedad Gradle y, si no existe, como variable de entorno. La propiedad tiene prioridad. Con `WOODMATCH_REQUIRE_CONNECTED=true`, la configuración Gradle falla si falta el JSON cliente o la URL. Esto comprueba la configuración, no la disponibilidad del servidor.

## Ejecutar

Pulsa Run en Android Studio o ejecuta:

```bash
bash gradlew :app:installDebug
bash gradlew :app:testDebugUnitTest
```

Se mantienen los límites de memoria y trabajadores definidos en el proyecto. No hace falta un emulador para instalar en el teléfono. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Correspondencia con GitHub Actions

El workflow `pilot.yml` usa el secreto `WOODMATCH_GOOGLE_SERVICES_JSON` para generar el mismo archivo cliente y la variable `WOODMATCH_API_URL` para el endpoint. El paso se omite en pull requests. Los valores personales de este computador no se suben mediante Git ni se transfieren automáticamente a Actions.

Para comparar resultados usa el mismo commit, paquete, proyecto Firebase y URL de backend. Los cambios de Android requieren recompilar; las Edge Functions se despliegan por separado. No hace falta redesplegarlas al cambiar únicamente una pantalla.

El APK local y el artifact de Actions pueden usar certificados debug distintos. Conserva el certificado local; no desinstales la app para resolver un conflicto de firma sin respaldar primero los datos locales.

## Verificación funcional

Iniciar sesión con una cuenta de prueba existente y comprobar una lectura autorizada, operaciones de fotos y las APIs requeridas. No declarar todos los servicios operativos solo porque el APK abre. No compartir contraseñas, tokens de sesión ni JSON de cuentas de servicio en el chat o en Git.
