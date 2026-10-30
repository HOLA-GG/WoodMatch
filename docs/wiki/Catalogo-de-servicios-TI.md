# Catálogo de servicios TI — WoodMatch 2.3

**Ingeniería de Software II · ITIL 4**  
**Versión:** 1.0 · **Fecha:** 30 de octubre de 2026  
**Ámbito:** Piloto Android para aprovechamiento comunitario de madera y residuos leñosos en Suba, Bogotá  
**Elaboración y responsables:** Juan Sebastian Fune Sanchez (desarrollo y operación técnica) · Yeison David Diaz Navarrete (gestión de pruebas y documentación)

> **Control de alcance.** Este catálogo describe capacidades identificadas en el código de WoodMatch y propone su gestión bajo ITIL 4. No constituye un SLA contractual ni una certificación de despliegue productivo. Los objetivos, indicadores y roles de gestión se consideran propuestas académicas hasta contar con mediciones y validación operativa. El estado de la versión 2.3 se relaciona con el PR #7 fusionado el 29-10-2026; las pruebas Android, reglas y lint asociadas a la integración estaban pendientes de evidencia.

## 1. Propósito y alcance

El catálogo establece una referencia organizada sobre los servicios digitales de WoodMatch, los usuarios que los consumen, sus dependencias, responsables y criterios de medición. Su finalidad es facilitar la comunicación del alcance, la atención de incidencias y la mejora continua del piloto. Se documentan servicios de usuario y servicios de soporte; los proveedores tecnológicos son *dependencias* y no servicios contratados directamente por la comunidad.

### Objetivo general

Documentar y clasificar los servicios TI de WoodMatch según su finalidad, acceso, dependencias, responsabilidades, riesgos e indicadores, aplicando la práctica de **gestión del catálogo de servicios** de ITIL 4.

### Objetivos específicos

- Identificar y describir los servicios digitales visibles y los servicios internos de operación.
- Vincular cada servicio con usuarios, recursos tecnológicos y puntos de control.
- Proponer indicadores y metas internas, distinguiéndolos de resultados medidos.
- Asignar responsabilidades al equipo real de dos integrantes mediante una matriz RACI.

## 2. Portafolio resumido

| Código | Servicio TI | Grupo | Destinatarios | Estado documental |
|---|---|---|---|---|
| ST-01 | Acceso, autenticación y cuentas | Comunitario | Cuentas registradas | Implementado en código |
| ST-02 | Registro de hallazgos y ofertas | Comunitario | Generador y mixto; hallazgos según permisos | Implementado en código |
| ST-03 | Catálogo de materiales | Comunitario | Comunidad autenticada | Implementado en código |
| ST-04 | Mapa interactivo OSM | Geográfico | Comunidad / visitante según pantalla | Implementado en código |
| ST-05 | Solicitudes y reservas con plazo | Comunitario | Generador, receptor y mixto | Implementado en código |
| ST-06 | Pase QR/código y verificación de retiro | Comunitario | Participantes en entrega | Implementado en código |
| ST-07 | Actividad, trazabilidad y comprobantes | Comunitario | Participantes en entrega | Implementado en código |
| ST-08 | Escáner de identificación botánica | Botánico | Cuentas autorizadas | Implementado en código |
| ST-09 | Álbum botánico personal y copias ZIP | Botánico | Cuentas autorizadas | Implementado en código |
| ST-10 | Orientación inteligente y apoyo a reportes | Botánico | Cuentas autorizadas | Implementado en código |
| ST-11 | Notificaciones y seguimiento | Operación | Comunidad y soporte | Implementado en código |
| ST-12 | Mesa de ayuda y gestión de incidencias | Operación | Comunidad y Cookiestech | Implementado en código |
| ST-13 | Moderación de publicaciones | Operación | Cookiestech | Implementado en código |
| ST-14 | Perfil, exportación y eliminación de cuenta | Transversal | Titulares de cuenta | Implementado en código; comprobar extremo a extremo |
| ST-15 | Monitoreo de indicadores KPI | Transversal | Equipo del proyecto | Prototipo académico existente; integración pendiente de validar |

**Interpretación de estados:** *Implementado en código* significa que se localizaron pantallas/operaciones asociadas; no implica que su despliegue, aceptación en dispositivo ni disponibilidad real estén certificados. *Prototipo* no implica integración productiva.

## 3. Fichas normalizadas de servicio

Las fichas describen **finalidad, acceso, dependencias, control y KPI**. Los indicadores se formulan para ser calculados posteriormente en el tablero; no se presentan valores no medidos.

### ST-01 · Acceso, autenticación y cuentas

**Finalidad.** Registrar usuarios, autenticar sesiones y recuperar contraseñas, respetando roles y permisos. **Acceso.** Inicio de sesión, registro y recuperación. **Usuarios.** Generador, receptor, mixto y operador autorizado. **Dependencias.** Firebase Authentication, Firestore y conexión remota. **Control.** Verificación de identidad en servidor, no inferir privilegios por el correo o el rol informado por Android. **KPI.** Autenticaciones exitosas / intentos válidos × 100. **Prácticas ITIL.** Seguridad de la información; solicitudes de servicio.

### ST-02 · Registro de hallazgos y ofertas

**Finalidad.** Registrar materiales y hallazgos con evidencia fotográfica, datos y ubicación confirmada. **Acceso.** Crear publicación. **Usuarios.** Ofertas: generadores/mixtos; hallazgos: perfiles autorizados. **Dependencias.** Android, Firestore, Supabase Storage y Edge Functions. **Control.** Foto, consentimiento de ubicación y autorización de entrega cuando aplique. **KPI.** Publicaciones completadas / intentos de publicación × 100. **ITIL.** Solicitudes de servicio; incidentes ante fallos.

### ST-03 · Catálogo de materiales

**Finalidad.** Consultar ofertas disponibles mediante búsqueda, filtros y detalles de material. **Acceso.** Catálogo. **Usuarios.** Comunidad autenticada. **Dependencias.** Firestore y conectividad. **Control.** Visibilidad de estados activos y permisos de lectura. **KPI.** Consultas satisfactorias / consultas realizadas × 100. **ITIL.** Disponibilidad; capacidad y rendimiento.

### ST-04 · Mapa interactivo OpenStreetMap

**Finalidad.** Explorar publicaciones georreferenciadas y confirmar puntos de ubicación. **Acceso.** Mapa y formularios de publicación. **Usuarios.** Usuarios según pantalla y permisos. **Dependencias.** OpenStreetMap, osmdroid, GPS opcional y caché local. **Control.** Confirmación consciente de ubicación; alternativa manual cuando GPS no está disponible. **KPI.** Cargas satisfactorias de mapa / intentos de carga × 100. **ITIL.** Disponibilidad; incidentes. **Límite.** La caché no garantiza mapas completos sin conexión y el proveedor de teselas es externo.

### ST-05 · Solicitudes, reservas y negociación

**Finalidad.** Gestionar solicitudes, aceptación o rechazo, fechas y contrapropuestas de retiro. **Acceso.** Catálogo y Mi actividad. **Usuarios.** Generadores, receptores y mixtos. **Dependencias.** Firestore, Edge Functions y notificaciones. **Control.** Una reserva aceptada bloquea otras aceptaciones incompatibles. **KPI.** Solicitudes respondidas antes de vencer / solicitudes con plazo evaluable × 100. **ITIL.** Solicitudes de servicio; disponibilidad. **Regla de negocio.** Las solicitudes vencen tras 72 h; **no** es un SLA de soporte.

### ST-06 · Pase QR y verificación de retiro

**Finalidad.** Comprobar la autorización de retiro mediante QR/código de vida corta. **Acceso.** Mi actividad. **Usuarios.** Participantes de la transacción. **Dependencias.** Android, autenticación y Edge Functions. **Control.** Pase temporal, de un solo uso; emisión nueva invalida el anterior. **KPI.** Validaciones exitosas / intentos legítimos de validación × 100. **ITIL.** Seguridad de la información; incidentes.

### ST-07 · Actividad y comprobantes digitales

**Finalidad.** Mostrar historial de las transiciones y permitir exportar comprobantes de entregas completadas en PDF/JSON. **Acceso.** Mi actividad. **Usuarios.** Participantes autorizados. **Dependencias.** Firestore y generación local de archivos. **Control.** Comprobante únicamente tras las confirmaciones exigidas; no equivale a factura ni acredita identidad. **KPI.** Entregas completadas con comprobante disponible / entregas completadas × 100. **ITIL.** Medición y reporte; seguridad de la información.

### ST-08 · Escáner botánico con Pl@ntNet

**Finalidad.** Analizar una fotografía y mostrar especies candidatas con puntajes de coincidencia. **Acceso.** Mi álbum botánico → Escanear planta. **Usuarios.** Cuentas autorizadas. **Dependencias.** Cámara/galería, Pl@ntNet y Edge Functions. **Control.** Selección y revisión humana de candidatos; cuotas del proveedor. **KPI.** Consultas con candidatos / consultas completadas de identificación × 100. **ITIL.** Disponibilidad; gestión de proveedores. **Límite.** Identificación orientativa, no certificación taxonómica.

### ST-09 · Álbum botánico personal

**Finalidad.** Guardar voluntariamente identificaciones, fotos, etiquetas y cuidados, con búsqueda y exportación/importación ZIP. **Acceso.** Mi álbum botánico. **Usuarios.** Cuentas autorizadas. **Dependencias.** Almacenamiento local de Android. **Control.** Datos separados por cuenta; exportar respaldo antes de desinstalar o cambiar de teléfono. **KPI.** Guardados y restauraciones exitosos / operaciones intentadas × 100. **ITIL.** Seguridad; continuidad. **Límite.** No es sincronización automática en la nube.

### ST-10 · Orientación inteligente y apoyo a reportes

**Finalidad.** Proporcionar recomendaciones botánicas y propuestas revisables a partir de evidencia o texto. **Acceso.** Guía botánica y flujo de publicación. **Usuarios.** Cuentas autorizadas. **Dependencias.** Gemini y Edge Functions. **Control.** Consulta explícita, validación de contenido y cuotas. **KPI.** Orientaciones respondidas / solicitudes procesadas × 100. **ITIL.** Gestión de proveedores; disponibilidad. **Límite.** No reemplaza asesoría profesional ni certifica especies, usos o seguridad estructural.

### ST-11 · Notificaciones y seguimiento

**Finalidad.** Comunicar eventos relevantes de solicitudes, acuerdos y respuestas de soporte. **Acceso.** Bandeja de notificaciones y canales habilitados por dispositivo. **Usuarios.** Participantes y soporte. **Dependencias.** Edge Functions, almacenamiento de eventos y configuración Android. **Control.** Notificaciones vinculadas a operaciones; registrar fallos de entrega cuando sea posible. **KPI.** Avisos entregados / avisos despachados con resultado verificable × 100. **ITIL.** Monitoreo y gestión de eventos.

### ST-12 · Mesa de ayuda y gestión de incidencias

**Finalidad.** Registrar, clasificar, priorizar, responder, resolver y reabrir casos; incluye feedback y satisfacción sin confundir estas categorías con incidentes. **Acceso.** Ayuda y participación; bandeja Cookiestech. **Usuarios.** Comunidad y operador. **Dependencias.** Firebase Authentication, Firestore, Edge Functions. **Control.** Permisos exclusivos por UID y correo; historial de transiciones, respuesta obligatoria para cerrar, motivo para reabrir. **KPI.** Tiempo de primera respuesta; casos resueltos / casos ingresados × 100; satisfacción media de 1 a 5. **ITIL.** Mesa de servicio; gestión de incidentes. **Límite.** No hay un proceso completo documentado de problema/causa raíz.

### ST-13 · Moderación de publicaciones

**Finalidad.** Revisar reportes y ocultar/restaurar publicaciones cuando el estado de negocio lo permita. **Acceso.** Cookiestech → Moderación. **Usuarios.** Operador de soporte. **Dependencias.** Firestore y Edge Functions. **Control.** Justificación obligatoria, historial de decisiones y restricciones para materiales reservados o retirados. **KPI.** Reportes de moderación atendidos / reportes recibidos × 100. **ITIL.** Seguridad de la información; gestión de incidentes.

### ST-14 · Gestión de perfil y datos personales

**Finalidad.** Actualizar datos permitidos, exportar información y solicitar eliminación de cuenta. **Acceso.** Perfil. **Usuarios.** Titulares de cuenta. **Dependencias.** Firebase Authentication, Firestore y backend. **Control.** Reautenticación para operaciones sensibles, controles de acceso y tratamiento de fallos parciales. **KPI.** Solicitudes de gestión de datos completadas / solicitudes válidas × 100. **ITIL.** Seguridad de la información; solicitudes de servicio. **Validación.** Probar de extremo a extremo en entorno controlado.

### ST-15 · Monitoreo y KPIs (prototipo académico)

**Finalidad.** Consolidar mediciones de uso, disponibilidad y atención para mejora continua. **Acceso.** Tablero de KPI del proyecto (herramienta externa o prototipo). **Usuarios.** Equipo del proyecto. **Dependencias.** Dataset validado, exportaciones o integración futura con la aplicación. **Control.** Identificar claramente métricas simuladas vs. reales. **KPI.** Indicadores actualizados en la periodicidad prevista / indicadores planificados × 100. **ITIL.** Medición y reporte; mejora continua. **Estado.** No se afirma integración en tiempo real.

## 4. Matriz RACI para equipo de dos integrantes

**R = Responsible:** ejecuta. **A = Accountable:** responde por el resultado. **C = Consulted:** aporta criterio. **I = Informed:** recibe información. En un equipo pequeño una persona puede asumir **R/A**, sin crear roles ficticios.

| Actividad de gestión | Juan Sebastian Fune Sanchez · Desarrollo técnico | Yeison David Diaz Navarrete · Pruebas y documentación |
|---|---|---|
| Arquitectura, programación y mantenimiento Android | R/A | C |
| Integración Firebase, Supabase, OSM e IA | R/A | I |
| Seguridad técnica y control de permisos | R/A | C |
| Corrección técnica de incidentes | R/A | C |
| Plan y ejecución de pruebas funcionales | C | R/A |
| Registro de evidencias de APK y resultados | C | R/A |
| Redacción y mantenimiento del catálogo ITIL | C | R/A |
| Revisión documental de KPIs y reportes | C | R/A |
| Definición y revisión de objetivos operativos | A | R |
| Validación conjunta de la entrega académica | A | R |

**Nota operativa.** La cuenta Cookiestech es una identidad técnica de soporte dentro del producto, no una tercera persona del equipo ni una función administrativa general. La operación real de esa cuenta debe asignarse y registrarse por el equipo antes de usarla con usuarios externos.

## 5. Metas académicas de atención (propuestas, no SLA vigentes)

Se proponen objetivos internos para el piloto; su conveniencia depende de horarios, demanda y capacidad de dos personas. El conteo empieza con el registro del incidente y se detiene según la definición de cada métrica; no existe aún evidencia de acuerdos formales o cumplimiento.

| Prioridad | Ejemplo orientativo | Objetivo de primera atención | Objetivo de solución o plan de contención |
|---|---|---|---|
| Crítica | Servicio esencial indisponible para todos | 4 h hábiles | 1 día hábil |
| Alta | Función principal bloqueada para varios usuarios | 8 h hábiles | 2 días hábiles |
| Media | Fallo parcial con alternativa de uso | 2 días hábiles | 5 días hábiles |
| Baja | Mejora, consulta o inconveniente menor | 3 días hábiles | Planificación en siguiente ciclo |

**Horario sugerido de medición:** lunes a viernes, 9:00–17:00 (hora de Colombia), excluyendo festivos; **pendiente de adopción por el equipo**. Las metas no deben mostrarse en la app como garantía hasta su aprobación y validación. El vencimiento de **72 horas de las solicitudes de materiales** es una regla de negocio independiente.

## 6. KPIs, definición y control de datos

| Indicador | Cálculo propuesto | Fuente de evidencia | Frecuencia sugerida |
|---|---|---|---|
| Acceso exitoso | Accesos correctos / intentos válidos × 100 | Registros seguros agregados de autenticación | Mensual |
| Publicación exitosa | Publicaciones confirmadas / intentos iniciados × 100 | Eventos de publicación | Mensual |
| Solicitudes dentro de plazo | Solicitudes respondidas antes de vencer / solicitudes evaluables × 100 | Solicitudes y marcas de tiempo | Semanal |
| Mapa disponible | Cargas satisfactorias / cargas intentadas × 100 | Pruebas de pantalla y monitoreo permitido | Mensual |
| Identificación botánica con resultados | Consultas con candidatos / consultas completadas × 100 | Pruebas y resultados agregados Pl@ntNet | Mensual |
| Primera respuesta | Promedio de (primera atención del operador − creación del caso) | Historial de `support_cases` | Semanal |
| Resolución de incidentes | Casos de tipo INCIDENCIA resueltos / incidencias registradas en cohorte × 100 | `support_cases`, tipo y estado | Mensual |
| Satisfacción | Media de calificaciones válidas de 1 a 5 | Casos SATISFACCION | Mensual |
| Comprobantes | Entregas cerradas con comprobante / entregas cerradas × 100 | Solicitudes y recibos | Mensual |

**Criterios de calidad de datos:** documentar ventana temporal, denominador, exclusiones, fuente, responsable y si el dato es simulado o real. La ausencia de dato debe mostrarse como **Sin datos**, nunca como cumplimiento del 100 %. No publicar UID, correos, teléfonos ni localizaciones de terceros en el tablero académico.

## 7. Riesgos, dependencias y controles prioritarios

| Riesgo o dependencia | Consecuencia | Control recomendado |
|---|---|---|
| Caída o cuotas de Firebase/Supabase | Acceso o transacciones no disponibles | Alertas, registro de fallos, prueba de recuperación y documentación de cuota |
| Servicio OSM o falta de red | Mapa incompleto o sin teselas nuevas | Alternativa de lista, caché limitada y ubicación manual |
| Límites de Pl@ntNet/Gemini | Identificación u orientación temporalmente indisponibles | Mensaje de cuota/error y alternativas manuales |
| Pérdida del almacenamiento local del álbum | Pérdida de registros en el dispositivo | Exportación ZIP y ensayo de restauración |
| Soporte atendido por solo dos personas | Demora en atención | Priorización, metas realistas y revisión periódica de cola |
| Publicación sin pruebas integrales | Incidentes operativos y de seguridad | CI Android, tests de reglas, lint y matriz de pruebas físicas antes de piloto |

## 8. Evidencias mínimas sugeridas para el docente

| Evidencia | Demostración | Responsable de recopilar | Estado |
|---|---|---|---|
| E-01 | Inicio de sesión y roles | Yeison | Por capturar |
| E-02 | Publicación y catálogo de materiales | Yeison | Por capturar |
| E-03 | Mapa OSM con puntos y filtros | Yeison | Por capturar |
| E-04 | Solicitud, aceptación, fecha, vencimiento y seguimiento | Yeison | Por capturar |
| E-05 | QR, confirmación doble y comprobante | Yeison | Por capturar |
| E-06 | Escáner Pl@ntNet y puntuación de candidato | Yeison | Por capturar |
| E-07 | Álbum, filtros, cuidados y respaldo ZIP | Yeison | Por capturar |
| E-08 | Caso creado por usuario y gestionado por Cookiestech | Yeison | Por capturar |
| E-09 | Notificación, reapertura y moderación autorizada | Yeison | Por capturar |
| E-10 | Tablero KPI con origen real/simulado identificado | Yeison | Por validar |

Juan verifica las condiciones técnicas y acompaña la corrección de incidencias encontradas durante las pruebas. Las capturas reales se incorporarán sin datos personales, tokens, claves ni ubicaciones sensibles.

## 9. Relación con ITIL 4

Este catálogo aplica la **gestión del catálogo de servicios** al declarar servicios, usuarios y condiciones de consumo; relaciona la **mesa de servicio** y la **gestión de incidentes** con el centro Cookiestech; vincula **medición y reporte** con indicadores; y considera **gestión de disponibilidad, seguridad de la información, proveedores y mejora continua** en los servicios dependientes de terceros. La clasificación es una adaptación académica, no una auditoría ni certificación de ITIL.

## 10. Conclusión

El catálogo identifica quince servicios TI de WoodMatch y establece una base de administración común para el desarrollo, las pruebas y la documentación. Las fichas, la matriz RACI, las metas propuestas y los indicadores permiten planificar un seguimiento consistente sin confundir funcionalidades implementadas con disponibilidad productiva. Su validación se fortalecerá con capturas del APK, pruebas reproducibles y revisión periódica de resultados.

## 11. Fuentes y control documental

- Repositorio: https://github.com/HOLA-GG/WoodMatch
- PR #7 (WoodMatch 2.3): https://github.com/HOLA-GG/WoodMatch/pull/7
- Arquitectura: [Arquitectura y servicios](Arquitectura-y-servicios)
- Operación: [Operación, pruebas y lanzamiento](Operacion-pruebas-y-lanzamiento)
- Manual: [Manual de usuario](Manual-de-usuario)
- Consulta oficial de prácticas ITIL: https://www.peoplecert.org/

**Historial:** v1.0, 30-10-2026 — catálogo inicial con quince servicios, RACI para dos personas, KPIs, riesgos y objetivos académicos. **Próxima revisión:** después de recopilar evidencias del APK y confirmar los objetivos de atención.

## Evidencias visuales del piloto

Las capturas revisadas se describen en la [galería de evidencias de servicios TI](Evidencias-de-servicios). Se incluye una captura verificable del [mapa OSM](evidencias/mapa/mapa-osm-suba.jpeg). Las demás imágenes seleccionadas se conservan en el paquete descargable de evidencias, pendiente de incorporación gráfica a esta carpeta. Una captura de historial con fechas y cantidades contradictorias se excluyó del material destinado a publicación académica.
