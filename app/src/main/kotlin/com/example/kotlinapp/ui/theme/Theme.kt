package com.example.kotlinapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import android.app.Activity
import com.example.kotlinapp.data.model.SeasonalTheme

private val SummerColorScheme = lightColorScheme(
    primary = SummerPrimary,
    onPrimary = SummerOnPrimary,
    primaryContainer = SummerPrimaryContainer,
    onPrimaryContainer = SummerOnPrimaryContainer,
    secondary = SummerSecondary,
    onSecondary = SummerOnPrimary,
    secondaryContainer = SummerSecondaryContainer,
    onSecondaryContainer = SummerOnSecondaryContainer,
    tertiary = SummerTertiary,
    onTertiary = SummerOnPrimary,
    tertiaryContainer = SummerTertiaryContainer,
    onTertiaryContainer = SummerTertiary,
    background = SummerBackground,
    onBackground = SummerOnSurface,
    surface = SummerSurface,
    onSurface = SummerOnSurface,
    surfaceVariant = SummerSurfaceVariant,
    onSurfaceVariant = SummerOnSurfaceVariant,
    outline = SummerOutline
)

private val AutumnColorScheme = lightColorScheme(
    primary = AutumnPrimary,
    onPrimary = AutumnOnPrimary,
    primaryContainer = AutumnPrimaryContainer,
    onPrimaryContainer = AutumnOnPrimaryContainer,
    secondary = AutumnSecondary,
    onSecondary = AutumnOnPrimary,
    secondaryContainer = AutumnSecondaryContainer,
    onSecondaryContainer = AutumnOnSecondaryContainer,
    tertiary = AutumnTertiary,
    onTertiary = AutumnOnPrimary,
    tertiaryContainer = AutumnTertiaryContainer,
    onTertiaryContainer = AutumnTertiary,
    background = AutumnBackground,
    onBackground = AutumnOnSurface,
    surface = AutumnSurface,
    onSurface = AutumnOnSurface,
    surfaceVariant = AutumnSurfaceVariant,
    onSurfaceVariant = AutumnOnSurfaceVariant,
    outline = AutumnOutline
)

private val SpringColorScheme = lightColorScheme(
    primary = SpringPrimary,
    onPrimary = SpringOnPrimary,
    primaryContainer = SpringPrimaryContainer,
    onPrimaryContainer = SpringOnPrimaryContainer,
    secondary = SpringSecondary,
    onSecondary = SpringOnPrimary,
    secondaryContainer = SpringSecondaryContainer,
    onSecondaryContainer = SpringOnSecondaryContainer,
    tertiary = SpringTertiary,
    onTertiary = SpringOnPrimary,
    tertiaryContainer = SpringTertiaryContainer,
    onTertiaryContainer = SpringTertiary,
    background = SpringBackground,
    onBackground = SpringOnSurface,
    surface = SpringSurface,
    onSurface = SpringOnSurface,
    surfaceVariant = SpringSurfaceVariant,
    onSurfaceVariant = SpringOnSurfaceVariant,
    outline = SpringOutline
)

private val SnowColorScheme = lightColorScheme(
    primary = SnowPrimary,
    onPrimary = SnowOnPrimary,
    primaryContainer = SnowPrimaryContainer,
    onPrimaryContainer = SnowOnPrimaryContainer,
    secondary = SnowSecondary,
    onSecondary = SnowOnPrimary,
    secondaryContainer = SnowSecondaryContainer,
    onSecondaryContainer = SnowOnSecondaryContainer,
    tertiary = SnowTertiary,
    onTertiary = SnowOnPrimary,
    tertiaryContainer = SnowTertiaryContainer,
    onTertiaryContainer = SnowTertiary,
    background = SnowBackground,
    onBackground = SnowOnSurface,
    surface = SnowSurface,
    onSurface = SnowOnSurface,
    surfaceVariant = SnowSurfaceVariant,
    onSurfaceVariant = SnowOnSurfaceVariant,
    outline = SnowOutline
)

@Composable
fun WoodMatchTheme(
    season: SeasonalTheme = SeasonalTheme.SUMMER,
    appearance: String = "SYSTEM",
    textScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val colorScheme = when (season) {
        SeasonalTheme.SUMMER -> SummerColorScheme
        SeasonalTheme.AUTUMN -> AutumnColorScheme
        SeasonalTheme.SPRING -> SpringColorScheme
        SeasonalTheme.SNOW -> SnowColorScheme
    }

    val dark = appearance == "DARK" || (appearance == "SYSTEM" && isSystemInDarkTheme())
    val accent = when(season) {
        SeasonalTheme.AUTUMN -> Color(0xFFFFB68A)
        SeasonalTheme.SPRING -> Color(0xFFB3DDBD)
        SeasonalTheme.SNOW -> Color(0xFFABD4EA)
        else -> Color(0xFFA5D0B9)
    }
    val resolved = if (dark) {
        val darkPalette = when (season) {
            SeasonalTheme.SPRING -> arrayOf(
                Color(0xFFC2F1AD), Color(0xFF173316), Color(0xFF275426),
                Color(0xFFD8F8C6), Color(0xFF112411), Color(0xFFE8F5E1),
                Color(0xFF253B24), Color(0xFFC8DCC4), Color(0xFF8EAE87),
            )
            SeasonalTheme.SNOW -> arrayOf(
                Color(0xFFB9E4FF), Color(0xFF00344E), Color(0xFF0A4968),
                Color(0xFFD4EEFF), Color(0xFF081B27), Color(0xFFE6F5FF),
                Color(0xFF243B48), Color(0xFFC3D8E5), Color(0xFF8DA8B9),
            )
            SeasonalTheme.AUTUMN -> arrayOf(
                Color(0xFFFFB68A), Color(0xFF4A1F08), Color(0xFF6B3216),
                Color(0xFFFFDCC7), Color(0xFF26160E), Color(0xFFFFF0E8),
                Color(0xFF4A342A), Color(0xFFE7CFC3), Color(0xFFB89B8E),
            )
            SeasonalTheme.SUMMER -> arrayOf(
                Color(0xFFA5D0B9), Color(0xFF10291B), Color(0xFF294737),
                Color(0xFFD5EEDC), Color(0xFF191C18), Color(0xFFF3EEE5),
                Color(0xFF30372E), Color(0xFFD4DCCF), Color(0xFFA0AA9B),
            )
        }
        darkColorScheme(
            primary = darkPalette[0], onPrimary = darkPalette[1],
            primaryContainer = darkPalette[2], onPrimaryContainer = darkPalette[3],
            secondary = darkPalette[0], onSecondary = darkPalette[1],
            secondaryContainer = darkPalette[2], onSecondaryContainer = darkPalette[3],
            background = darkPalette[4], onBackground = darkPalette[5],
            surface = darkPalette[4], onSurface = darkPalette[5],
            surfaceVariant = darkPalette[6], onSurfaceVariant = darkPalette[7],
            outline = darkPalette[8],
        )
    } else colorScheme
    // Derive every surface role from the seasonal palette, avoiding Material's default lilac cards.
    val surfaces = resolved.copy(
        surfaceContainerLowest = resolved.surface,
        surfaceContainerLow = lerp(resolved.surface, resolved.primaryContainer, 0.08f),
        surfaceContainer = lerp(resolved.surface, resolved.primaryContainer, 0.14f),
        surfaceContainerHigh = lerp(resolved.surface, resolved.primaryContainer, 0.22f),
        surfaceContainerHighest = resolved.surfaceVariant,
        outlineVariant = lerp(resolved.surface, resolved.onSurface, 0.20f),
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    val systemDensity = LocalDensity.current
    val multiplier = textScale.takeIf { it.isFinite() }?.coerceIn(1f, 1.3f) ?: 1f
    // User preference adds to, and never replaces or reduces, Android accessibility font scaling.
    CompositionLocalProvider(
        LocalDensity provides Density(systemDensity.density, systemDensity.fontScale * multiplier)
    ) {
    MaterialTheme(
        colorScheme = surfaces,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(30.dp),
        ),
        content = content
    )
    }
}

