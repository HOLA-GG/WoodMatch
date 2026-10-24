package com.example.kotlinapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    val resolved = if(dark) darkColorScheme(
        primary=accent,onPrimary=Color(0xFF15251C),primaryContainer=Color(0xFF294737),onPrimaryContainer=Color(0xFFD5EEDC),
        secondary=accent,secondaryContainer=Color(0xFF36493C),onSecondaryContainer=Color(0xFFE3F1E4),
        background=Color(0xFF191C18),onBackground=Color(0xFFF3EEE5),surface=Color(0xFF191C18),onSurface=Color(0xFFF3EEE5),
        surfaceVariant=Color(0xFF30372E),onSurfaceVariant=Color(0xFFD4DCCF),outline=Color(0xFFA0AA9B)
    ) else colorScheme.copy(background=SummerBackground,surface=SummerSurface)
    MaterialTheme(
        colorScheme = resolved,
        typography = Typography,
        content = content
    )
}
