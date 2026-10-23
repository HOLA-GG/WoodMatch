package com.example.kotlinapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.ui.navigation.Screen

data class NavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun WoodMatchBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavItem("Inicio", Screen.Dashboard.route, Icons.Filled.Home, "nav_home"),
        NavItem("Mapa", Screen.TreeMap.route, Icons.Filled.Map, "nav_map"),
        NavItem("Reportes", Screen.ViewReports.route, Icons.AutoMirrored.Filled.Assignment, "nav_reports"),
        NavItem("Perfil", Screen.Profile.route, Icons.Filled.Person, "nav_profile")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(72.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            val containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                androidx.compose.ui.graphics.Color.Transparent
            }
            val contentColor = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(containerColor)
                    .clickable {
                        if (!selected) {
                            onNavigate(item.route)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag(item.tag)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor
                )
            }
        }
    }
}
