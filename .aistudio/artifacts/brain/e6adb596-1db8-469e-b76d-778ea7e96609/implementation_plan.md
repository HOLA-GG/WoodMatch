# Plan de Implementación (Revisado): Visor Catastro Bogotá con Leaflet Ligero y Asistente Botánico Guiado

Este plan incorpora tus recomendaciones: utilizaremos **Leaflet** como motor principal de mapas en el WebView por ser extremadamente ligero, de código abierto y de alto rendimiento en dispositivos móviles (gama media y alta con aceleración por hardware), permitiendo alternar entre el **Sistema 3857** y **Sistema 4686** de Catastro Bogotá, con respaldo de OpenStreetMap.

---

## 1. Visor de Mapas Ultraligero con Leaflet (EPSG:3857 y EPSG:4686)

### Motor JavaScript Seleccionado
* **Leaflet 1.9.4 + Esri-Leaflet 3.0.12:**
  * Peso mínimo (< 140 KB) y nulo consumo excesivo de memoria RAM.
  * Cero dependencias de Google Cloud ni consumo de cuotas.
  * Gestos multitáctiles fluidos (zoom táctil, paneo suave, inercia).
* **Aceleración por Hardware en Android:**
  * Habilitar `setLayerType(View.LAYER_TYPE_HARDWARE, null)` en el `WebView` para garantizar 60 fps constantes sin tirones en equipos de gama media y alta.

### URLs Oficiales del Servicio Distrital
1. **Sistema 3857 (Web Mercator / WGS 84):**  
   `https://serviciosgis.catastrobogota.gov.co/arcgis/rest/services/Mapa_Referencia/mapa_hibrido/MapServer/`
2. **Sistema 4686 (MAGNA-SIRGAS Geográfico Colombia):**  
   `https://serviciosgis.catastrobogota.gov.co/arcgis/rest/services/Mapa_Referencia/mapa_hibrido_4686/MapServer/`
3. **Capa Base de Respaldo:** OpenStreetMap para garantizar disponibilidad continua si la red distrital presenta intermitencias.

### Controles en Pantalla
* Selector de chip dinámico en `TreeMapScreen.kt`:
  * `[🌐 Catastro 3857 (Web Mercator)]`
  * `[📍 Catastro 4686 (MAGNA-SIRGAS)]`
* Al cambiar el chip, la función JavaScript `switchCrsService(srsCode, url)` reemplaza la capa activa al vuelo sin recargar la página ni perder la posición de los marcadores de residuos ni de las reservas naturales.
* Restricción estricta de navegación a la **Localidad de Suba y proximidades** (`maxBounds`).

---

## 2. Calibración y Restricción Estricta de la API de Gemini

### Parámetros de Control de Respuestas
1. **Límite de Longitud (`maxOutputTokens: 280`):**
   * Configurado en el cuerpo de la solicitud JSON para forzar respuestas directas y breves.
2. **Instrucción de Sistema Blindada (Guardrails Botánicos):**
   * **Rol:** Asistente Botánico y de Gestión Forestal de WoodMatch para la localidad de Suba, Bogotá.
   * **Enfoque:** Ayudar exclusivamente a usuarios nuevos a clasificar madera y crear reportes de residuos leñosos en Suba.
   * **Regla Antidesvío:** No responder sobre ningún tema ajeno a árboles, madera y reciclaje ambiental de Suba.
   * **Estructura Estricta:** Respuesta obligatoria en **máximo 3 puntos clave numerados** (1. Identificación y estado, 2. Uso circular recomendado, 3. Instrucción para reportarlo).
3. **Filtro de Caracteres:**
   * Sanitización en tiempo real para eliminar asteriscos (`**`) y etiquetas markdown en bruto que afecten la estética de la app.

---

## 3. Experiencia Guiada para Nuevos Usuarios (Creación de Reportes)

### Flujo Interactivo Asistido en `SpeciesScreen.kt`
Reemplazaremos el diálogo abierto genérico por un modal interactivo guiado:
1. **Paso 1: Tipo de Madera o Especie**
   * Chips rápidos: *Poda / Ramas*, *Tronco sólido*, *Estibas / Tablas*, *Aserrín / Viruta* o especie identificada por foto.
2. **Paso 2: Sector de Suba**
   * Selección rápida entre los 7 sectores territoriales de Suba.
3. **Paso 3: Síntesis Inteligente (Gemini en 3 Puntos)**
   * Análisis instantáneo con las 3 recomendaciones clave.
4. **Paso 4: Acción Directa "Completar Reporte"**
   * Botón visible que transfiere al usuario a la pantalla de publicación (`PublishWoodWasteScreen`) con los campos pre-llenados, eliminando dudas en usuarios novatos.

---

## 4. Archivos a Modificar
1. `/app/src/main/kotlin/com/example/kotlinapp/ui/screens/BogotaArcGisWebView.kt`  
   * Implementación de Leaflet con soporte dual 3857 / 4686 y aceleración por hardware.
2. `/app/src/main/kotlin/com/example/kotlinapp/ui/screens/TreeMapScreen.kt`  
   * Selector interactivo de sistema de referencia (3857 / 4686) con diseño responsivo.
3. `/app/src/main/kotlin/com/example/kotlinapp/data/network/ForestAiService.kt`  
   * Parámetros de control de tokens, prompt estricto de 3 puntos y guardrails botánicos.
4. `/app/src/main/kotlin/com/example/kotlinapp/ui/screens/SpeciesScreen.kt`  
   * Modal interactivo de creación de reporte paso a paso para nuevos usuarios.

---

## 5. Verificación de Compilación y Rendimiento
* Ejecución de `compile_applet` para garantizar cero errores de sintaxis y tipado.
* Comprobación de que la carga del mapa sea ligera y suave para dispositivos de cualquier gama.
