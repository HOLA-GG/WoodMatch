package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
                WelcomeFeature(Icons.Default.Forum, "Orientación para tus plantas", "Consulta al asistente y explora la guía de especies."),
                WelcomeFeature(Icons.Default.SupportAgent, "Tu voz cuenta", "Envía ideas, incidencias y satisfacción; reporta contenido y consulta la respuesta.")
            ))
    )

    var pageIndex by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var furthestPage by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var selectedFeature by remember { mutableStateOf<WelcomeFeature?>(null) }
    var accessOpen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val illustrations = listOf(
        com.example.kotlinapp.R.drawable.welcome_discover,
        com.example.kotlinapp.R.drawable.welcome_share,
        com.example.kotlinapp.R.drawable.welcome_transform
    )
    val titles = listOf("Dale otra vida\na la madera.", "Comparte madera.\nConecta personas.", "Transforma hoy.\nCuida el mañana.")
    val item = pages[pageIndex]
    val pageScroll = key(pageIndex) { rememberScrollState() }
    androidx.activity.compose.BackHandler {
        if (accessOpen) accessOpen = false
        else if (pageIndex > 0) pageIndex--
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer)
            .safeDrawingPadding().padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Forest, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(" WOODMATCH", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
            Text("${pageIndex + 1} / 3", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        androidx.compose.runtime.key(pageIndex) {
            Column(
                Modifier.weight(1f).verticalScroll(pageScroll),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(illustrations[pageIndex]),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).aspectRatio(1.35f),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                Text(titles[pageIndex], style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
                    fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(item.body, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("TOCA PARA CONOCER CÓMO FUNCIONA", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                item.features.forEach { feature ->
                    Card(onClick = { selectedFeature = feature }, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(feature.icon, null, tint = MaterialTheme.colorScheme.primary)
                            Text(feature.title, Modifier.weight(1f).padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.titleSmall)
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { index ->
                TextButton(
                    enabled = index <= furthestPage,
                    onClick = { pageIndex = index; accessOpen = false },
                    modifier = Modifier.width(56.dp).heightIn(min = 48.dp).semantics { contentDescription = "Paso ${index + 1} de 3" }
                ) {
                    Box(Modifier.height(6.dp).width(if (index == pageIndex) 28.dp else 8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(if (index == pageIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)))
                }
            }
        }
        if (!accessOpen) {
            FilledTonalButton(
                enabled = !pageScroll.canScrollForward,
                onClick = {
                    if (pageIndex == pages.lastIndex) accessOpen = true
                    else { pageIndex++; furthestPage = maxOf(furthestPage, pageIndex) }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(if (pageIndex == pages.lastIndex) Icons.Default.Check else Icons.Default.Eco, null)
                Text(if (pageScroll.canScrollForward) "Desliza y descubre" else if (pageIndex == pages.lastIndex) "¡Listo, comenzar!" else "Siguiente",
                    Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
            Text("Descubre · Comparte · Aprovecha", Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        } else {
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Crear cuenta o iniciar sesión")
            }
            TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Ya tengo una cuenta")
            }
            TextButton(onClick = onExplore, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Explorar como invitado")
            }
        }
    }

    if (selectedFeature != null) {
        AlertDialog(
            onDismissRequest = { selectedFeature = null },
            icon = { Icon(selectedFeature!!.icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary) },
            title = { Text(selectedFeature!!.title) },
            text = { Text(selectedFeature!!.body) },
            confirmButton = {
                TextButton(onClick = { selectedFeature = null }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@com.example.kotlinapp.ui.preview.WoodMatchPreviews
@Composable
private fun OnboardingScreenDesignPreview() {
    com.example.kotlinapp.ui.preview.PreviewFrame { OnboardingScreen({}, {}, {}) }
}

