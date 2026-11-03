# Migración de administración a web — WoodMatch

## Alcance implementado

- Retirada de SupportHomeScreen.kt y AdminUsersScreen.kt, rutas de operador y menú administrativo del APK. Ayuda y participación conserva creación, seguimiento, respuestas y reapertura de casos propios, con encabezado centrado y degradado del tema Material.
- Retirados perfiles locales ficticios y detección de operador por UID/correo incrustado en Android. Roles administrativos heredados no conceden capacidades comunitarias. La comprobación clientAccess del servidor rechaza cuentas de la lista privada administrativa en el APK nuevo.
- Web de operación real sin selector demo ni cuentas preconfiguradas: casos, prioridades, respuestas, reapertura, moderación, corrección de nombres, avisos y eliminación con motivo.
- Paletas claras/oscuras de las cuatro estaciones tomadas literalmente de Color.kt y Theme.kt; preferencia local de apariencia, sin persistir la sesión Firebase.
- Backend dedicado woodmatch-admin: identidad Firebase, cuenta y revocación, lista privada de hasta dos UID, CORS exacto, límites de cuerpo/cuota, control de versiones y auditoría.
- Eliminación de cuentas reanudable: bloqueo/tombstone, limpieza de material/fotos, anonimización de entregas/casos, retiro de avisos/dispositivos e identidad al final. Ninguna cuenta administrativa puede ser eliminada por esta operación.

## Acciones externas confirmadas

App web WoodMatch Dashboard registrada en el proyecto Firebase existente. La colección support_cases mostraba dos documentos; no se copiaron, recrearon ni eliminaron. El propietario no tenía todavía una identidad en Firebase Authentication para el correo indicado; la sesión Google de consola es independiente.

No se manipuló la contraseña compartida por chat. No se modificaron casos reales ni se ejecutaron eliminaciones para probar funcionalidades.

## Validación

39 pruebas Node de dominio/soporte/KPI/administración aprobadas. 16 pruebas Edge (8 existentes, 4 lectoras y 4 administrativas) aprobadas con claves generadas en memoria y proveedores simulados. Comprobación Deno del backend aprobada; build Vite aprobado. Smoke de navegador: login, consulta, búsqueda, detalle, actualización con motivo, color exacto de otoño oscuro, móvil sin desbordamiento y limpieza de datos al cerrar sesión.

Las capturas de docs/dashboard-desktop.png y docs/dashboard-mobile.png proceden exclusivamente de fixtures de pruebas interceptados por Playwright. Esos fixtures están fuera del código que se distribuye al navegador. No representan métricas de producción.

Compilación/pruebas/lint Android: consultar el CI del PR antes de publicar APK. La autorización del usuario permite desplegar cuando la validación y el acceso real estén comprobados; no convierte pruebas con mocks en aceptación productiva.

## Pendientes de puesta en marcha

1. Alta segura de la cuenta del propietario en Firebase Authentication y obtención de su UID. Cookiestech puede usar su identidad de correo/contraseña sin buzón real.
2. Configurar ambos UID y orígenes de Hosting como secretos de servidor; verificar permisos reales sin crear roles amplios en Firestore.
3. Publicar y probar woodmatch-admin; después Hosting clásico con configuración exclusiva de dashboard. La CLI local carece de cuenta autorizada. Cloud Shell devuelve Site Unavailable en este navegador. La sesión de Firebase Console no autoriza automáticamente la CLI.
4. Verificar ambas cuentas y acceso denegado de terceros. Solo después desplegar el cierre del endpoint comunitario y distribuir el APK nuevo para evitar interrupciones.

No declarar publicado ni migrado el servicio hasta completar estos puntos. No se modifica el plan de facturación ni se usa App Hosting.

Manual actualizado: [Administración web](wiki/Administracion-Web.md). El informe documental previo se conserva como historial de fases anteriores.
