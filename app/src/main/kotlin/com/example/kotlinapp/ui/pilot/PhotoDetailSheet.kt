package com.example.kotlinapp.ui.pilot

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.kotlinapp.R
import kotlin.math.abs

/** Photo stays behind the information panel; only the handle region resizes it. */
@Composable
internal fun PhotoDetailSheet(
    title: String,
    image: String?,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    var fraction by rememberSaveable { mutableFloatStateOf(0.57f) }
    var dragging by remember { mutableStateOf(false) }
    val settledFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "Tamaño del panel",
    )
    val visibleFraction = if (dragging) fraction else settledFraction
    val density = LocalDensity.current
    val notch: Shape = remember(density) {
        val corner = with(density) { 28.dp.toPx() }
        val dip = with(density) { 18.dp.toPx() }
        val half = with(density) { 38.dp.toPx() }
        GenericShape { size, _ ->
            val c = corner.coerceAtMost(size.width / 4)
            moveTo(0f, c)
            quadraticBezierTo(0f, 0f, c, 0f)
            lineTo(size.width / 2 - half, 0f)
            cubicTo(size.width / 2 - half / 2, 0f, size.width / 2 - half / 2, dip, size.width / 2, dip)
            cubicTo(size.width / 2 + half / 2, dip, size.width / 2 + half / 2, 0f, size.width / 2 + half, 0f)
            lineTo(size.width - c, 0f)
            quadraticBezierTo(size.width, 0f, size.width, c)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer)
            .safeDrawingPadding().imePadding()) {
            val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.78f)) {
                if (image != null) AsyncImage(
                    model = image, contentDescription = "Foto de $title",
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Image(painterResource(R.drawable.welcome_discover),
                    contentDescription = "Ilustración de madera recuperable; publicación sin fotografía",
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(24.dp))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(Color.Transparent, MaterialTheme.colorScheme.primaryContainer))))
            }
            FilledIconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface)) {
                Icon(Icons.Default.Close, "Cerrar detalle")
            }
            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(visibleFraction),
                shape = notch, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Column {
                    Box(Modifier.fillMaxWidth().height(48.dp)
                        .semantics {
                            contentDescription = "Ajustar tamaño del panel"
                            customActions = listOf(
                                CustomAccessibilityAction("Expandir información") { fraction = 0.9f; true },
                                CustomAccessibilityAction("Restaurar tamaño") { fraction = 0.57f; true },
                                CustomAccessibilityAction("Reducir información") { fraction = 0.32f; true },
                            )
                        }
                        .draggable(
                            state = rememberDraggableState { delta ->
                                fraction = (fraction - delta / heightPx).coerceIn(0.32f, 0.9f)
                            },
                            orientation = Orientation.Vertical,
                            onDragStarted = { dragging = true },
                            onDragStopped = {
                                fraction = listOf(0.32f, 0.57f, 0.9f).minBy { abs(it - fraction) }
                                dragging = false
                            }
                        ), contentAlignment = Alignment.Center) {
                        Box(Modifier.padding(top = 12.dp).width(32.dp).height(4.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    Box(Modifier.weight(1f)) { content() }
                }
            }
        }
    }
}
