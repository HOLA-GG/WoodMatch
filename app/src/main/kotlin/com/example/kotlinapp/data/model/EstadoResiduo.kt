package com.example.kotlinapp.data.model

import androidx.compose.ui.graphics.Color

/**
 * Estados del residuo para trazabilidad en Suba y pintado de pines en OpenStreetMap.
 * PENDIENTE se visualiza como un pin naranja brillante estilo Uber.
 */
enum class EstadoResiduo(
    val displayName: String,
    val hexColor: String,
    val composeColor: Color
) {
    PENDIENTE(
        displayName = "Pendiente de verificación",
        hexColor = "#FF9800",
        composeColor = Color(0xFFFF9800) // Naranja vibrante estilo Uber
    ),
    DISPONIBLE(
        displayName = "Disponible",
        hexColor = "#2E7D32",
        composeColor = Color(0xFF2E7D32) // Verde
    ),
    RESERVADO(
        displayName = "En Reserva",
        hexColor = "#E65100",
        composeColor = Color(0xFFE65100) // Ámbar oscuro
    ),
    APROVECHADO(
        displayName = "Aprovechado / Retirado",
        hexColor = "#0288D1",
        composeColor = Color(0xFF0288D1) // Azul
    ),
    INACTIVO(
        displayName = "Inactivo / Vencido",
        hexColor = "#757575",
        composeColor = Color(0xFF757575) // Gris
    )
}
