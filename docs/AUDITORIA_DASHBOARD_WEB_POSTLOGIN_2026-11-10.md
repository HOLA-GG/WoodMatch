# Auditoría del Dashboard Web WoodMatch — revisión pública y post-login

**Fecha de ejecución:** 10 de noviembre de 2026 (America/Bogota)  
**Estado:** informe de hallazgos para correcciones posteriores con Codex — **no es certificación de seguridad ni aceptación productiva**  
**Repositorio:** [HOLA-GG/WoodMatch](https://github.com/HOLA-GG/WoodMatch)  
**Sitio examinado:** https://woodmatch-piloto.firebaseapp.com/  
**Origen de evidencias:** inspecciones de Browser Use en navegador público y con una cuenta de prueba autorizada. La síntesis post-login procede del entregable Browser Use `woodmatch-auditoria-funcional.html`; el archivo original HTML no forma parte de este commit.  
**Política de datos:** este documento omite credenciales, UID, tokens, nombres, identificadores y valores privados de las tablas.

> [!IMPORTANT]
> El resultado **PASA** se limita a *carga básica de la vista*. No significa que las operaciones, los roles o los permisos estén verificados. Se preserva la diferencia entre **observado**, **hipótesis** y **corrección propuesta**. La actividad fue de consulta y navegación, sin escrituras administrativas deliberadas.

## 1. Alcance y método

1. **Revisión pública (sin login):** texto de acceso, botones de navegación, semántica básica, contraste, alineaciones y medidas responsive.
2. **Revisión post-login:** una sesión de prueba permitió navegar por Resumen, Incidentes, Moderación, Usuarios, Alertas, Servicios TI y Auditoría. Se observaron indicadores agregados, tablas y peticiones de red.
3. **Segundo recorrido:** navegación rápida y repetida para observar el tratamiento de límites. Se recibieron respuestas HTTP 429.
4. **No ejecutado:** crear/editar/eliminar usuarios o incidentes; moderar, enviar avisos, cambiar roles, descargar datos privados, inspeccionar detalles sensibles, ejecutar pruebas de autorización adversaria o hacer despliegues. No se aplicaron filtros ni se inspeccionaron cuerpos de solicitudes/respuestas.

El post-login mostró el estado **«Sesión administrativa activa.»**; no se confirmó el proveedor de identidad específico ni el rol efectivo del usuario. Por ello no se afirma que Firebase Authentication o la autorización del backend estén verificados en profundidad.

## 2. Resultado funcional post-login

| Módulo | Resultado | Evidencia agregada observada | Qué no se comprobó |
| --- | --- | --- | --- |
| Resumen | **PASA: carga básica** | 4 KPI y 4 casos recientes; 1 incidencia recibida, 0 resueltas/cerradas, 0 de prioridad alta/crítica; satisfacción «Sin datos suficientes», 0 respuestas | Precisión de cálculos, filtros, detalles y procedencia |
| Incidentes | **PASA: carga básica** | 4 registros: 2 RESUELTO, 1 NUEVO, 1 CERRADO | Detalle, búsqueda, cambio de estado y período |
| Moderación | **PASA: carga básica** | 7 registros: 2 HALLAZGO, 3 DISPONIBLE, 1 MODERADO, 1 APROVECHADO | Reglas, acciones y permisos de moderación |
| Usuarios | **PASA: carga básica** | 9 registros; los 9 con estado ACTIVA | Perfiles, cambios de nombre, bloqueo, baja y restricciones por rol |
| Alertas | **PASA: lista** | 9 registros; se muestra «Enviar aviso» | Destinatarios, confirmación y envío (no se pulsó) |
| Servicios TI | **PASA: carga básica** | Tabla agregada de 15 filas Servicio/Casos | Filtrado, detalle y validación de agregaciones |
| Auditoría | **PASA: lista** | 8 registros con estado COMPLETED; columnas Acción, Destino, Motivo, Fecha y Estado | Integridad, trazabilidad de operaciones, búsqueda y exportación |

Las cifras representan **el período y filtros por defecto observados el día de la prueba**. No se ha acreditado si son datos del piloto, fixtures o información real actual. No publicar identidades ni muestras de contenido privado en futuras evidencias.

## 3. Red, estado y accesibilidad

- **Primer recorrido:** peticiones al endpoint `/functions/v1/woodmatch-admin` con HTTP **200** y preflight **204**; contenido visible en los siete módulos.
- **Consola observada en primera pasada:** 0 errores, 0 advertencias y 0 excepciones; 0 fallos de transporte registrados. No equivale a ausencia de bugs.
- **Recorrido rápido posterior:** **13 respuestas HTTP 429**. La interfaz mostró exactamente: **«Espera un minuto antes de volver a consultar.»**; dejó de mostrar la tabla.
- Tras detener las consultas y esperar **62 segundos**, apareció **«Inicia sesión para continuar.»**. **No se pudo confirmar si expiró la sesión real, se invalidó el token o solo cambió el estado de frontend.**
- Las consultas a la función usaron método HTTP **POST** durante la navegación; como no se revisaron cuerpos ni efectos internos, no se afirma que fueran semánticamente de solo lectura en el servidor.
- Controles visibles post-login: filtros Desde/Hasta, Servicio y Actualizar; Auditoría ofrece Buscar registros. No fueron ejecutados.
- Accesibilidad básica: navegación semántica, etiquetas/nombres accesibles en muestras, `aria-current="page"` en el módulo activo; tipografía Inter. Contrastes calculados de muestra: encabezado 16,31:1; texto secundario 8,94:1; navegación activa 9,95:1. No se ejecutó auditoría integral con lector de pantalla ni teclado.

### Responsive post-login (observado)

| Vista | Dimensiones CSS | Resultado |
| --- | --- | --- |
| Escritorio | 1536 × 900 | Sin desbordamiento horizontal observado |
| Tablet | 768 × 1024 | Sin desbordamiento horizontal observado |
| Móvil | 390 × 844 | Sin desbordamiento horizontal observado; 7/7 botones de navegación visibles |

La menor altura observada de botón de navegación fue 41 px. **No desbordarse no equivale a usabilidad óptima**; sigue pendiente la ergonomía de tablas, filtros y acciones en móvil.

## 4. Revisión pública previa (sin autenticación)

Hallazgos de la inspección separada, para no confundirlos con resultados post-login:

| ID | Prioridad UX | Evidencia observada | Recomendación |
| --- | --- | --- | --- |
| UX-01 | P1 | Los siete botones de secciones parecían activos, pero la URL y el contenido siguieron en Resumen sin sesión; `aria-current` permaneció en Resumen | Deshabilitar/ocultar o etiquetar como «Requiere iniciar sesión», también para teclado |
| UX-02 | P1 | A 768 × 1024, sidebar de 200 px divide símbolo y texto «WoodMatch» en dos líneas | Mantener unidad visual de marca, ajustar logo/ancho/tamaño por breakpoint |
| UX-03 | P2 | Contorno de foco `#CE8C24` frente al fondo `#FFF8F3`: contraste calculado **2,70:1** | Oscurecer anillo o usar doble contorno; revisar foco en temas |
| UX-04 | P2 | En móvil 390 × 844, navegación ocupa unos **317 px** de alto y desplaza el formulario; documento de unos 938 px de alto | Cabecera/menú colapsable; priorizar acceso y contenido principal |
| UX-05 | P2 | «Tema» ofrece estaciones y «Apariencia» ofrece Sistema/Clara/Oscura | Renombrar «Tema» a «Estilo visual» o «Temporada» y describir ambos controles |
| UX-06 | P3 | Iconos Unicode están incluidos en el nombre accesible de botones | Separar iconos decorativos del texto accesible |
| UX-07 | Oportunidad | En escritorio sin sesión hay mucho espacio inferior vacío; copy breve de bloqueo | Mejorar composición del login y explicar permisos de forma clara |

En la revisión pública se verificó que **no hubo overflow horizontal** en tablet (768 × 1024) ni móvil (390 × 844). Ortografía y tildes de etiquetas visibles correctas («Contraseña», «Moderación», «Otoño», «Apariencia», «Auditoría»). Contrastes de texto medidos en muestras: principal 16,31:1, secundario 8,94:1, sidebar 15,06:1 y activo 9,95:1. Existían enlace «Saltar al contenido», `aria-label="Secciones"` y labels asociados.

## 5. Hallazgos para Codex, separados por riesgo

### PERF-01 — P2: HTTP 429 interrumpe la consulta y el estado visual

- **Hecho:** al recorrer módulos repetidamente se produjeron 13 respuestas HTTP 429 y el mensaje de espera; la tabla desapareció.
- **Efecto observado posterior:** al cabo de 62 segundos la UI mostró el formulario de login, **sin causa confirmada**.
- **Hipótesis a investigar, NO causa establecida:** solicitudes redundantes, falta de cache/coalescencia, política deliberada de cuota, tratamiento inadecuado del 429, o mezcla de estado de error con autenticación.
- **Candidatos de solución, sujetos a revisión:** deduplicación/cancelación de peticiones simultáneas, caché por sección y filtros, backoff respetando `Retry-After` cuando exista, conservar últimos datos y módulo seleccionado, mensaje con reintento controlado y manejo separado de 401/403/429. **No eliminar ni elevar límites de seguridad por defecto.**
- **Reproducción no destructiva:** con cuenta autorizada en entorno de pruebas, navegar entre módulos más rápido que el patrón habitual; inspeccionar tasa, trazas minimizadas y respuestas; detenerse al primer 429 en nuevas verificaciones para evitar carga innecesaria.

### AUTH-01 — Prioridad de investigación: estado de sesión tras errores de consulta

- **Hecho:** después del 429 y una pausa, se mostró «Inicia sesión para continuar.».
- **No demostrado:** cierre efectivo de Firebase, revocación, token caducado, fallo de permisos o cierre voluntario.
- **Comprobar:** conservar estado autenticado cuando la API responde 429; distinguir respuesta 401 de 403, 429 y error de red; registrar de forma segura eventos de transición de estado (sin credenciales ni PII); verificar cuándo realmente debe ofrecerse un nuevo login.
- **Criterio:** un 429 de datos no debe por sí solo presentar el estado «sin sesión» si el proveedor de identidad aún tiene sesión válida.

### UX-08 — P3: «Enviar aviso» junto a vista de consulta

- **Hecho:** el botón aparece en Alertas; **no se pulsó** y no se verificaron protecciones ni destinatarios.
- **Mejora:** separar consulta y composición; antes de enviar exigir resumen de destinatarios, contenido, permisos y confirmación explícita. Nunca enviar avisos en un smoke test sin autorización específica.

### SEC-01 — Verificaciones pendientes, sin fallo de seguridad demostrado

No están evaluados: validación del rol concreto, denegación de usuarios no autorizados, permisos por operación, rutas/acciones restringidas, aislamiento de datos ni integridad de auditoría. Debe probarse en un entorno controlado con identidades y permisos preparados, sin modificar datos reales.

## 6. Plan de corrección y criterios de aceptación para Codex

**Fase A — diagnóstico técnico (sin modificar políticas ni producción)**

- [ ] Identificar ruta de frontend que consulta `woodmatch-admin`, ciclos de render y disparadores de cada petición.
- [ ] Revisar política de cuota del backend, su respuesta 429 y el contrato de errores, **sin cambiar límites por defecto**.
- [ ] Determinar, mediante estados y logs minimizados, si el formulario reapareció por sesión ausente o por manejo de errores.
- [ ] Registrar referencias de código, commit base y pruebas reproducibles en el PR de corrección.

**Fase B — corrección acotada**

- [ ] Evitar consultas duplicadas al alternar secciones rápidamente; prevenir múltiples requests idénticos en vuelo.
- [ ] Preservar la última tabla/KPI válido frente a 429, con aviso no bloqueante y reintento manual/automático moderado.
- [ ] Diferenciar explícitamente los estados cargando, vacío, sin datos suficientes, sin permiso, cuota excedida, fallo de red y sesión realmente vencida.
- [ ] Corregir UX-01…UX-06 sin ocultar funciones de manera engañosa ni cambiar autorizaciones reales.
- [ ] Revisar teclado, foco, nombres accesibles y mobile-first para tablas y filtros.
- [ ] Si se trabaja Alertas, implementar confirmación segura antes de acciones, conservando las verificaciones de permisos del servidor.

**Fase C — pruebas de regresión y cierre**

- [ ] Tests unitarios del adaptador de API para HTTP 200, 401, 403, 429, errores de red y `Retry-After`.
- [ ] Test de ráfaga controlada con dobles solicitudes equivalentes y cambios de módulo; verificar que no se amplifique la carga.
- [ ] Login autorizado y visita a 7/7 módulos sin pérdidas del estado autenticado por errores 429.
- [ ] E2E en escritorio 1536 × 900, tablet 768 × 1024 y móvil 390 × 844; revisar también tablas horizontales, modal y teclado.
- [ ] Tests de roles permitidos/denegados y endpoints de solo lectura con fixtures o entorno controlado; no usar cuentas de producción para acciones destructivas.
- [ ] Aportar evidencia sin PII, salida de CI, lista de archivos, riesgos residuales y casos no cubiertos.
- [ ] Solicitar aprobación explícita antes de fusionar cambios operativos o desplegar a producción.

### Condiciones de no regresión

1. No desactivar rate limiting, autenticación, allowlists, CORS, revocación ni trazabilidad como «solución rápida».
2. No cambiar la aplicación Android ni la infraestructura de producción sin alcance y autorización explícitos.
3. No incluir contraseñas, secretos, JWT, claves de API, UID, correos de testing ni capturas de datos personales en issues, PRs o documentos.
4. Mantener separados los resultados con fixtures de los resultados reales del piloto.
5. No considerar corregido un hallazgo hasta repetir la prueba y obtener evidencia. **Si falla, corregir y volver a verificar**.

## 7. Trazabilidad y documentación relacionada

- **Fuente de esta revisión:** entregable Browser Use `woodmatch-auditoria-funcional.html` (post-login) más auditoría pública previa de UX/UI realizada el 10 de noviembre de 2026. Sus resultados se transcriben de forma minimizada en este archivo.
- **Dashboard público:** https://woodmatch-piloto.firebaseapp.com/
- **Interfaz de API inspeccionada:** Edge Function de Supabase `woodmatch-admin` (ruta `/functions/v1/woodmatch-admin`); no se inspeccionaron payloads.
- **Antecedentes de implementación:** [ADMINISTRATION_MIGRATION_REPORT.md](ADMINISTRATION_MIGRATION_REPORT.md), [DASHBOARD_EXECUTION_REPORT.md](DASHBOARD_EXECUTION_REPORT.md), [PUBLICAR_DASHBOARD_HOSTING.md](PUBLICAR_DASHBOARD_HOSTING.md), [OPERACION_Y_SEGURIDAD.md](OPERACION_Y_SEGURIDAD.md) y [QA_UI_Y_ENVIO_2026-11-09.md](QA_UI_Y_ENVIO_2026-11-09.md).
- **Límite:** es un registro de lo que vio el navegador, **no** una revisión del código actualizado, una pentest, una auditoría de base de datos, ni una comprobación de origen real de cada registro.

**Estado de seguimiento:** pendiente de diagnóstico/corrección con Codex y nueva verificación. Este archivo es solo documentación: su incorporación no modifica la app ni despliega Hosting/Edge Functions.
