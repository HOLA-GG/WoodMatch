# Control de calidad y evidencias — 2026-11-03

Base auditada: `e42bc112d4eaa1e198b51577319d900fed5a100f`. Fuente vigente: código Android `app/`, `supabase/functions/woodmatch/` y páginas de este directorio. `Kotlin_App/` y documentos de prototipos no sustituyen la implementación actual. Este registro no certifica producción.

## Resultado del plan

| Actividad | Estado | Evidencia / condición de cierre |
|---|---|---|
| Catálogo, manual, roles, soporte, arquitectura e índice | Completado: existencia verificada | Páginas enlazadas en [Home](Home.md) |
| Matriz requisito/servicio/prueba | Completado documental | Tabla siguiente; ejecución física separada |
| Render real Mermaid en GitHub | Completado: ocho bloques existentes renderizados | Revisión de iframes Mermaid en las cuatro páginas principales; detalle en informe de ejecución |
| Capturas: inventario técnico | Completado | Dimensiones abajo; no acredita privacidad ni funcionamiento |
| Capturas: privacidad y aceptación física | Bloqueado por evidencia | Yeison valida datos visibles y aporta secuencias descritas abajo |
| KPI y metas académicas | Completado documental | [Definiciones](Dashboard-Web.md); catálogo mantiene metas propuestas |
| Ciclo soporte | Prueba automatizada disponible | `backend-tests/support.test.mjs`; no equivale a prueba en teléfono |
| Anexos de auditorías cruzadas | Bloqueado por evidencia | No están adjuntos en este encargo; aportar versiones finales anonimizadas |
| Wiki nativa | Pendiente deliberado | Mantener docs/wiki como fuente; sincronización manual revisada, nunca automática implícita |
| Dashboard real y vista previa Hosting | Pendiente de integración | [Manual](Dashboard-Web.md); sin despliegue autorizado |

## Trazabilidad

Los resultados de pruebas se registran en el PR; “pendiente” no significa aprobado. Release de referencia funcional: PR #7, no certifica que el APK instalado corresponda a ese commit.

| Requisito | Servicio | Pantalla / operación | Prueba y resultado | Evidencia / integración |
|---|---|---|---|---|
| R-01 | ST-01: Acceso, autenticación y cuentas | Ver ficha de catálogo y manual | Aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-02 | ST-02: Registro de hallazgos y ofertas | Ver ficha de catálogo y manual | Aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-03 | ST-03: Catálogo de materiales | Ver ficha de catálogo y manual | Aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-04 | ST-04: Mapa interactivo OSM | Ver ficha de catálogo y manual | Aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-05 | ST-05: Solicitudes y reservas con plazo | Ver ficha de catálogo y manual | Backend: domain.test.mjs / support.test.mjs; aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-06 | ST-06: Pase QR/código y verificación de retiro | Ver ficha de catálogo y manual | Backend: domain.test.mjs / support.test.mjs; aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-07 | ST-07: Actividad, trazabilidad y comprobantes | Ver ficha de catálogo y manual | Backend: domain.test.mjs / support.test.mjs; aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-08 | ST-08: Escáner de identificación botánica | Ver ficha de catálogo y manual | Aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-09 | ST-09: Álbum botánico personal y copias ZIP | Ver ficha de catálogo y manual | Aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-10 | ST-10: Orientación inteligente y apoyo a reportes | Ver ficha de catálogo y manual | Aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-11 | ST-11: Notificaciones y seguimiento | Ver ficha de catálogo y manual | Backend: domain.test.mjs / support.test.mjs; aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-12 | ST-12: Mesa de ayuda y gestión de incidencias | Ver ficha de catálogo y manual | Backend: domain.test.mjs / support.test.mjs; aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-13 | ST-13: Moderación de publicaciones | Ver ficha de catálogo y manual | Backend: domain.test.mjs / support.test.mjs; aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-14 | ST-14: Perfil, exportación y eliminación de cuenta | Ver ficha de catálogo y manual | Aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |
| R-15 | ST-15: Monitoreo de indicadores KPI | Ver ficha de catálogo y manual | Aceptación física pendiente | [Galería](Evidencias-de-servicios.md); PR #7 / commit base |

## Inventario técnico de capturas

| Archivo | Resolución | Alcance |
|---|---|---|
| [mi-actividad](evidencias/actividad/mi-actividad.jpeg) | 720 × 1600 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |
| [album-botanico](evidencias/botanico/album-botanico.jpeg) | 720 × 1600 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |
| [catalogo-comunitario](evidencias/catalogo/catalogo-comunitario.jpeg) | 720 × 1600 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |
| [privacidad-datos-exportacion](evidencias/cuenta/privacidad-datos-exportacion.jpeg) | 720 × 1600 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |
| [conexiones-en-suba](evidencias/directorio/conexiones-en-suba.jpeg) | 720 × 1600 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |
| [inicio-comunidad-circular](evidencias/inicio/inicio-comunidad-circular.jpeg) | 720 × 1600 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |
| [inicio-explora-woodmatch](evidencias/inicio/inicio-explora-woodmatch.jpeg) | 720 × 1600 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |
| [mapa-osm-suba](evidencias/mapa/mapa-osm-suba.jpeg) | 450 × 1000 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |
| [crear-reporte-evidencia](evidencias/reportes/crear-reporte-evidencia.jpeg) | 720 × 1600 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |
| [ayuda-participacion](evidencias/soporte/ayuda-participacion.jpeg) | 720 × 1600 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |
| [centro-soporte-cookiestech](evidencias/soporte/centro-soporte-cookiestech.jpeg) | 720 × 1600 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |
| [moderacion-casos](evidencias/soporte/moderacion-casos.jpeg) | 720 × 1600 | Interfaz; privacidad y legibilidad pendientes de revisión humana antes de republicar |

## Guía de evidencia pendiente (Yeison; apoyo técnico Juan)

Usar dos cuentas ficticias y material de prueba; anotar commit/APK, dispositivo, Android, fecha con zona, precondición, pasos, esperado, obtenido y archivo. No publicar UID, correo, teléfono, ubicación de vivienda, tokens ni QR vigente. Cubrir:

1. ST-01: registro → verificación → salir → entrar → contraseña incorrecta, sin mostrar contraseña.
2. ST-05: catálogo → solicitar → aceptar/rechazar; vencimiento 72 h en reloj controlado de pruebas; reprogramar y cancelar con motivo. No alterar reloj de producción.
3. ST-06/07: actividad → pase → rechazo por expirado → nueva emisión → doble confirmación → PDF/JSON privado consistente. Redactar códigos y datos identificables.
4. ST-11: bandeja antes/después del evento; push con permiso concedido y dispositivo configurado. Separar aviso creado de entrega push real.
5. ST-08/09: foto de planta → identificación exitosa → guardado voluntario → exportar ZIP → importar en entorno de prueba; comparar cantidad y contenido.
6. ST-12: crear incidencia → Cookiestech prioriza → responde → resuelve/cierra → titular reabre con motivo. Tercero no puede consultar ni reabrir.
7. ST-13: reportar publicación → ocultar con motivo → restaurar; material reservado debe rechazar moderación incompatible.

## Acciones correctivas

| Hallazgo verificable | Responsable | Prioridad | Fecha objetivo | Validación / cierre |
|---|---|---|---|---|
| Casos carecen de serviceId ST; no inferir del asunto | Juan | Alta | Antes de KPI por servicio real | Mostrar Sin clasificar; futura captura explícita compatible |
| Historial se recorta a 50 eventos; no hay evento inequívoco de primera respuesta | Juan | Alta | Antes de publicar tiempos de atención | Mostrar Sin datos suficientes; diseñar evento durable |
| Capturas ilustran interfaz, no transacciones completas | Yeison | Alta | Antes de cierre piloto | Ejecutar guía y revisión conjunta |
| Anexos finales de auditoría no disponibles | Yeison | Media | Antes de entrega académica | Incorporar copia anonimizada con procedencia |
| Wiki nativa separada de docs/wiki | Yeison | Baja | Próxima revisión | Registrar commit de sincronización manual |

## Regresión, riesgos y cierre

Por versión ejecutar pruebas de dominio y soporte, autenticación Edge, reglas en emulador, compilación/lint Android si cambia Android y pruebas del dashboard. Registrar comando, resultado, commit y entorno; no trasladar resultados de una versión anterior.

Riesgos: cuotas Firebase/Supabase y proveedores IA, teselas OSM, pérdida del álbum local y capacidad de dos operadores. Mantener alternativa manual, mensajes de error, ZIP de prueba y revisión semanal de cola. No prometer disponibilidad sin medición.

Cierre del piloto: pruebas obligatorias aprobadas, cero defectos críticos abiertos, evidencias anonimizadas completas, restauración ensayada y aceptación de ambos responsables. Lección: separar capacidad localizada en código, simulación académica y resultado medido.

## Versiones y registro documental

| Versión | Fuente | Estado |
|---|---|---|
| 2.3 / PR #7 | Catálogo y soporte actuales | Implementación en repositorio; dispositivo pendiente |
| main / e42bc11 | Auditoría 2026-11-03 | Línea base de este informe |
| Propuesta dashboard | PR de esta fase | Desarrollo; no desplegado |

2026-11-03: se añade control documental, inventario, trazabilidad, guía de capturas y seguimiento. Conservar historia de Git; no sobrescribir anexos antiguos como si fueran actuales.
