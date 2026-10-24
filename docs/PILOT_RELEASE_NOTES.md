WoodMatch incorpora publicaciones comunitarias de hallazgos y material recuperable, un formulario con foto y ubicación, mapa nativo de OpenStreetMap, edición de publicaciones y coordinación de retiros con QR/código y confirmación de ambas personas.

Los comprobantes quedan privados y el material recolectado sale del catálogo. La interfaz conserva los tonos cálidos de Stitch, con temas estacionales automáticos o manuales y apariencia clara/oscura.

Esta es una versión académica de pruebas, con identidad `com.example.kotlinapp.pilot` y firma de depuración. Puede instalarse junto al APK anterior. No es una publicación de Play Store.

Revisa `build-info.json`: si `firebaseConfigured` o `apiConfigured` es falso, faltan las conexiones del piloto. En ese caso no funcionan cuentas nuevas, publicaciones compartidas ni consultas remotas; la guía botánica local sigue disponible. Las claves privadas nunca se incluyen en el APK.

Las pruebas físicas de cámara, ubicación, mapas y recolección entre dos teléfonos deben completarse antes de compartir el piloto como versión operativa. Las referencias visuales de Stitch incluidas en los assets no son capturas de esta compilación.

Guía de configuración gratuita, pruebas en Linux y limitaciones: `docs/PILOTO.md` en el código de esta versión.
