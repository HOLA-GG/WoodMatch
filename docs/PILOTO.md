# Operación del piloto gratuito

Esta guía corresponde a la app raíz 2.0. Las guías antiguas describen el prototipo y el backend anterior; no las uses para desplegar esta versión. El objetivo es una prueba académica pequeña con personas conocidas, sin tarjeta ni facturación.

## Qué se instala y qué se aloja

```mermaid
flowchart LR
    A[Android WoodMatch Piloto] --> B[Firebase Authentication Spark]
    A --> C[Firestore: lecturas con reglas]
    A --> D[Supabase Edge Function: verifica token Firebase]
    D --> C
    D --> E[Supabase Storage privado]
    D --> F[Gemini gratuito]
    D --> G[Pl@ntNet gratuito]
    A --> H[OpenStreetMap: mapa y caché local]
```

El APK contiene la interfaz y configuración pública. Las cuentas, publicaciones y comprobantes permanecen en servicios remotos. Google Play distribuye la app; no despliega esos servicios ni paga sus cuotas.

La app verifica operaciones en el servidor: no basta con ocultar un botón. Las escrituras de publicaciones y solicitudes desde clientes están denegadas. Las lecturas comunitarias requieren cuenta y solo incluyen estados activos. Tras completar un retiro, únicamente las dos personas pueden leer la publicación y su comprobante.

### Límites deliberados del piloto

- Una foto por publicación, recodificada en Android, máximo 1 MiB de JPEG. La selección original admite hasta 15 MiB.
- Hasta 100 cargas por usuario y 700 MiB reservados globalmente; los cambios de foto consumen nuevas cargas hasta su limpieza de mantenimiento.
- Gemini: cinco consultas por persona y veinte globales al día UTC. Pl@ntNet: diez por persona y cien globales al día UTC. Los intentos fallidos también consumen cuota para evitar abuso.
- La vista comunitaria sincroniza hasta cien publicaciones activas; cada consulta privada admite hasta cien publicaciones o solicitudes. No es un catálogo escalado ni paginado para miles de movimientos.
- Los pases vencen en diez minutos; emitir otro invalida el anterior. Cinco intentos erróneos bloquean la validación durante diez minutos.
- OSM es un servicio de mejor esfuerzo, sin garantía de disponibilidad. No se permite precargar zonas ni descargar teselas masivamente.
- Supabase Free puede pausarse tras una semana de inactividad; el proyecto debe reanudarse desde su panel antes de la demostración. No se generan llamadas artificiales para evitar esa pausa.
- El almacenamiento y las cuotas gratuitos son finitos. Si se agotan, el piloto informa del problema; no cambia automáticamente a planes de pago.

Fuentes: [Firebase](https://firebase.google.com/pricing), [Supabase](https://supabase.com/pricing), [Gemini](https://ai.google.dev/gemini-api/docs/billing), [Pl@ntNet](https://my.plantnet.org/terms_of_use), [política OSM](https://operations.osmfoundation.org/policies/tiles/). Revisa las condiciones vigentes antes de ampliar la distribución.

## Configurar cuentas sin tarjeta

1. Conserva Firebase en **Spark**. Habilita Authentication con correo/contraseña y Firestore. No habilites Cloud Functions ni Firebase Storage para esta versión.
2. Registra una aplicación Android adicional con paquete `com.example.kotlinapp.pilot` en el mismo proyecto. Descarga su `google-services.json` a `app/google-services.json`. Así las cuentas existentes del mismo proyecto pueden conservarse y el APK anterior permanece instalado.
3. Crea un proyecto **Supabase Free**. Anota su referencia real y URL. No selecciones Pro ni complementos de pago. Instala Supabase CLI e inicia sesión en tu computador.
4. Crea una cuenta de servicio dedicada en Google Cloud para el mismo proyecto Firebase. Necesita permisos de datos de Firestore y administración de Firebase Authentication: `roles/datastore.user` y `roles/firebaseauth.admin`. Guarda su clave privada solo en tu computador y en los secretos del servidor; nunca en Git ni en Android.
5. Copia `supabase/functions/.env.example` a un archivo privado fuera del repositorio. Completa el identificador Firebase, el JSON de esa cuenta de servicio en una sola línea, `PASS_SECRET` aleatorio de al menos 32 caracteres y las claves de Gemini y Pl@ntNet. Mantén Gemini en su nivel gratuito; revisa en AI Studio que el modelo configurado esté disponible en tu cuenta y región. El valor inicial es `gemini-2.5-flash` y no hay fallback a modelos de pago.
6. Configura esos secretos con `supabase secrets set --env-file /ruta/privada/piloto.env --project-ref REFERENCIA_REAL`. `SUPABASE_URL` y `SUPABASE_SERVICE_ROLE_KEY` los proporciona el entorno de funciones; no se ponen en el APK.
7. Revisa el script de despliegue. Sin argumentos solo muestra instrucciones. Cuando apruebes el proyecto de destino, ejecuta `bash scripts/deploy-pilot.sh --apply PROYECTO_FIREBASE REFERENCIA_SUPABASE`. Publica exclusivamente reglas/índices, el bucket privado y la función. No cambia facturación.
8. Compila con `WOODMATCH_API_URL=https://REFERENCIA_REAL.supabase.co/functions/v1/woodmatch bash gradlew --max-workers=2 assembleDebug`. Conserva el certificado de depuración de tu computador para poder actualizar sus APK de prueba sin desinstalarlos.
9. Comprueba registro de dos cuentas, publicación con foto, edición en el segundo teléfono, IA, identificación y retiro completo. Comprueba también los errores de cuota y conexión. No distribuyas como piloto operativo un APK con `firebaseConfigured=false` o `apiConfigured=false`.

El archivo `google-services.json` es configuración cliente; no sustituye las claves del servidor, reglas ni índices. Añadir `.env` después de instalar un APK no cambia su configuración: hay que recompilarlo. Las claves de Gemini, Pl@ntNet, la cuenta de servicio y la clave administrativa de Supabase nunca deben aparecer en BuildConfig, logs o assets.

App Check/Play Integrity debe evaluarse al preparar la distribución de Play. El piloto instalado directamente funciona con tokens Firebase verificados, comprobación de cuenta y cuotas; no exige que una instalación externa haya sido descargada desde Play Store. Los perfiles de depuración anteriores no se utilizan para simular autenticación.

## Flujo y permisos

- Cuentas generadoras o mixtas ofrecen material; receptoras o mixtas lo solicitan. Cualquier cuenta real puede registrar un hallazgo. Un administrador no obtiene permisos de negocio especiales mediante un texto de rol enviado por el cliente.
- Un hallazgo informa, sin habilitar reservas. Solo su autor puede convertirlo en oferta, con rol adecuado y confirmación de autorización. La condición física permanece separada del estado de disponibilidad.
- La ubicación inicial no es una ubicación confirmada. GPS puede fallar; tocar el mapa o aplicar coordenadas permite una alternativa accesible. Antes de publicar se confirma la exposición exacta del punto a la comunidad con cuenta.
- El oferente acepta una solicitud; la transacción impide que dos aceptaciones reserven el mismo material. Mientras esté reservado no se puede editar la publicación.
- Quien recoge muestra el QR o código. Quien entrega lo valida. Ambas personas confirman por separado después de la entrega real; entonces se emite el comprobante y se retira la publicación del catálogo.
- Una cancelación libera la reserva. Las otras solicitudes no pasan a aceptadas automáticamente.
- El comprobante es un registro de entrega, no acredita identidad física ni constituye factura fiscal.

### Borradores y datos anteriores

Los borradores se guardan en el almacenamiento privado de la app para la cuenta actual; volver a Crear los recupera. La foto y datos temporales de cámara sobreviven a la recreación de la actividad. La exportación o cierre remoto requieren conexión.

El nuevo paquete no puede acceder a los archivos privados del APK anterior. Esa separación conserva sus datos intactos. Los reportes presentes en el almacenamiento de esta instalación aparecen en Mi actividad con «Revisar e importar»; siempre se vuelven a confirmar foto y ubicación antes de compartirlos. No se convierten automáticamente en ofertas ni se publican datos antiguos. También se pueden seleccionar archivos JSON con una lista de reportes o un objeto con campo `reports`, hasta cien registros y 1 MiB. Los enlaces a fotos del archivo no se importan: hay que adjuntarlas de nuevo.

Las publicaciones compartidas `schemaVersion=2` se siguen leyendo; al editarlas se exige una foto y ubicación revisadas y se actualizan al esquema 3. Las fotos antiguas que ya eran públicas no se convierten retroactivamente en privadas.

## Probar en un celular desde Linux

1. Instala Android Studio y abre la raíz WoodMatch. Selecciona JDK 21 y SDK 36.
2. En Android, activa Opciones de desarrollador pulsando siete veces el número de compilación; activa Depuración USB. Conecta un cable de datos y acepta la huella del computador.
3. En distribuciones basadas en Ubuntu, instala las reglas USB si hacen falta: `sudo apt install android-sdk-platform-tools-common`. Si el sistema usa el grupo `plugdev`, agrega tu usuario y vuelve a iniciar sesión. No ejecutes ADB como root por defecto.
4. Comprueba `adb devices -l`. Debe aparecer `device`, no `unauthorized`. Selecciona el teléfono en Android Studio y pulsa Run. Los cambios de lógica suelen requerir volver a ejecutar; algunos cambios de interfaz admiten Live Edit en depuración.
5. Para obtener el cierre de cámara: `adb logcat -c`, reproduce el error y consulta `adb logcat -b crash -d`. Revisa y retira datos personales antes de compartir el registro.
6. Con Android 11 o superior puedes emparejar desde Device Manager → Pair Devices Using Wi-Fi. Ambos equipos deben estar en la misma red. Revoca la autorización al terminar en equipos compartidos.

Este entorno de desarrollo en la nube no tiene acceso al USB de tu computador. No abras puertos de depuración de tu teléfono a Internet.

### Matriz de validación física pendiente

Probar como mínimo: cámara/galería/cancelación; rotación mientras se usa la cámara; recreación de actividad; imagen grande y orientada; GPS preciso y aproximado; permiso rechazado; GPS apagado; mapa con Wi-Fi, datos y sin conexión; texto al 200 %; TalkBack; escala de animación cero; claro/oscuro y cuatro estaciones; publicación/editado en dos dispositivos; QR y código; cancelación y comprobante offline. Registrar modelo, Android, versión de APK, resultado y logs saneados. No marcar una prueba como aprobada por compilar.

## Versiones, monitoreo y mantenimiento

GitHub Actions ejecuta pruebas de dominio, autenticación del servidor, integración de reglas con emulador, compilaciones y lint. Solo si pasan publica una prerelease para la rama del piloto con APK, assets, pruebas, checksum y `build-info.json`. En PR no se leen los secretos de compilación.

Para una compilación conectada, configura en GitHub el secreto `WOODMATCH_GOOGLE_SERVICES_JSON` y la variable `WOODMATCH_API_URL`. No configures allí las claves privadas de los proveedores para incorporarlas al APK. Un APK sin esos valores se etiqueta como pendiente de conexión.

Crashlytics está desactivado hasta que la persona acepta compartir diagnósticos en Perfil. La app registra errores remotos mediante operación y estado, sin adjuntar cuerpos ni excepciones que contengan datos. Los logs de Edge contienen únicamente versión, operación reconocida, correlación, resultado y duración. El historial de negocio se guarda en documentos privados de eventos; no se reemplaza por los logs efímeros del plan gratuito.

Revisa cuotas en Firebase, Supabase, AI Studio y Pl@ntNet. Conserva copias privadas de los datos de prueba necesarios para el grado; el plan gratuito no equivale a una política de copias de seguridad. Limpia cargas fallidas o fotos reemplazadas antes de alcanzar el límite. Las reservas de espacio son conservadoras: no se liberan automáticamente tras una carga fallida para evitar subestimar consumo.

La eliminación de cuenta bloquea primero las operaciones y luego elimina perfil, fotos y publicaciones. Si ocurre un fallo parcial, la cuenta queda bloqueada; el responsable debe completar la limpieza con credenciales administrativas. No se debe retirar el tombstone para intentar recuperar acceso a datos pendientes de eliminación. `scripts/recover-account.ts` permite al responsable revisar y reanudar una eliminación: cargar el archivo privado de entorno, añadir `SUPABASE_URL` y `SUPABASE_SERVICE_ROLE_KEY` solo a ese entorno y ejecutar `deno run --allow-env --allow-net --env-file=/ruta/privada/piloto.env scripts/recover-account.ts UID`. Sin `--apply` solo muestra el número de pendientes; con `--apply` completa la limpieza de una cuenta previamente bloqueada. El historial de la otra persona conserva hechos no identificativos del movimiento.

Antes de compartir ampliamente, sustituye cualquier dato de contacto antiguo o ficticio y facilita un canal real a los participantes. Los términos del piloto identifican servicios, exposición de ubicación, asistencia remota y diagnósticos opcionales.

## Ruta futura a Play Store

El APK de esta prerelease usa firma de depuración. Para Play: elegir identificador definitivo, crear y custodiar la clave de subida, generar AAB firmado, configurar Play App Signing y registrar certificados correctos en Firebase. La clave de subida y el certificado con el que Play firma las instalaciones cumplen funciones distintas.

Después: completar política de privacidad y formularios de datos, requisitos de la cuenta de desarrollador vigentes y pruebas internas/cerradas. Verificar servicios con la app firmada de distribución antes de abrir el acceso. Publicar en Play y activar servicios de pago quedan fuera de esta entrega.

No prometemos ausencia absoluta de errores: el criterio de salida es evidencia de compilación, pruebas automatizadas, pruebas físicas completadas y servicios reales comprobados dentro de sus cuotas.
