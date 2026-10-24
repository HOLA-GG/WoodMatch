package com.example.kotlinapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.kotlinapp.ui.pilot.CommunityScreen

data class EcologicalZone(
    val name: String,
    val type: String,
    val description: String,
    val centerLng: Double,
    val centerLat: Double,
    val radiusWidthKm: Double,
    val radiusHeightKm: Double,
    val color: Color
)

object SubaEcologicalData {
    val ZONES = listOf(
        EcologicalZone(
            name = "Reserva Thomas van der Hammen",
            type = "Reserva Forestal Productora",
            description = "Principal corredor biológico del norte de Bogotá conectando los Cerros Orientales con el Río Bogotá.",
            centerLng = -74.0750,
            centerLat = 4.7850,
            radiusWidthKm = 0.038,
            radiusHeightKm = 0.024,
            color = Color(0xFF2E7D32)
        ),
        EcologicalZone(
            name = "Humedal La Conejera",
            type = "Humedal y Reserva de Biodiversidad",
            description = "Humedal emblemático de Suba, hábitat de la tingua bogotana y curí sabanero.",
            centerLng = -74.0920,
            centerLat = 4.7560,
            radiusWidthKm = 0.024,
            radiusHeightKm = 0.009,
            color = Color(0xFF00796B)
        ),
        EcologicalZone(
            name = "Cerros de Suba",
            type = "Reserva Forestal y Mirador",
            description = "Ecosistema de bosque andino y colinas que dividen el valle de Suba con el Río Bogotá.",
            centerLng = -74.0850,
            centerLat = 4.7450,
            radiusWidthKm = 0.020,
            radiusHeightKm = 0.022,
            color = Color(0xFF388E3C)
        ),
        EcologicalZone(
            name = "Humedal Juan Amarillo (Tibabuyes)",
            type = "Humedal Ramsar Protegido",
            description = "El humedal más extenso de Bogotá, amortiguador hídrico en el límite entre Suba y Engativá.",
            centerLng = -74.1020,
            centerLat = 4.7180,
            radiusWidthKm = 0.035,
            radiusHeightKm = 0.008,
            color = Color(0xFF0288D1)
        ),
        EcologicalZone(
            name = "Humedal de Córdoba",
            type = "Humedal Urbano y Avifauna",
            description = "Corredor hídrico en Niza con más de 120 especies de aves registradas.",
            centerLng = -74.0670,
            centerLat = 4.7080,
            radiusWidthKm = 0.015,
            radiusHeightKm = 0.008,
            color = Color(0xFF0097A7)
        ),
        EcologicalZone(
            name = "Humedal Torca y Guaymaral",
            type = "Reserva Hídrica del Norte",
            description = "Cuerpo hídrico en la cuenca alta limítrofe con Chía y Usaquén.",
            centerLng = -74.0380,
            centerLat = 4.7950,
            radiusWidthKm = 0.018,
            radiusHeightKm = 0.012,
            color = Color(0xFF00897B)
        ),
        EcologicalZone(
            name = "Parque Mirador de los Nevados",
            type = "Parque Ecológico Distrital",
            description = "Antigua cantera restaurada como santuario de flora nativa y cosmovisión Muisca.",
            centerLng = -74.0815,
            centerLat = 4.7480,
            radiusWidthKm = 0.009,
            radiusHeightKm = 0.007,
            color = Color(0xFF43A047)
        ),
        EcologicalZone(
            name = "Bosques San José de Bavaria",
            type = "Zona Verde y Arbolado de Protección",
            description = "Sector de alta cobertura arbórea de acacias, cipreses y sauces en Suba Norte.",
            centerLng = -74.0620,
            centerLat = 4.7600,
            radiusWidthKm = 0.015,
            radiusHeightKm = 0.014,
            color = Color(0xFF4CAF50)
        )
    )
}


@Composable
fun TreeMapScreen(onNavigateBack: () -> Unit) {
    CommunityScreen(mapFirst = true, onCreate = {}, onEdit = {})
}
