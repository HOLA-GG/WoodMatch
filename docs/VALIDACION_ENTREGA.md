# Validación de entrega de prueba · WoodMatch 1.1.0

22 de octubre de 2026. Cambios locales sobre b59ea53eff9eae7b28ed0b7a5a2b1edf10d341b9; no se ha realizado commit, push, despliegue Firebase ni publicación.

## Resultados comprobados

| Comprobación | Resultado |
|---|---|
| assembleDebug | Aprobado; APK instalable firmado con clave de depuración. |
| testDebugUnitTest | 13 pruebas: 8 de validación/autenticación y 5 de economía circular; 0 fallos, errores u omitidas. Incluye rechazo de cuentas inventadas cuando Firebase no está disponible. |
| lintDebug | Aprobado sin errores; 54 advertencias que siguen pendientes de revisión. |
| assembleRelease / lintVitalRelease | Aprobados; APK sin firma productiva, con R8 y reducción de recursos. |
| Ofuscación | Mapa R8 generado; se comprobaron nombres de clases de UI transformados. Los DTO Firestore conservan sus propiedades para no romper la persistencia. |
| Pruebas del servidor con Node 22.23.4 | 12 aprobadas: 9 de rechazo de invocaciones sin autenticación y 3 de autorización/transiciones/vigencia de pases. No sustituyen pruebas de integración. |
| Carga de las funciones | Nueve endpoints cargan correctamente con SDK actuales y proyecto ficticio, sin contactar servicios externos. |
| npm audit --omit=dev | 0 vulnerabilidades conocidas reportadas para dependencias de producción en esta ejecución. No constituye auditoría de seguridad completa. |
| Manifiesto empaquetado | Sin permisos de cámara propia, ubicación, micrófono, contactos o almacenamiento general. Permisos de red y permisos auxiliares de SDK de Google documentados por el manifiesto fusionado. |
| DEX de los APK | 0 coincidencias del patrón de clave Google inspeccionado. Los campos de claves privadas de IA están vacíos; esta comprobación no reemplaza una auditoría de secretos ni implica que una configuración Firebase cliente futura sea un secreto privado. |
| Integridad del árbol | git diff --check sin errores. |

## Artefactos

- `/workspace/entregables/WoodMatch-1.1.0-debug.apk`: 29.420.922 bytes; para pruebas, generado sin google-services.json.
- `/workspace/entregables/WoodMatch-1.1.0-release-sin-firma.apk`: 3.839.160 bytes; requiere firma y configuración antes de distribución.
- `/workspace/entregables/WoodMatch-1.1.0-SHA256.txt`: sumas de comprobación de ambos.
- `app/build/outputs/mapping/release/mapping.txt`: conservar en privado para interpretar errores de la release.

## Bloqueos y límites

- La descarga del emulador Firestore fue rechazada con HTTP 403 “Domain forbidden” desde storage.googleapis.com. Las pruebas de reglas no se ejecutaron; tampoco se verificaron reglas de Storage ni operaciones concurrentes contra un backend real.
- Sin dispositivo/emulador Android: no se reprodujo ni se verificó el cierre reportado en el APK anterior, ni se comprobaron visualmente cámara, QR, accesibilidad o navegación. Se añadió un flujo explícito de cámara/galería y se acotó la decodificación de imágenes para abordar fallos potenciales; no se afirma una causa confirmada del incidente anterior.
- Firebase, App Check, Storage, Functions y APIs remotas requieren el proyecto y las credenciales que configurará el usuario. No se ha verificado emisión/consumo de QR entre dos teléfonos ni disponibilidad real de los endpoints de proveedores. El backend y las reglas están preparados en código, sin despliegue remoto.
- La política de privacidad es de prueba, con el contacto ficticio solicitado. No hay responsable real, canal atendido ni política productiva de retención aprobada. No usar datos personales reales ni declarar cumplimiento jurídico.
- El APK actual no obtiene una conexión Firebase por añadir .env posteriormente: añadir google-services.json y recompilar.
- Pendientes funcionales explícitos: edición completa, cancelación/reprogramación, migraciones de datos antiguos, notificaciones reales y validación de todos los controles en dispositivo. No se declara que todos los requisitos del SRS estén cerrados.

Siguiente paso: seguir `OPERACION_Y_SEGURIDAD.md` para configurar un proyecto de pruebas y ejecutar la aceptación de extremo a extremo antes de una publicación pública.
