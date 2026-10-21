package com.example.kotlinapp.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kotlinapp.data.model.WasteStatus
import com.example.kotlinapp.data.model.WoodWasteItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sistemas de Referencia Espacial de Catastro Bogotá disponibles
 */
enum class CatastroSrsReference(
    val code: String,
    val displayName: String,
    val subtitle: String,
    val serviceUrl: String
) {
    EPSG_3857(
        code = "EPSG:3857",
        displayName = "🌐 3857 (Web Mercator)",
        subtitle = "Estándar Web / Móvil",
        serviceUrl = "https://serviciosgis.catastrobogota.gov.co/arcgis/rest/services/Mapa_Referencia/mapa_hibrido/MapServer"
    ),
    EPSG_4686(
        code = "EPSG:4686",
        displayName = "📍 4686 (MAGNA-SIRGAS)",
        subtitle = "Geodésico Oficial Colombia",
        serviceUrl = "https://serviciosgis.catastrobogota.gov.co/arcgis/rest/services/Mapa_Referencia/mapa_hibrido_4686/MapServer"
    )
}

/**
 * Visor de mapa ultraligero que incrusta los servicios oficiales de Catastro Bogotá
 * utilizando Leaflet 1.9.4 + Esri-Leaflet y aceleración por hardware en Android.
 *
 * Soporta alternancia en caliente entre EPSG:3857 y EPSG:4686 sin recargar la página.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BogotaArcGisWebView(
    wasteItems: List<WoodWasteItem>,
    ecologicalZones: List<EcologicalZone>,
    showReserves: Boolean,
    selectedSrs: CatastroSrsReference = CatastroSrsReference.EPSG_3857,
    centerLat: Double = 4.7450,
    centerLng: Double = -74.0850,
    zoomLevel: Int = 13,
    onWasteSelected: (WoodWasteItem) -> Unit,
    onReserveSelected: (EcologicalZone) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Generar JSON de residuos para inyección en Leaflet
    val itemsJson = remember(wasteItems) {
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
        array.toString()
    }

    // Generar JSON de reservas ecológicas de Suba
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
            array.toString()
        }
    }

    // Código HTML ligero con Leaflet 1.9.4
    val htmlContent = remember(itemsJson, reservesJson) {
        buildBogotaMapHtml(
            itemsJson = itemsJson,
            reservesJson = reservesJson,
            initialSrsCode = selectedSrs.code,
            initialSrsUrl = selectedSrs.serviceUrl,
            centerLat = centerLat,
            centerLng = centerLng,
            zoom = zoomLevel
        )
    }

    // Conmutación en caliente de sistema de referencia (EPSG:3857 vs EPSG:4686) sin recargar página
    LaunchedEffect(selectedSrs) {
        webViewRef?.evaluateJavascript(
            "if (window.switchCrsService) { window.switchCrsService('${selectedSrs.code}', '${selectedSrs.serviceUrl}'); }",
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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewRef = this
                    // Aceleración por hardware para garantizar 60 fps en dispositivos de gama media y alta
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

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
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    // Puente bidireccional entre JavaScript y Jetpack Compose
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

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            // Se mantiene el visor con capa OpenStreetMap si hay error de red distrital
                        }
                    }

                    loadDataWithBaseURL(
                        "https://serviciosgis.catastrobogota.gov.co",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { webView ->
                // Actualizar WebView si es necesario
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(bottom = 6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cargando Ortoimágenes de Suba...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 46.dp)
                )
            }
        }
    }
}

/**
 * Plantilla HTML con Leaflet 1.9.4 optimizada para dispositivos móviles
 */
private fun buildBogotaMapHtml(
    itemsJson: String,
    reservesJson: String,
    initialSrsCode: String,
    initialSrsUrl: String,
    centerLat: Double,
    centerLng: Double,
    zoom: Int
): String {
    return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <title>Visor Catastro Bogotá - Suba</title>
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <script src="https://unpkg.com/esri-leaflet@3.0.12/dist/esri-leaflet.js"></script>
          <style>
            html, body {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              overflow: hidden;
              background-color: #17241a;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            }
            #map {
              width: 100%;
              height: 100%;
            }
            .leaflet-bar {
              border-radius: 8px !important;
              box-shadow: 0 2px 6px rgba(0,0,0,0.3) !important;
              border: none !important;
            }
            .leaflet-bar a {
              background-color: #ffffff !important;
              color: #1b5e20 !important;
              font-weight: bold !important;
            }
            .leaflet-popup-content-wrapper {
              border-radius: 14px;
              box-shadow: 0 4px 14px rgba(0,0,0,0.25);
              padding: 4px;
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
            .suba-banner {
              position: absolute;
              bottom: 8px;
              left: 8px;
              z-index: 1000;
              background: rgba(255, 255, 255, 0.92);
              padding: 4px 10px;
              border-radius: 8px;
              font-size: 10px;
              color: #1b5e20;
              font-weight: 600;
              box-shadow: 0 2px 6px rgba(0,0,0,0.15);
              pointer-events: none;
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <div id="srs-label" class="suba-banner">📍 Catastro Bogotá • $initialSrsCode</div>

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

            // 2. Delimitar estrictamente a Suba y proximidades para evitar desplazamientos accidentales
            var subaSouthWest = L.latLng(4.6800, -74.1500);
            var subaNorthEast = L.latLng(4.8350, -74.0200);
            var subaBounds = L.latLngBounds(subaSouthWest, subaNorthEast);
            map.setMaxBounds(subaBounds);

            // 3. Capa base de respaldo OpenStreetMap (por si la red distrital presenta retrasos)
            var osmFallback = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
              maxZoom: 19,
              opacity: 0.35,
              attribution: 'OpenStreetMap'
            }).addTo(map);

            // 4. Capa Activa de Catastro Bogotá (inicializada con $initialSrsCode)
            var activeBogotaLayer = null;

            function loadBogotaLayer(srsCode, serviceUrl) {
              if (activeBogotaLayer) {
                map.removeLayer(activeBogotaLayer);
              }
              activeBogotaLayer = L.esri.tiledMapLayer({
                url: serviceUrl,
                maxZoom: 19,
                attribution: 'Catastro Distrital (' + srsCode + ')'
              });
              activeBogotaLayer.addTo(map);
              activeBogotaLayer.bringToBack();
              osmFallback.bringToBack();

              var label = document.getElementById('srs-label');
              if (label) {
                label.innerText = '📍 Catastro Bogotá • ' + srsCode;
              }
            }

            loadBogotaLayer('$initialSrsCode', '$initialSrsUrl');

            // Función expuesta para conmutar el Sistema de Referencia en caliente
            window.switchCrsService = function(srsCode, url) {
              loadBogotaLayer(srsCode, url);
            };

            // 5. Inyectar Reservas Naturales de Suba
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

            // 6. Inyectar Marcadores de Residuos de Madera
            var wasteData = $itemsJson;
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

              var popupContent = '<div class="popup-card">' +
                '<div class="popup-title">' + item.title + '</div>' +
                '<div class="popup-sector">📍 ' + item.sector + ' • ' + item.quantity + '</div>' +
                '<span class="popup-badge badge-' + item.status + '">' + item.statusLabel + '</span>' +
                '<button class="popup-btn" onclick="selectWaste(\'' + item.id + '\')">Ver Información de Retiro</button>' +
                '</div>';

              marker.bindPopup(popupContent);
            });

            // 7. Funciones de interacción con Android
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

            window.panToLocation = function(lat, lng, zoom) {
              map.flyTo([lat, lng], zoom || 14, { duration: 1.2 });
            };

            if (window.AndroidBridge && window.AndroidBridge.onMapReady) {
              window.AndroidBridge.onMapReady();
            }
          </script>
        </body>
        </html>
    """.trimIndent()
}
