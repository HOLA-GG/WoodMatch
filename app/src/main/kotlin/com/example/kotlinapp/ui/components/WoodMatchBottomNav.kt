package com.example.kotlinapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WoodMatchBottomNav(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple("Inicio", "dashboard", Icons.Default.Home),
        Triple("Mapa", "tree_map", Icons.Default.Map),
        Triple("Nuevo", "report_tree", Icons.Default.AddCircle),
        Triple("Actividad", "my_activity", Icons.Default.ReceiptLong),
        Triple("Perfil", "profile", Icons.Default.Person),
    )
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            items.forEach { (title, route, icon) ->
                val selected = currentRoute == route
                NavigationBarItem(
                    selected = selected,
                    onClick = { if (!selected) onNavigate(route) },
                    icon = { Icon(icon, null, Modifier.size(24.dp)) },
                    label = {
                        Text(
                            title,
                            Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 0.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.testTag("nav_$route").semantics {
                        contentDescription = if (route == "my_activity") "Mi actividad" else title
                    },
                )
            }
        }
    }
}
