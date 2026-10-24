# Validación de WoodMatch 2.0 piloto

Esta entrega se prepara sobre `29afd17e057aa6d7f32ef7a3d0c5b5e20f19e019`. Las fechas de publicación y el commit final se registran en GitHub y en `build-info.json`.

## Evidencia obtenida

- 16 pruebas de dominio aprobadas: reservas concurrentes, roles, edición y revisión, conversión de hallazgos, idempotencia, pases y doble confirmación; presupuesto de fotos concurrente.
- 5 pruebas Edge con fixtures aprobadas: tokens inválidos/vencidos/de otro proyecto/anónimos, cuentas deshabilitadas o revocadas, cuotas, validación de entradas y exclusión de datos privados de logs. No consumen servicios reales.
- Comprobación de tipos Deno del servidor.
- Android: APK debug compilado y 20 pruebas unitarias aprobadas; lint debug sin errores. APK release ofuscado y AAB generados, lint release sin errores; los informes se adjuntan con los assets. Los binarios release todavía requieren firma de distribución.
- Pruebas de reglas y transacciones REST listas para ejecutarse con Firestore Emulator y obligatorias en GitHub Actions. La ejecución local fue bloqueada al descargar `cloud-firestore-emulator-v1.19.8.jar`: respuesta 403 «Domain forbidden» de la política de red del entorno. No se eludió ese bloqueo.

## Comprobaciones que requieren configuración o dispositivo

No se han desplegado Firebase/Supabase ni activado facturación. Este entorno no contiene la configuración cliente de tu proyecto ni los secretos remotos. No se comprobaron respuestas reales de Gemini/Pl@ntNet, carga de fotos, OSM en redes móviles ni retiros entre celulares reales.

No hay dispositivo Android conectado ni emulador Android instalado en este entorno. La causa del cierre informado se abordó corrigiendo el ciclo de captura, restauración y manejo de errores; su reproducción exacta y confirmación mediante Logcat requieren el teléfono donde ocurre.

La matriz de pruebas físicas está en `docs/PILOTO.md`. Los assets de Stitch son referencias visuales; no constituyen evidencia de ejecución de la nueva interfaz.
