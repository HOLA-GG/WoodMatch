# Arquitectura y servicios

```mermaid
flowchart LR
  A[Android WoodMatch] --> B[Firebase Authentication]
  A --> C[Firestore: lecturas protegidas]
  A --> D[Supabase Edge Function woodmatch]
  D --> C
  D --> E[Supabase Storage privado]
  D --> F[Asistente botánico]
  D --> G[Pl@ntNet]
  A --> H[OpenStreetMap]
```

| Componente | Responsabilidad |
|---|---|
| Android `com.example.kotlinapp.pilot` | Interfaz, cámara/galería, GPS, mapa OSM, borradores y llamadas autenticadas. |
| Firebase Authentication | Registro, inicio de sesión, recuperación de contraseña y token de sesión. |
| Firestore | Perfiles, publicaciones, solicitudes, pases, comprobantes, eventos y cuotas. |
| Supabase Edge Function `woodmatch` | Verifica JWT de Firebase; aplica reglas de negocio, cuota, fotos y proveedores externos. |
| Supabase Storage | Fotos JPEG privadas; se entregan mediante URL firmada temporal. |
| Asistente botánico | Orientación en español y propuestas revisables; nunca publica ni modifica por sí solo. |
| Pl@ntNet | Identificación orientativa de plantas a partir de fotografías. |
| OpenStreetMap | Visualización del mapa y selección manual del punto. |

## Configuración permitida

- El APK utiliza `google-services.json`, `WOODMATCH_API_URL` y claves públicas necesarias.
- Los secretos viven solamente en Supabase/GitHub Actions: cuenta de servicio Firebase, `PASS_SECRET`, credenciales de proveedores y clave de servicio de Supabase.
- Nunca incluir `.env`, JSON de cuenta de servicio, claves de proveedores ni Service Role en Git, APK, capturas o chat.

## Operaciones de servidor

La función procesa, entre otras, publicación/edición, solicitud y transición de retiro, pase de retiro, carga de foto, URL firmada, guía botánica, lectura de evidencia, identificación de plantas, exportación y eliminación de cuenta. Todas requieren sesión Firebase válida y un perfil disponible.
