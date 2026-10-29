package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class WelcomeFeature(val icon: ImageVector, val title: String, val body: String)
private data class WelcomePage(val icon: ImageVector, val title: String, val body: String, val features: List<WelcomeFeature>)

@Composable
fun OnboardingScreen(onFinish: () -> Unit, onLogin: () -> Unit, onExplore: () -> Unit) {
    val pages = listOf(
        WelcomePage(Icons.Default.Forest, "La madera merece otra oportunidad",
            "WoodMatch conecta a la comunidad de Suba para recuperar madera y reducir residuos: una estiba, una poda o un mueble pueden convertirse en algo nuevo.",
            listOf(
                WelcomeFeature(Icons.Default.Search, "Busca con la lupa", "Encuentra materiales disponibles para tu próximo proyecto."),
                WelcomeFeature(Icons.Default.Map, "Explora el mapa", "Ubica materiales y hallazgos por sector."),
                WelcomeFeature(Icons.Default.Recycling, "Cierra el ciclo", "Encuentra → solicita → coordina → aprovecha.")
            )),
        WelcomePage(Icons.Default.Handshake, "Comparte y conecta",
            "Elige cómo participar: como generador, receptor o ambos. Un hallazgo informa; una oferta significa que tienes autorización para entregar el material.",
            listOf(
                WelcomeFeature(Icons.Default.Add, "Publica con +", "Añade fotografía, material, cantidad y sector."),
                WelcomeFeature(Icons.Default.Inventory2, "Solicita y coordina", "El receptor solicita; el generador acepta y acuerdan la entrega."),
                WelcomeFeature(Icons.Default.QrCodeScanner, "Confirma la recolección", "Usa el pase QR en la entrega y sigue el proceso en Mi actividad.")
            )),
        WelcomePage(Icons.Default.Eco, "Una comunidad que también cuida",
            "Tu cuenta abre herramientas para conocer el entorno, cuidar plantas y ayudar a mejorar WoodMatch.",
            listOf(
                WelcomeFeature(Icons.Default.CameraAlt, "Escáner botánico", "Identifica plantas con una foto cuando decidas usar la cámara."),
                WelcomeFeature(Icons.Default.Chat, "Orientación para tus plantas", "Consulta al asistente y explora la guía de especies."),
                WelcomeFeature(Icons.Default.SupportAgent, "Tu voz cuenta", "Envía ideas, incidencias y satisfacción; reporta contenido y consulta la respuesta.")
            ))
    )
    var page by rememberSaveable { mutableIntStateOf(0) }
    val item = pages[page]
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer))).padding(22.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(Icons.Default.Spa, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                Icon(item.icon, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Icon(Icons.Default.Eco, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text("WOODMATCH · " + (page + 1) + " / " + pages.size, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(item.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(item.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        item.features.forEach { feature ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(feature.icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(feature.title, style = MaterialTheme.typography.titleMedium)
                        Text(feature.body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (page == 2) Text("Como invitado puedes explorar inicio y mapa. Regístrate para publicar, solicitar y usar las herramientas de IA.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (page > 0) IconButton(onClick = { page-- }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Pantalla anterior") }
            Button(onClick = { if (page == pages.lastIndex) onFinish() else page++ }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                Text(if (page == pages.lastIndex) "Comenzar" else "Siguiente")
                Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
        }
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Iniciar sesión o crear cuenta") }
        TextButton(onClick = onExplore, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Explorar como invitado") }
    }
}
