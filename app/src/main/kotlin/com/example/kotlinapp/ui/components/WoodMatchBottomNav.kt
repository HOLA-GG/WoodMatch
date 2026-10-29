package com.example.kotlinapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class WoodMatchDestination(
    val label: String,
    val route: String,
    val icon: ImageVector,
    val prominent: Boolean = false,
)

/** Role changes the shortcuts, never the server's authorization decisions. */
@Composable
fun WoodMatchBottomNav(
    currentRoute: String,
    role: String = "GENERADOR",
    onNavigate: (String) -> Unit,
) {
    val roleName = role.uppercase()
    if (roleName == "SOPORTE") {
        GuestNavigationBar(currentRoute, listOf(
            WoodMatchDestination("Resumen", "support_home", Icons.Default.Dashboard),
            WoodMatchDestination("Casos", "support_inbox", Icons.Default.SupportAgent),
            WoodMatchDestination("Moderar", "support_moderation", Icons.Default.Gavel),
            WoodMatchDestination("Perfil", "profile", Icons.Default.Person),
        ), onNavigate)
        return
    }
    if (roleName == "INVITADO") {
        val guestItems = listOf(
            WoodMatchDestination("Inicio", "guest", Icons.Default.Home),
            WoodMatchDestination("Mapa", "guest_map", Icons.Default.Map),
            WoodMatchDestination("Cuenta", "login", Icons.Default.Person, true),
        )
        GuestNavigationBar(currentRoute, guestItems, onNavigate)
        return
    }
    val create = roleName in setOf("GENERADOR", "MIXTO")
    val search = roleName != "GENERADOR"
    val items = buildList {
        add(WoodMatchDestination("Inicio", "dashboard", Icons.Default.Home))
        add(WoodMatchDestination("Mapa", "tree_map", Icons.Default.Map))
        if (create) add(WoodMatchDestination("Crear", "report_tree", Icons.Default.Add, true))
        if (search) add(WoodMatchDestination("Buscar", "view_reports", Icons.Default.Search, true))
        add(WoodMatchDestination("Actividad", "my_activity", Icons.Default.ReceiptLong))
        add(WoodMatchDestination("Perfil", "profile", Icons.Default.Person))
    }
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier.heightIn(min = 96.dp),
        ) {
            items.forEach { destination ->
                val selected = currentRoute == destination.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { if (!selected) onNavigate(destination.route) },
                    icon = {
                        if (destination.prominent) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                shadowElevation = if (selected) 4.dp else 1.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            ) {
                                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Icon(destination.icon, null, Modifier.size(28.dp))
                                }
                            }
                        } else {
                            Icon(destination.icon, null, Modifier.size(24.dp))
                        }
                    },
                    label = {
                        Text(
                            destination.label,
                            Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 0.sp,
                                fontWeight = if (selected || destination.prominent) FontWeight.SemiBold
                                    else FontWeight.Medium,
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = if (destination.prominent) androidx.compose.ui.graphics.Color.Transparent
                            else MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.testTag("nav_${destination.route}").semantics {
                        contentDescription = when (destination.route) {
                            "my_activity" -> "Mi actividad"
                            "report_tree" -> "Crear publicación"
                            "view_reports" -> "Buscar materiales"
                            else -> destination.label
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun GuestNavigationBar(currentRoute: String, items: List<WoodMatchDestination>, onNavigate: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            items.forEach { item -> NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { if (currentRoute != item.route) onNavigate(item.route) },
                icon = { Icon(item.icon, null) },
                label = { Text(item.label, maxLines = 1) },
            ) }
        }
    }
}
