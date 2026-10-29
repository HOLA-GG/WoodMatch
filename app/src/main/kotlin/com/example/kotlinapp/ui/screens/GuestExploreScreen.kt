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
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GuestExploreScreen(onRegister: () -> Unit, onLogin: () -> Unit, onMap: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Explora WoodMatch", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Mira cómo la comunidad de Suba da una nueva vida a la madera.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onMap, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            androidx.compose.material3.Icon(Icons.Outlined.Map, null)
            Spacer(Modifier.padding(4.dp))
            Text("Ver mapa de Suba")
        }
        androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Icon(Icons.Outlined.Eco, null, tint = MaterialTheme.colorScheme.primary)
                Text("Mira todo lo que puedes hacer", style = MaterialTheme.typography.titleLarge)
                Text("Al crear tu cuenta podrás publicar madera, solicitar materiales, usar el escáner botánico y recibir orientación para el cuidado de plantas.")
                Button(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Comienza hoy") }
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Ya tengo una cuenta") }
    }
}
