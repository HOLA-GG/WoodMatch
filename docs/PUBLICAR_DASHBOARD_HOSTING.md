# Publicar WoodMatch Dashboard en Firebase Hosting

## Estado actual

- App web registrada y cuenta propietaria creada en Firebase Authentication.
- Dos administradores y orígenes HTTPS configurados en Supabase.
- Función woodmatch-admin desplegada y protegida. La prueba sin token con origen permitido exige iniciar sesión.
- Frontend compilado y comprobaciones aprobadas.
- **Pendiente:** Firebase CLI autenticada, despliegue del frontend y aceptación real de las dos cuentas.

## Usando el ZIP compilado entregado

1. Instala Node.js con npm si tu computador no lo tiene.
2. Descomprime WoodMatch_Dashboard_Firebase_Hosting.zip.
3. Abre PowerShell o la terminal de VS Code **en la carpeta que contiene firebase.json**.
4. Ejecuta:

```powershell
npx -y firebase-tools@latest login
npx -y firebase-tools@latest deploy --only hosting --project woodmatch-piloto --config firebase.json
```

En login, elige tu cuenta Google propietaria del proyecto. No envíes códigos, tokens ni contraseñas por chat. Este paquete ya contiene dist/ y config.json pública: no necesita npm install, recompilación ni firebase init.

Si Firebase informa que no existe el sitio, confirma en la consola el proyecto woodmatch-piloto y crea el sitio predeterminado desde Hosting o con:

```powershell
npx -y firebase-tools@latest hosting:sites:create woodmatch-piloto --project woodmatch-piloto
```

Después repite el comando deploy. La configuración del ZIP limita la publicación al sitio woodmatch-piloto. El alcance --only hosting evita desplegar reglas, Auth o funciones. No uses App Hosting ni cambies a Blaze para este sitio estático.

## Si trabajas desde el repositorio

La ruta de configuración es dashboard/firebase.json y los archivos se construyen con npm --prefix dashboard run build. Primero debes preparar dashboard/public/config.json con la configuración pública de la app registrada y el endpoint woodmatch-admin, siguiendo config.example.json, que debe apuntar a `/functions/v1/woodmatch-admin` (no `woodmatch-dashboard`). No uses una cuenta de servicio en frontend.

## Validación antes de completar la migración

Abre la URL HTTPS que devuelva Firebase CLI. Comprueba acceso de ambas cuentas, consulta de los casos existentes, temas, móvil, cierre de sesión y rechazo a una cuenta ajena. No borres usuarios reales para probar.

Solo después actualiza la función comunitaria y distribuye el APK nuevo, tal como indica [el cierre del trabajo](CIERRE_CHATGPT_WORK.md). No se ha aplicado todavía ese corte: el operador actual sigue disponible durante la transición.
