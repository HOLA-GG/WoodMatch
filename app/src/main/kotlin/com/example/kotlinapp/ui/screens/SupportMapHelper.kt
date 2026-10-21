package com.example.kotlinapp.ui.screens

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.example.kotlinapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Gestor del SupportMapFragment para la integración tradicional de Google Maps SDK.
 * Configura las capas (Terreno, Satélite, Normal, Híbrido) y habilita los controles de tipo de mapa.
 */
object SupportMapHelper {

    /**
     * Inicializa y configura el SupportMapFragment en una FragmentActivity o contenedor de vistas.
     * @param activity La actividad anfitriona (FragmentActivity)
     * @param containerId El ID del contenedor (por ejemplo R.id.map_fragment_container)
     * @param initialMapType Tipo inicial: GoogleMap.MAP_TYPE_TERRAIN, MAP_TYPE_SATELLITE, MAP_TYPE_NORMAL, MAP_TYPE_HYBRID
     * @param onMapReady Callback invocado cuando el mapa está listo para operar
     */
    fun setupSupportMapFragment(
        activity: FragmentActivity,
        containerId: Int = R.id.map,
        initialMapType: Int = GoogleMap.MAP_TYPE_TERRAIN,
        onMapReady: ((GoogleMap) -> Unit)? = null
    ) {
        val mapFragment = activity.supportFragmentManager.findFragmentById(containerId) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also { fragment ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(containerId, fragment)
                    .commit()
            }

        mapFragment.getMapAsync { googleMap ->
            // 1. Configurar tipo de mapa (Terreno, Satélite, etc.)
            googleMap.mapType = initialMapType

            // 2. Habilitar controles de tipo de mapa y herramientas de UI
            googleMap.uiSettings.apply {
                isMapToolbarEnabled = true
                isZoomControlsEnabled = true
                isCompassEnabled = true
                isMyLocationButtonEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = true
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
            }

            // 3. Posicionar cámara por defecto en la Localidad de Suba, Bogotá
            val subaCenter = LatLng(4.7450, -74.0850)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(subaCenter, 13f))

            // 4. Agregar marcador de referencia de Suba Centro
            googleMap.addMarker(
                MarkerOptions()
                    .position(subaCenter)
                    .title("Suba Centro - Nodo de Economía Circular")
                    .snippet("Zona piloto de recolección y valorización de residuos leñosos")
            )

            onMapReady?.invoke(googleMap)
        }
    }
}
