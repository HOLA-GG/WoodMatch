package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GuestMapScreen(onRegister: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        androidx.compose.material3.Icon(Icons.Outlined.Map, null, Modifier.height(96.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Mapa de la comunidad", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Regístrate para consultar publicaciones disponibles y ver sus ubicaciones aproximadas. Protegemos los datos de quienes participan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Crear cuenta para ver materiales") }
    }
}
