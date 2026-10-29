package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class WelcomePage(val icon: ImageVector, val title: String, val body: String)

@Composable
fun OnboardingScreen(onFinish: () -> Unit, onLogin: () -> Unit, onExplore: () -> Unit) {
    val pages = listOf(
        WelcomePage(Icons.Outlined.Forest, "La madera tiene otra oportunidad", "Descubre materiales y hallazgos que pueden volver a ser útiles en Suba."),
        WelcomePage(Icons.Outlined.Handshake, "Conecta con tu comunidad", "Encuentra materiales, publica aportes y conoce entidades locales."),
        WelcomePage(Icons.Outlined.Eco, "Haz más al registrarte", "Publica, solicita, usa el escáner botánico y consulta orientación para tus plantas."),
    )
    var page by rememberSaveable { mutableIntStateOf(0) }
    val item = pages[page]
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(item.icon, null, Modifier.padding(20.dp).height(96.dp), tint = MaterialTheme.colorScheme.primary)
        Text(item.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(item.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(36.dp))
        Text("${page + 1} de ${pages.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { if (page == pages.lastIndex) onFinish() else page++ }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (page == pages.lastIndex) "Comenzar" else "Continuar") }
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp)) { Text("Iniciar sesión o crear cuenta") }
        androidx.compose.material3.TextButton(onClick = onExplore, modifier = Modifier.fillMaxWidth()) { Text("Explorar como invitado") }
    }
}
