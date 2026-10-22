# Estado de despliegue de WoodMatch

## Veredicto

WoodMatch está listo para pruebas internas en un teléfono, pero **no para lanzar a usuarios reales ni para coordinar retiros/verificaciones entre personas**. El flujo de reporte funciona de forma local: la foto y el registro permanecen privados en el equipo donde se crearon.

## Flujo actual

1. La persona crea un reporte de árbol caído, dañado o en riesgo, con foto, descripción y ubicación.
2. La aplicación copia la foto al almacenamiento privado de la app y guarda el reporte localmente.
3. `Mis reportes` muestra el historial del teléfono, su estado pendiente y una vigencia de 30 días.
4. El mapa muestra esos mismos reportes locales junto a las zonas verdes de Suba.
5. El catálogo botánico y Pl@ntNet son herramientas opcionales; no bloquean el reporte.

No se afirma que el reporte fue enviado a una alcaldía, autoridad ambiental, guardabosque ni a otro usuario. Tampoco se muestran muestras de datos como si fueran actividad real.

## Correcciones incorporadas en esta iteración

- Las rutas que abrían el catálogo de residuos en vez del formulario e historial de reportes fueron corregidas.
- Se agregó el historial local visible, con foto, fecha, estado pendiente y vencimiento.
- La cámara abre mediante `FileProvider` y una URI privada; la foto capturada se puede leer de forma segura para Pl@ntNet.
- La compresión de fotos de Pl@ntNet ahora calcula el tamaño antes de decodificarlas, reduciendo el riesgo de cierre por falta de memoria con fotos de cámara grandes.
- El mapa ya no usa publicaciones ficticias: usa los reportes guardados en el dispositivo.
- Se quitó la aceptación de certificados TLS inválidos en el visor de mapas.

## Bloqueadores para uso por personas reales

| Prioridad | Falta | Riesgo si se publica así |
| --- | --- | --- |
| P0 | Sin servidor de reportes ni fotos | Los reportes no llegan a nadie, no se ven entre teléfonos y se pierden al desinstalar. |
| P0 | Autenticación con usuarios de demostración y rutas de respaldo | Un usuario podría obtener una identidad/rol que no corresponde; no es aceptable para administradores ni operadores. |
| P0 | Reglas de seguridad de Firestore y separación de roles verificadas | Riesgo de leer o modificar perfiles/publicaciones ajenas. |
| P0 | Claves de Pl@ntNet/Gemini en `BuildConfig` | Se pueden extraer de una APK. La clave de Pl@ntNet que estuvo en el historial público debe revocarse y rotarse. |
| P0 | Protocolo institucional de atención | Un reporte de riesgo no sustituye una línea de emergencias, ni existe hoy un responsable, SLA, evidencia de recepción o escalamiento. |
| P1 | Trazabilidad central | Faltan estados administrados: recibido, validado, asignado, resuelto, rechazado; además de comentarios y notificaciones. |
| P1 | Vencimientos centralizados | La vigencia actual se calcula en el teléfono. Para una comunidad debe usar reloj/servidor y trabajos programados. |
| P1 | Privacidad | Faltan consentimiento explícito para foto/GPS, borrado de datos, retención, exportación y una política de privacidad. |
| P1 | Observabilidad | Faltan Crashlytics, analítica con consentimiento, alertas y pruebas de dispositivos/red lenta. |
| P2 | Mapa operativo | Para volumen real, revisar política de teselas de OpenStreetMap y considerar un proveedor/servidor de teselas permitido; no depender de un WebView como único visor. |

## Ruta recomendada antes de piloto público

1. Definir quién recibe y verifica un reporte, los tipos de incidente y cuándo debe redirigirse a una línea oficial de emergencia.
2. Montar un backend mínimo: Firestore con reglas por rol y un almacenamiento de fotos aprobado, o un backend propio. Mantener una cola local con Room y sincronizar con WorkManager cuando vuelva la red.
3. Reemplazar los usuarios de demostración y los fallbacks de autenticación por Firebase Auth real, verificación de correo/teléfono y perfiles creados en servidor.
4. Mover Pl@ntNet/Gemini a un proxy/backend; nunca distribuir esas claves en una APK. Rotar cualquier clave que haya llegado al repositorio público.
5. Añadir una consola de verificación para operadores, notificaciones de cambios de estado y cierre documentado del caso.
6. Ejecutar prueba de campo: crear, cerrar y sincronizar reportes con GPS denegado, sin red, cámara, galería, Android 8+ y reinstalación.

## Lista mínima de prueba interna

- Crear un reporte con cámara y otro desde galería; cerrar y reabrir la app; ambos deben seguir apareciendo en `Mis reportes`.
- Abrir el mapa y comprobar que muestra únicamente los reportes creados en ese teléfono.
- Probar Pl@ntNet con una foto grande tomada por cámara; debe mostrar resultado o un error controlado, nunca cerrar la app.
- Denegar GPS y cámara; el formulario debe explicar el estado y seguir permitiendo guardar sin foto si se requiere.
- Desinstalar una copia de prueba: confirmar que los reportes locales desaparecen, tal como advierte la interfaz.
