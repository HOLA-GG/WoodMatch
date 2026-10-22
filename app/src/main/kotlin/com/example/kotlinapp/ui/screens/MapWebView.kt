package com.example.kotlinapp.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kotlinapp.data.model.WoodWasteItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Estilos de capas basadas en el ecosistema OpenStreetMap (100% abiertas, gratuitas y de alta velocidad)
 */
enum class OsmLayerStyle(
    val layerId: String,
    val title: String,
    val subtitle: String,
    val tileUrl: String,
    val attribution: String,
    val maxZoom: Int = 19
) {
    STANDARD(
        layerId = "standard",
        title = "Calles",
        subtitle = "Barrios y vías de Suba",
        tileUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        attribution = "© OpenStreetMap contributors"
    ),
    HUMANITARIAN(
        layerId = "humanitarian",
        title = "Zonas verdes",
        subtitle = "Humedales y parques",
        tileUrl = "https://a.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png",
        attribution = "© OpenStreetMap contributors, Humanitarian OpenStreetMap Team"
    ),
    SATELLITE(
        layerId = "satellite",
        title = "Satélite",
        subtitle = "Imágenes aéreas",
        tileUrl = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
        attribution = "Source: Esri, Maxar, Earthstar Geographics"
    ),
    TOPO(
        layerId = "topo",
        title = "Relieve",
        subtitle = "Altitud y terreno",
        tileUrl = "https://a.tile.opentopomap.org/{z}/{x}/{y}.png",
        attribution = "© OpenStreetMap contributors, OpenTopoMap",
        maxZoom = 17
    )
}

/**
 * User-Agent de navegador móvil legítimo conforme a la política de uso de teselas de OpenStreetMap
 */
const val OSM_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Mobile; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 WoodMatchSuba/2.0"

/**
 * Componente MapWebView basado exclusivamente en el ecosistema OpenStreetMap.
 *
 * Características:
 * 1. 100% Gratuito, sin claves de Google Cloud ni bloqueos institucionales de Catastro.
 * 2. Motor Leaflet 1.9.4 embebido localmente desde assets para arranque instantáneo y seguro.
 * 3. Invocación automática de `map.invalidateSize()` para asegurar el cálculo del contenedor.
 * 4. Delimitación y enfoque en la localidad de Suba, Bogotá.
 * 5. Polígonos interactivos para todas las reservas ecológicas de Suba.
 * 6. Marcadores de residuos de madera sincronizados bidireccionalmente con Jetpack Compose.
 * 7. Alternancia instantánea entre 4 estilos de capas OSM (Estándar, Eco/Verde, Satélite y Relieve).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapWebView(
    wasteItems: List<WoodWasteItem> = emptyList(),
    ecologicalZones: List<EcologicalZone> = SubaEcologicalData.ZONES,
    showReserves: Boolean = true,
    centerLat: Double = 4.7450,
    centerLng: Double = -74.0850,
    zoomLevel: Int = 13,
    onWasteSelected: (WoodWasteItem) -> Unit = {},
    onReserveSelected: (EcologicalZone) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var selectedLayer by remember { mutableStateOf(OsmLayerStyle.STANDARD) }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Cargar librerías de Leaflet directamente desde assets locales (garantiza funcionamiento offline y sin bloqueo CDN)
    val localLeafletJs = remember {
        try {
            context.assets.open("leaflet.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w("MapWebView", "No se encontró leaflet.js local: ${e.message}")
            ""
        }
    }
    val localLeafletCss = remember {
        try {
            context.assets.open("leaflet.css").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w("MapWebView", "No se encontró leaflet.css local: ${e.message}")
            ""
        }
    }

    // Generar JSON seguro de residuos de madera para Leaflet
    val wasteJson = remember(wasteItems) {
        val array = JSONArray()
        wasteItems.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("lat", item.exactLatitude)
                put("lng", item.exactLongitude)
                put("sector", item.subaSector)
                put("quantity", item.formattedQuantity)
                put("status", item.typedStatus.name)
                put("statusLabel", item.typedStatus.displayName)
                put("uses", item.suggestedUses)
            }
            array.put(obj)
        }
        array.toString().replace("<", "\\u003c").replace(">", "\\u003e")
    }

    // Generar JSON de reservas naturales de Suba
    val reservesJson = remember(ecologicalZones, showReserves) {
        if (!showReserves) "[]"
        else {
            val array = JSONArray()
            ecologicalZones.forEach { zone ->
                val obj = JSONObject().apply {
                    put("name", zone.name)
                    put("type", zone.type)
                    put("desc", zone.description)
                    put("lat", zone.centerLat)
                    put("lng", zone.centerLng)
                    put("radW", zone.radiusWidthKm)
                    put("radH", zone.radiusHeightKm)
                    put("color", "#2E7D32")
                }
                array.put(obj)
            }
            array.toString().replace("<", "\\u003c").replace(">", "\\u003e")
        }
    }

    // Generar HTML autosuficiente de OpenStreetMap + Leaflet
    val htmlContent = remember(wasteJson, reservesJson, localLeafletJs, localLeafletCss) {
        buildOsmHtml(
            initialLayer = selectedLayer,
            wasteJson = wasteJson,
            reservesJson = reservesJson,
            centerLat = centerLat,
            centerLng = centerLng,
            zoom = zoomLevel,
            inlinedJs = localLeafletJs,
            inlinedCss = localLeafletCss
        )
    }

    // Alternancia dinámica de capa OSM sin recargar la página web
    LaunchedEffect(selectedLayer) {
        webViewRef?.evaluateJavascript(
            "if (window.switchOsmLayer) { window.switchOsmLayer('${selectedLayer.layerId}', '${selectedLayer.tileUrl}', '${selectedLayer.attribution}', ${selectedLayer.maxZoom}); }",
            null
        )
    }

    // Reubicación de cámara por sector de Suba
    LaunchedEffect(centerLat, centerLng, zoomLevel) {
        webViewRef?.evaluateJavascript(
            "if (window.panToLocation) { window.panToLocation($centerLat, $centerLng, $zoomLevel); }",
            null
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Fila de Selector de Capas OpenStreetMap
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(OsmLayerStyle.values()) { layer ->
                val isSelected = selectedLayer == layer
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedLayer = layer },
                    label = {
                        Text(
                            text = layer.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (layer) {
                                OsmLayerStyle.STANDARD -> Icons.Filled.Public
                                OsmLayerStyle.HUMANITARIAN -> Icons.Filled.Forest
                                OsmLayerStyle.SATELLITE -> Icons.Filled.Layers
                                OsmLayerStyle.TOPO -> Icons.Filled.Terrain
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Subtítulo descriptivo de la capa activa
        Text(
            text = selectedLayer.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Contenedor del Mapa
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("osm_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this

                        // 1. Aceleración por hardware para fluidez a 60 fps
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        // 2. Parámetros óptimos del motor web
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            allowFileAccess = false
                            allowContentAccess = false
                            allowFileAccessFromFileURLs = false
                            allowUniversalAccessFromFileURLs = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            userAgentString = OSM_USER_AGENT
                        }

                        // 3. Puente JS bidireccional entre Leaflet y Jetpack Compose
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onWasteClick(id: String) {
                                    post {
                                        val match = wasteItems.find { it.id == id }
                                        if (match != null) {
                                            onWasteSelected(match)
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun onReserveClick(name: String) {
                                    post {
                                        val match = ecologicalZones.find { it.name == name }
                                        if (match != null) {
                                            onReserveSelected(match)
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun onMapReady() {
                                    post {
                                        isLoading = false
                                    }
                                }
                            },
                            "AndroidBridge"
                        )

                        // 4. WebChromeClient para rastreo de progreso
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress
                                if (newProgress >= 90) {
                                    isLoading = false
                                }
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    Log.d("OsmWebView:JS", "${it.message()} [línea ${it.lineNumber()}]")
                                }
                                return true
                            }
                        }

                        // 5. WebViewClient para gestión de errores de red y SSL
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean = true
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                hasError = false
                                errorMessage = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                // Forzar recálculo de dimensiones del viewport en Leaflet
                                view?.evaluateJavascript(
                                    "if (window.invalidateMapSize) { window.invalidateMapSize(); }",
                                    null
                                )
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                    isLoading = false
                                    errorMessage = "Error de red: ${error?.description ?: "No se pudo conectar a OpenStreetMap"}"
                                }
                            }

                            @SuppressLint("WebViewClientOnReceivedSslError")
                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                handler?.cancel()
                            }
                        }

                        // Carga inicial del mapa con origen web seguro
                        loadDataWithBaseURL(
                            "https://www.openstreetmap.org",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                update = { webView ->
                    // Las transiciones reactivas se gestionan mediante los LaunchedEffect
                }
            )

            // Botón flotante para centrar rápidamente en Suba
            SmallFloatingActionButton(
                onClick = {
                    webViewRef?.evaluateJavascript(
                        "if (window.centerOnSuba) { window.centerOnSuba(); }",
                        null
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Centrar en Suba",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Indicador de progreso durante la carga inicial
            if (isLoading && !hasError) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Cargando mapa...",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedLayer.title} ($loadingProgress%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Vista de Error amigable con botón de reintento
            if (hasError) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No se pudo cargar el mapa",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = errorMessage ?: "Verifica tu conexión a internet para descargar las teselas de mapa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = {
                                hasError = false
                                isLoading = true
                                webViewRef?.loadDataWithBaseURL(
                                    "https://www.openstreetmap.org",
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Plantilla HTML con Leaflet 1.9.4 optimizada para OpenStreetMap, con polígonos de reservas
 * ecológicas, marcadores de madera, popup interactivo, invalidateSize automático y conmutación instantánea.
 */
private fun buildOsmHtml(
    initialLayer: OsmLayerStyle,
    wasteJson: String,
    reservesJson: String,
    centerLat: Double,
    centerLng: Double,
    zoom: Int,
    inlinedJs: String = "",
    inlinedCss: String = ""
): String {
    // Si tenemos los recursos locales en assets, los incrustamos directamente para garantizar 0 fallos de red
    val cssHeader = if (inlinedCss.isNotBlank()) {
        "<style>$inlinedCss</style>"
    } else {
        """
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" />
        """.trimIndent()
    }

    val jsHeader = if (inlinedJs.isNotBlank()) {
        "<script>$inlinedJs</script>"
    } else {
        """
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <title>OpenStreetMap Suba</title>
          $cssHeader
          $jsHeader
          <style>
            html, body {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              overflow: hidden;
              background-color: #e8ede4;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            }
            #map {
              width: 100%;
              height: 100%;
              background-color: #e8ede4;
            }
            .leaflet-control-zoom {
              border: none !important;
              box-shadow: 0 3px 10px rgba(0,0,0,0.22) !important;
              border-radius: 10px !important;
              overflow: hidden;
            }
            .leaflet-control-zoom a {
              background: #ffffff !important;
              color: #1b5e20 !important;
              font-weight: bold !important;
            }
            .suba-badge {
              position: absolute;
              top: 10px;
              left: 10px;
              z-index: 1000;
              background: rgba(255, 255, 255, 0.94);
              padding: 5px 12px;
              border-radius: 10px;
              font-size: 11px;
              font-weight: 700;
              color: #1b5e20;
              box-shadow: 0 3px 8px rgba(0,0,0,0.18);
              pointer-events: none;
            }
            .suba-legend {
              position: absolute;
              bottom: 8px;
              left: 8px;
              z-index: 1000;
              background: rgba(255, 255, 255, 0.92);
              padding: 4px 10px;
              border-radius: 8px;
              font-size: 10px;
              font-weight: 600;
              color: #333;
              box-shadow: 0 2px 6px rgba(0,0,0,0.15);
              pointer-events: none;
            }
            .popup-card {
              min-width: 180px;
              max-width: 240px;
              padding: 4px 2px;
            }
            .popup-title {
              font-weight: 700;
              font-size: 14px;
              color: #1b5e20;
              margin-bottom: 4px;
            }
            .popup-sector {
              font-size: 11px;
              color: #555;
              margin-bottom: 6px;
            }
            .popup-badge {
              display: inline-block;
              padding: 2px 8px;
              border-radius: 6px;
              font-size: 10px;
              font-weight: 700;
              margin-bottom: 8px;
            }
            .badge-DISPONIBLE { background: #e8f5e9; color: #2e7d32; border: 1px solid #a5d6a7; }
            .badge-RESERVADO { background: #fff3e0; color: #e65100; border: 1px solid #ffcc80; }
            .badge-APROVECHADO { background: #e1f5fe; color: #0288d1; border: 1px solid #81d4fa; }
            .badge-INACTIVO { background: #f5f5f5; color: #757575; border: 1px solid #e0e0e0; }
            .popup-btn {
              display: block;
              width: 100%;
              background: #2e7d32;
              color: white;
              border: none;
              border-radius: 8px;
              padding: 8px;
              font-weight: 600;
              font-size: 12px;
              cursor: pointer;
              text-align: center;
              box-sizing: border-box;
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <div id="badge" class="suba-badge">🌿 Suba • ${initialLayer.title}</div>
          <div class="suba-legend">Suba, Bogotá</div>

          <script>
            // 1. Inicializar mapa Leaflet centrado en Suba
            var map = L.map('map', {
              center: [$centerLat, $centerLng],
              zoom: $zoom,
              minZoom: 11,
              maxZoom: 19,
              zoomControl: false
            });

            L.control.zoom({ position: 'topright' }).addTo(map);

            // CRÍTICO: InvalidateSize para que Leaflet reconozca las dimensiones reales del contenedor WebView
            function invalidateSizeSafely() {
              if (map) {
                map.invalidateSize();
              }
            }
            window.invalidateMapSize = invalidateSizeSafely;
            setTimeout(invalidateSizeSafely, 100);
            setTimeout(invalidateSizeSafely, 300);
            setTimeout(invalidateSizeSafely, 750);
            window.addEventListener('resize', invalidateSizeSafely);

            // 2. Capa base de respaldo OSM siempre activa
            var osmFallback = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
              maxZoom: 19,
              opacity: 0.99,
              attribution: '© OpenStreetMap contributors'
            }).addTo(map);

            // 3. Capa activa seleccionada
            var activeTileLayer = null;

            function setLayer(layerId, url, attribution, maxZoom) {
              if (activeTileLayer) {
                map.removeLayer(activeTileLayer);
                activeTileLayer = null;
              }
              if (layerId !== 'standard') {
                activeTileLayer = L.tileLayer(url, {
                  maxZoom: maxZoom || 19,
                  attribution: attribution || '© OpenStreetMap contributors'
                });
                activeTileLayer.addTo(map);
              }
              invalidateSizeSafely();
            }

            setLayer('${initialLayer.layerId}', '${initialLayer.tileUrl}', '${initialLayer.attribution}', ${initialLayer.maxZoom});

            // Función expuesta para alternar capa en caliente
            window.switchOsmLayer = function(layerId, url, attribution, maxZoom) {
              setLayer(layerId, url, attribution, maxZoom);
              var badge = document.getElementById('badge');
              if (badge) {
                var title = '🌱 OSM Estándar';
                if (layerId === 'humanitarian') title = '🌿 Zonas Verdes (Eco)';
                if (layerId === 'satellite') title = '🛰️ Satelital Libre';
                if (layerId === 'topo') title = '🏔️ Relieve / Topo';
                badge.innerText = '🌿 Suba • ' + title;
              }
            };

            // Función expuesta para reubicar la cámara al cambiar de sector
            window.panToLocation = function(lat, lng, zoom) {
              map.flyTo([lat, lng], zoom || 14, { duration: 1.2 });
            };

            // Función para centrar en Suba
            window.centerOnSuba = function() {
              map.flyTo([4.7450, -74.0850], 13, { duration: 1.0 });
            };

            // 4. Inyectar Reservas Ecológicas de Suba con círculos y popups
            var reservesData = $reservesJson;
            reservesData.forEach(function(res) {
              var circle = L.circle([res.lat, res.lng], {
                color: res.color || '#2E7D32',
                fillColor: res.color || '#2E7D32',
                fillOpacity: 0.28,
                weight: 2,
                radius: (res.radW || 0.02) * 111000 * 0.85
              }).addTo(map);

              var popupHtml = '<div class="popup-card">' +
                '<div class="popup-title">🌿 ' + res.name + '</div>' +
                '<div class="popup-sector">' + res.type + '</div>' +
                '<p style="font-size:11px;color:#444;margin:4px 0 8px 0;">' + res.desc + '</p>' +
                '<button class="popup-btn" onclick="selectReserve(\'' + res.name.replace(/'/g, "\\'") + '\')">Consultar Reserva</button>' +
                '</div>';
              circle.bindPopup(popupHtml);
            });

            // 5. Inyectar Marcadores de Residuos de Madera
            var wasteData = $wasteJson;
            wasteData.forEach(function(item) {
              var color = '#2E7D32';
              if (item.status === 'RESERVADO') color = '#E65100';
              if (item.status === 'APROVECHADO') color = '#0288D1';
              if (item.status === 'INACTIVO') color = '#757575';

              var customIcon = L.divIcon({
                className: 'custom-pin',
                html: '<div style="background-color:' + color + ';width:22px;height:22px;border-radius:50%;border:2.5px solid #ffffff;box-shadow:0 2px 6px rgba(0,0,0,0.4);display:flex;align-items:center;justify-content:center;color:#fff;font-size:10px;font-weight:bold;">🪵</div>',
                iconSize: [22, 22],
                iconAnchor: [11, 11]
              });

              var marker = L.marker([item.lat, item.lng], { icon: customIcon }).addTo(map);

              var popupContent = document.createElement('div');
              popupContent.className = 'popup-card';
              var title = document.createElement('div'); title.className = 'popup-title'; title.textContent = item.title;
              var detail = document.createElement('div'); detail.textContent = item.sector + ' · ' + item.quantity + ' · ' + item.statusLabel;
              var action = document.createElement('button'); action.className = 'popup-btn'; action.textContent = 'Ver información de retiro';
              action.addEventListener('click', function() { window.selectWaste(item.id); });
              popupContent.appendChild(title); popupContent.appendChild(detail); popupContent.appendChild(action);
              marker.bindPopup(popupContent);
            });

            // 6. Funciones de interacción hacia Android
            window.selectWaste = function(id) {
              if (window.AndroidBridge && window.AndroidBridge.onWasteClick) {
                window.AndroidBridge.onWasteClick(id);
              }
            };

            window.selectReserve = function(name) {
              if (window.AndroidBridge && window.AndroidBridge.onReserveClick) {
                window.AndroidBridge.onReserveClick(name);
              }
            };

            // Avisar que el mapa está listo
            if (window.AndroidBridge && window.AndroidBridge.onMapReady) {
              window.AndroidBridge.onMapReady();
            }
          </script>
        </body>
        </html>
    """.trimIndent()
}
