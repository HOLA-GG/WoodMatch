package com.example.kotlinapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun WoodMatchBottomNav(currentRoute: String, onNavigate: (String) -> Unit) {
    val items =
        listOf(
            Triple("Inicio", "dashboard", Icons.Default.Home),
            Triple("Mapa", "tree_map", Icons.Default.Map),
            Triple("Crear", "report_tree", Icons.Default.AddCircle),
            Triple("Mi actividad", "my_activity", Icons.Default.ReceiptLong),
            Triple("Perfil", "profile", Icons.Default.Person),
        )
    NavigationBar {
        items.forEach { (title, route, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { if (currentRoute != route) onNavigate(route) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(title) },
                modifier = Modifier.testTag("nav_$route"),
            )
        }
    }
}
