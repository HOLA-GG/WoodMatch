# Seguridad, datos y permisos

## Controles

- Firebase Authentication valida identidad; la Edge Function vuelve a verificar el JWT y perfil.
- Las escrituras de negocio se hacen por la función; ocultar botones no es un control de acceso.
- Fotos privadas en Supabase Storage y URL firmada limitada en tiempo.
- Una ubicación exacta solo se comparte tras consentimiento explícito.
- El rol del registro es inmutable desde el perfil normal.
- Los logs no incluyen claves, URL con claves, foto base64, prompt completo ni cuerpos de proveedor.

## Permisos Android

| Permiso | Uso |
|---|---|
| Cámara | Capturar evidencia. |
| Galería / selector visual | Adjuntar foto existente. |
| Ubicación precisa o aproximada | Proponer punto de reporte; puede rechazarse. |
| Internet | Firebase, Supabase, OSM, Pl@ntNet y asistencia. |

## Privacidad

El usuario puede actualizar datos permitidos, exportar su información y pedir eliminación. La eliminación bloquea primero operaciones, después borra perfil, fotos y publicaciones. Un fallo parcial requiere procedimiento privado de recuperación; no se reactiva un perfil marcado para borrar.
