# WoodMatch Prototipo Android

WoodMatch es un prototipo Android de economía circular para registrar, explorar y solicitar residuos leñosos urbanos en Suba, Bogotá.

## Incluye en modo demo

- Panel con indicadores calculados desde el inventario visible.
- Catálogo filtrable de residuos, publicación y solicitudes de retiro.
- Biblioteca botánica con asistencia opcional de PlantNet y Gemini.
- Mapa eco-vectorial de Suba que funciona sin claves ni conexión, con visores conectados opcionales.
- Datos de ejemplo para recorrer el flujo sin configurar Firebase.
- Reportes y fotos guardados en el almacenamiento privado del teléfono, sin Firebase Storage.

## Abrir en Android Studio

1. Abra esta carpeta raíz, no `Kotlin_App`.
2. Use JDK 21 y deje que Android Studio sincronice Gradle.
3. Ejecute la configuración `app` en un dispositivo Android o emulador.

El proyecto incluye Gradle Wrapper. Desde una terminal se puede crear un APK debug con:

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Datos y servicios

Los reportes creados por el usuario se guardan en este dispositivo. No se suben a Firebase ni se restauran tras desinstalar la aplicación. Las fotos se copian a los archivos privados de la app para que sigan disponibles después de cerrar la galería. Firebase Auth y Firestore continúan disponibles para los demás módulos.

El mapa ofrece una vista local y capas abiertas de OpenStreetMap y Bogotá. Google Maps no es necesario para navegar por la app.

El prototipo compila sin servicios externos. Para conectar Firebase en un entorno propio, coloque un `google-services.json` válido dentro de `app/`; Git lo ignora y el plugin se activa automáticamente. Las claves opcionales se pueden declarar en un archivo `.env` local a partir de `.env.example`:

```properties
PLANTNET_API_KEY=tu_clave
GEMINI_API_KEY=tu_clave
```

No incluya claves reales ni archivos de Firebase en el repositorio.
