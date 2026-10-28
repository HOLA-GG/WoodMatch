# Operación, pruebas y lanzamiento

## Configuración de piloto

- Firebase permanece en Spark con Authentication por correo y Firestore.
- Supabase permanece en Free, sin tarjeta ni complementos de pago.
- Registrar Android con `com.example.kotlinapp.pilot` y ubicar `google-services.json` en `app/google-services.json`.
- Configurar GitHub Actions con `WOODMATCH_GOOGLE_SERVICES_JSON` y variable `WOODMATCH_API_URL`.
- Configurar los secretos del servidor únicamente en Supabase.

## Pruebas físicas mínimas

- Cámara, galería, cancelación y foto orientada/grande.
- GPS preciso/aproximado, permiso rechazado y elección manual de punto.
- Mapa con red y sin red; tema claro/oscuro y estaciones.
- Texto ampliado, TalkBack y animaciones reducidas.
- Registro, recuperación de contraseña, publicación, edición, solicitud, pase, cancelación y comprobante en dos cuentas/dispositivos.
- Asistencia botánica, Pl@ntNet, cuotas y desconexión.

Para cierres Android:

```bash
adb logcat -c
adb logcat -b crash -d
```

Antes de compartir, retirar correos, ubicaciones, tokens o claves.

## Entrega

GitHub Actions debe aprobar build Android, pruebas, lint, reglas y Edge checks. El APK final se adjunta a una **GitHub Release** con checksum, notas de versión y `build-info.json`; no se suben claves ni se publica en Play Store.

Antes de Play: identificar paquete definitivo, crear y custodiar clave de subida, generar AAB firmado, configurar App Signing, política de privacidad y pruebas cerradas.
