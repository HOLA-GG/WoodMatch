# Evidencias visuales de WoodMatch 2.3

**Materia:** Ingeniería de Software II · **Marco de trabajo:** ITIL 4  
**Fecha de captura:** 30 de octubre de 2026  
**Responsable de desarrollo:** Juan Sebastian Fune Sanchez  
**Gestión de pruebas y documentación:** Yeison David Diaz Navarrete

> **Alcance de las evidencias.** Las capturas muestran interfaces existentes en un APK del piloto. La presencia de una pantalla no certifica que todos los flujos hayan sido probados de extremo a extremo, ni valida SLA o disponibilidad en producción.

## Registro de evidencias

| Código | Servicio | Pantalla identificada | Estado documental |
| --- | --- | --- | --- |
| EV-01 | ST-01 | Acceso y exploración de invitado | Captura seleccionada |
| EV-02 | ST-03 | Inicio de la comunidad autenticada | Captura seleccionada |
| EV-03 | ST-03 | Consulta del catálogo de materiales | Captura seleccionada |
| EV-04 | ST-04 | Mapa de Suba con OpenStreetMap | **Imagen incorporada abajo** |
| EV-05 | ST-02 | Crear reporte, evidencia fotográfica | Captura seleccionada |
| EV-06 | ST-05, ST-07 | Mi actividad y entregas | Captura seleccionada |
| EV-07 | ST-09 | Mi álbum botánico | Captura seleccionada |
| EV-08 | ST-12 | Formulario de ayuda y participación | Captura seleccionada |
| EV-09 | ST-12 | Centro de soporte Cookiestech | Captura seleccionada |
| EV-10 | ST-13 | Moderación de contenido | Captura seleccionada |
| EV-11 | ST-14 | Privacidad y exportación de datos | Captura seleccionada |
| EV-12 | ST-03 | Conexiones en Suba / directorio | Captura seleccionada |

**Estado de carga:** esta página contiene por ahora la imagen EV-04. Las otras once capturas están clasificadas en el paquete local de evidencias, **no están alojadas en el repositorio** y no se enlazan como imágenes para evitar referencias rotas.

## EV-04 · Servicio de mapa con OpenStreetMap (ST-04)

![Mapa interactivo de Suba con marcadores de WoodMatch y atribución a OpenStreetMap](evidencias/mapa/mapa-osm-suba.jpeg)

**Descripción:** pantalla de mapa con capas de OpenStreetMap, controles de acercamiento y alejamiento, marcadores de publicaciones comunitarias y atribución al proveedor cartográfico.

**Prueba pendiente:** verificar actualización de marcadores, filtros, manejo de fallos de red, permisos de ubicación y comportamiento de la caché sobre uno o más dispositivos.

## Hallazgos de control de calidad

- Una captura de solicitudes e historial muestra diferencias entre cantidades y fechas del registro. Se excluyó para evitar mostrarla como evidencia validada y se recomienda investigar los datos del caso antes de utilizarla académicamente.
- Una captura de perfil presenta datos personales identificables. No se incluyó en la selección publicable.
- No se encontró captura independiente de la bandeja de notificaciones para ST-11.
- Las capturas del álbum botánico y del centro de soporte muestran la interfaz, pero no bastan por sí solas para verificar una identificación exitosa o un ciclo completo de incidencia.

## Criterio de publicación

Antes de publicar más capturas en este repositorio público, revisar datos privados y anomalías de prueba; mantener la correspondencia entre servicio, captura y descripción. Las imágenes deben acompañar a las fichas ITIL sin sustituir métricas, registros de pruebas ni evidencias de validación funcional.

[Volver al catálogo de servicios TI](Catalogo-de-servicios-TI) · [Índice de documentación](Home)
