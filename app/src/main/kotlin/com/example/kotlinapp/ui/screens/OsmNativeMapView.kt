package com.example.kotlinapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import kotlinx.coroutines.delay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.kotlinapp.BuildConfig
import com.example.kotlinapp.R
import com.example.kotlinapp.data.model.WoodWasteItem
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/** One native map for browsing and selecting locations. No provider substitutions. */
@Composable
fun OsmNativeMapView(
    wasteItems: List<WoodWasteItem>,
    ecologicalZones: List<EcologicalZone> = emptyList(),
    showReserves: Boolean = false,
    centerLat: Double,
    centerLng: Double,
    zoomLevel: Int,
    onWasteSelected: (WoodWasteItem) -> Unit,
    onReserveSelected: (EcologicalZone) -> Unit = {},
    modifier: Modifier = Modifier,
    onPointSelected: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var loadingTiles by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var map by remember { mutableStateOf<MapView?>(null) }
    val pointClick by rememberUpdatedState(onPointSelected)
    val itemClick by rememberUpdatedState(onWasteSelected)
    DisposableEffect(lifecycle, map) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map?.onResume()
                Lifecycle.Event.ON_PAUSE -> map?.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) map?.onResume()
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Keep the user's zoom while choosing another point; coordinate changes only recenter.
    LaunchedEffect(zoomLevel, map) {
        map?.controller?.setZoom(zoomLevel.toDouble())
    }
    LaunchedEffect(centerLat, centerLng, map) {
        map?.controller?.setCenter(GeoPoint(centerLat, centerLng))
    }
    LaunchedEffect(map) {
        if (map != null) {
            delay(8000)
            loadingTiles = false // A cached map may not emit download callbacks.
        }
    }
    val tileHandler = remember {
        Handler(Looper.getMainLooper()) { message ->
            loadingTiles = false
            if (message.what == MapTileProviderBase.MAPTILE_FAIL_ID) failed = true
            false
        }
    }
    Box(modifier.clip(RoundedCornerShape(24.dp)).clipToBounds()) {
        AndroidView(
            modifier = Modifier.fillMaxSize().clipToBounds(),
            factory = { ctx ->
                Configuration.getInstance().apply {
                    load(ctx, ctx.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
                    userAgentValue =
                        "WoodMatch/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID}; https://github.com/HOLA-GG/WoodMatch)"
                    additionalHttpRequestProperties.remove("Referer")
                    osmdroidBasePath = File(ctx.filesDir, "osm")
                    osmdroidTileCache = File(ctx.filesDir, "osm/tiles")
                    expirationOverrideDuration = null
                    expirationExtendedDuration = 0L
                    // osmdroid honors Expires/Cache-Control, defaulting to 30 days when absent.
                    tileFileSystemCacheMaxBytes = 150L * 1024 * 1024
                    tileFileSystemCacheTrimBytes = 120L * 1024 * 1024
                }
                MapView(ctx).apply {
                    clipChildren = true
                    clipToPadding = true
                    setTileSource(
                        XYTileSource(
                            "OpenStreetMapStandard",
                            0,
                            19,
                            256,
                            ".png",
                            arrayOf("https://tile.openstreetmap.org/"),
                            "© OpenStreetMap contributors",
                        )
                    )
                    setMultiTouchControls(true)
                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                                view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }
                    minZoomLevel = 10.0
                    maxZoomLevel = 19.0
                    zoomController.setVisibility(
                        CustomZoomButtonsController.Visibility.NEVER
                    )
                    contentDescription =
                        "Mapa de publicaciones en Suba. También puedes consultarlas en la lista accesible."
                    controller.setZoom(zoomLevel.toDouble())
                    controller.setCenter(GeoPoint(centerLat, centerLng))
                    tileProvider.tileRequestCompleteHandlers.add(tileHandler)
                    map = this
                }
            },
            update = { view ->
                view.overlays.clear()
                if (pointClick != null)
                    view.overlays.add(
                        MapEventsOverlay(
                            object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                    pointClick?.invoke(p.latitude, p.longitude)
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint): Boolean {
                                    pointClick?.invoke(p.latitude, p.longitude)
                                    return true
                                }
                            }
                        )
                    )
                wasteItems.forEach { item ->
                    val marker =
                        Marker(view).apply {
                            position = GeoPoint(item.exactLatitude, item.exactLongitude)
                            title = item.title
                            snippet = "${item.subaSector} · ${item.typedStatus.displayName}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon =
                                androidx.core.content.ContextCompat.getDrawable(
                                        context,
                                        if (
                                            item.status == "RESERVADO" ||
                                                item.entryType == "HALLAZGO"
                                        )
                                            R.drawable.ic_map_pin_orange
                                        else R.drawable.ic_map_pin_green,
                                    )
                                    ?.mutate()
                            setOnMarkerClickListener { _, _ ->
                                itemClick(item)
                                true
                            }
                        }
                    view.overlays.add(marker)
                }
                view.invalidate()
            },
            onRelease = { view ->
                view.parent?.requestDisallowInterceptTouchEvent(false)
                view.tileProvider.tileRequestCompleteHandlers.remove(tileHandler)
                tileHandler.removeCallbacksAndMessages(null)
                view.setOnTouchListener(null)
                view.onPause()
                view.onDetach()
            },
        )
        if (loadingTiles && !failed)
            Surface(
                Modifier.align(Alignment.TopCenter).padding(12.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Cargando mapa…", style = MaterialTheme.typography.labelMedium)
                }
            }
        Surface(
            Modifier.align(Alignment.CenterEnd).padding(12.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
        ) {
            Column {
                IconButton(onClick = { map?.controller?.zoomIn() }) {
                    Icon(Icons.Default.Add, contentDescription = "Acercar mapa")
                }
                HorizontalDivider(Modifier.width(48.dp))
                IconButton(onClick = { map?.controller?.zoomOut() }) {
                    Icon(Icons.Default.Remove, contentDescription = "Alejar mapa")
                }
            }
        }
        if (failed)
            Card(Modifier.align(Alignment.TopCenter).padding(8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "No se pudo cargar parte del mapa. Los puntos guardados siguen disponibles."
                    )
                    TextButton(
                        onClick = {
                            failed = false
                            map?.invalidate()
                        }
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        Surface(
            Modifier.align(Alignment.BottomStart).padding(4.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.openstreetmap.org/copyright"),
                            )
                        )
                    }
                }
            ) {
                Text("© OpenStreetMap contributors", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

