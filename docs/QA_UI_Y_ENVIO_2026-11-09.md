# Revisión de interfaz, validación y envíos repetidos

Fecha local: 2026-11-09 (America/Bogota). Base: commit `0f0699074fc5559eab000a220bb86a5d67c91c8d`.
APK de desarrollo: `2.3.0-pilot`, versionCode 8. Backend desplegado: `woodmatch` v13, sin cambios durante esta revisión.

## Alcance y entorno

Se combinó revisión de las rutas activas de MainActivity, navegación manual mediante ADB, capturas/jerarquías de Android Studio, JUnit y las pruebas de dominio existentes en Node. No se añadió una nueva infraestructura de pruebas.

Dispositivo físico Z2450, Android 14, 720 × 1600 px, densidad 320. Se comprobó tamaño de letra del sistema 100 % y 150 %, tema oscuro/Sistema y una revisión temporal del tema claro en Perfil. Se restauraron font_scale=1.0 y apariencia Sistema; la paleta automática no se cambió.

Las capturas se observaron en la sesión del IDE. No se añadieron capturas con datos personales al repositorio.

## Hallazgos y correcciones

| Hallazgo | Corrección | Verificación |
|---|---|---|
| La etiqueta de Buscar quedaba más abajo por un icono de tamaño diferente | Mismo espacio para todos los iconos y dos líneas escalables para las etiquetas | Jerarquía final: las cinco etiquetas comparten la misma coordenada vertical, tanto al 100 % como al 150 % |
| Actividad se truncaba con letra grande | Etiqueta de hasta dos líneas con altura calculada a partir de cada línea escalada | Texto completo visible al 150 % |
| El contador circular de aportes recortaba texto al 150 % | El diámetro acompaña la escala tipográfica | Palabra aportes completa en el dispositivo |
| Publicaciones terminaba con una letra aislada en Mi actividad | Etiqueta breve Aportes y espacio uniforme para los tres contadores | Contadores finales alineados y legibles |
| El formulario permitía intentar enviar entradas vacías o fechas imposibles | Validación compartida en UI y repositorio, antes de la petición | Botón vacío deshabilitado; 2027-02-30 marcado como error; casos límite en JUnit |
| Varios botones dependían únicamente del estado enabled de la última composición | Guarda síncrona al principio del callback y bloqueo visual mientras la operación está activa | Revisión de publicación, solicitud, reporte, soporte, transiciones, reprogramación, pases y controles de perfil |
| Las reprogramaciones generaban un ID distinto en cada reintento | ID recordado para la misma solicitud, fecha, motivo y acción | Revisión del payload del cliente y pruebas existentes de idempotencia/transacciones |

La guarda de cliente limita invocaciones antes de la siguiente recomposición. No sustituye autorización, comprobación de estado o idempotencia del servidor.

## Validaciones revisadas

- Correo: regex existente y límite de 254 caracteres antes de evaluarla; prueba con una cadena de 10 000 caracteres y salto de línea final.
- Teléfono: regex de +57 y diez dígitos; pruebas existentes de longitudes inválidas.
- Contraseña de registro: al menos ocho caracteres, mayúscula, minúscula y número. Login, Registro y el ViewModel de cambio de contraseña ya tenían control de operación en curso.
- Fechas de solicitud/reprogramación: formato ASCII AAAA-MM-DD, calendario estricto y vigencia hasta el final del día en America/Bogota. Se prueba año bisiesto, fecha imposible, fecha pasada, año cero y formato incorrecto.
- Solicitud: uso obligatorio hasta 500 caracteres, cantidad obligatoria hasta 100 y mensaje hasta 1000; el repositorio repite la validación aunque el caller no sea el formulario actual.
- Código de pase: entrada limitada a ocho dígitos ASCII.
- Servidor: identificadores acotados, comprobación de rol/propietario, estado, revisión, cantidades finitas, consentimiento, cuotas específicas y operaciones transaccionales existentes.

## Resultados

| Prueba | Resultado y alcance |
|---|---|
| npm --prefix backend-tests test | 43 aprobadas, 0 fallidas; dominio, soporte, administración y dashboard simulados |
| 50 publicaciones concurrentes con el mismo identificador de operación | Una revisión y un evento |
| 50 reintentos de una solicitud y otros 50 intentos con identificadores distintos para el mismo material/usuario | Un resultado compartido para los reintentos; los duplicados distintos se rechazan |
| 50 aceptaciones en competencia entre dos solicitudes | Una única reserva |
| Fechas/identificadores/campos inválidos del servidor | Rechazo sin crear solicitudes en la base simulada |
| :app:installDebug :app:testDebugUnitTest | Compilación final, instalación y pruebas Android aprobadas; 1 min 20 s |
| Formulario de solicitud vacío y fecha imposible | Verificado en teléfono, sin enviar una solicitud real |
| Ayuda con asunto/descripción vacíos | Enviar caso deshabilitado |
| Inicio, mapa, catálogo, detalle, Perfil, Ayuda y Mi actividad | Inspección visual en el recorrido descrito; correcciones confirmadas |
| Reinicio de la app y cambios de escala | Sesión recuperada y navegación operativa |
| Logs recientes de la última app ejecutada | Sin marcadores FATAL EXCEPTION, ANR u OutOfMemory en las entradas devueltas |

Los tests de ráfagas ejecutan el dominio con una base en memoria que serializa transacciones. Verifican invariantes e idempotencia del código; no miden throughput, latencia, contención real de Firestore ni cuotas de infraestructura.

No se ejecutaron borrados de cuenta, cambios de contraseña, publicaciones de prueba, reservas ni envíos masivos sobre el backend real. La guarda del botón de borrado se verificó por revisión de código.

## Artefacto final y límites

APK: `app/build/outputs/apk/debug/app-debug.apk`.

SHA-256: `7bf7037e9253e9fffee1aa5f42a009b84b4192067b05a94c8d873e723f81702c`.

Esta es una revisión funcional y de controles de envío; no es una certificación de pentesting o de resistencia a denegación de servicio. No cubre todas las pantallas, todos los roles, tablets, TalkBack, todas las paletas ni el ciclo completo entre dos teléfonos.

Quedan para un entorno de pruebas dedicado: carga HTTP medida, reintentos con pérdida de respuesta, concurrencia contra Firestore real, recreación durante un envío real, recorrido generador/receptor, App Check/atestación y revisión del panel web. La sincronización de Actions y el backend mantiene los pendientes del historial anterior.

Para repetir las comprobaciones locales:

```bash
npm --prefix backend-tests test
bash gradlew :app:installDebug :app:testDebugUnitTest
```

Referencia de accesibilidad: [escalado tipográfico no lineal de Android 14](https://developer.android.com/about/versions/14/features#non-linear-font-scaling).
