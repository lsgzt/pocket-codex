package com.pocketdev.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Dark theme palette
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003E5C),
    primaryContainer = Color(0xFF004D6E),
    onPrimaryContainer = Color(0xFFB3E5FC),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF003A14),
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF442B00),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF690005),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFFCDD9E5),
    outline = Color(0xFF30363D),
    outlineVariant = Color(0xFF21262D),
    inverseSurface = Color(0xFFE6EDF3),
    inverseOnSurface = Color(0xFF161B22)
)

// Light theme palette
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0277BD),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB3E5FC),
    onPrimaryContainer = Color(0xFF003E5C),
    secondary = Color(0xFF2E7D32),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF003A14),
    tertiary = Color(0xFFE65100),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFB71C1C),
    onError = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1A2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = Color(0xFF424242),
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0)
)

// ============================================================
// PREMIUM GRADIENT COLORS
// ============================================================

object GradientColors {
    object Dark {
        val primaryGradient = listOf(
            Color(0xFF4FC3F7),
            Color(0xFF00ACC1),
            Color(0xFF00838F)
        )
        val secondaryGradient = listOf(
            Color(0xFF81C784),
            Color(0xFF4CAF50),
            Color(0xFF388E3C)
        )
        val accentGradient = listOf(
            Color(0xFFFFB74D),
            Color(0xFFFF9800),
            Color(0xFFF57C00)
        )
        val surfaceGradient = listOf(
            Color(0xFF161B22),
            Color(0xFF0D1117)
        )
        val cardGradient = listOf(
            Color(0xFF1C2128),
            Color(0xFF161B22)
        )
        val elevatedSurfaceGradient = listOf(
            Color(0xFF21262D),
            Color(0xFF161B22)
        )
        val shimmerGradient = listOf(
            Color(0xFF21262D),
            Color(0xFF30363D),
            Color(0xFF21262D)
        )
        val heroGradient = listOf(
            Color(0xFF0D1117),
            Color(0xFF161B22),
            Color(0xFF1C2128)
        )
        val codeBackgroundGradient = listOf(
            Color(0xFF0D1117),
            Color(0xFF0A0F14)
        )
        val successGradient = listOf(
            Color(0xFF4CAF50),
            Color(0xFF2E7D32)
        )
        val errorGradient = listOf(
            Color(0xFFEF5350),
            Color(0xFFC62828)
        )
    }
    
    object Light {
        val primaryGradient = listOf(
            Color(0xFF29B6F6),
            Color(0xFF0288D1),
            Color(0xFF0277BD)
        )
        val secondaryGradient = listOf(
            Color(0xFF66BB6A),
            Color(0xFF43A047),
            Color(0xFF2E7D32)
        )
        val accentGradient = listOf(
            Color(0xFFFFB74D),
            Color(0xFFFF9800),
            Color(0xFFF57C00)
        )
        val surfaceGradient = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF8F9FA)
        )
        val cardGradient = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFFAFAFA)
        )
        val elevatedSurfaceGradient = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF5F5F5)
        )
        val shimmerGradient = listOf(
            Color(0xFFE0E0E0),
            Color(0xFFF5F5F5),
            Color(0xFFE0E0E0)
        )
        val heroGradient = listOf(
            Color(0xFFF8F9FA),
            Color(0xFFFFFFFF),
            Color(0xFFFAFAFA)
        )
        val codeBackgroundGradient = listOf(
            Color(0xFFFAFAFA),
            Color(0xFFF5F5F5)
        )
        val successGradient = listOf(
            Color(0xFF66BB6A),
            Color(0xFF43A047)
        )
        val errorGradient = listOf(
            Color(0xFFEF5350),
            Color(0xFFE53935)
        )
    }
}

// ============================================================
// BRUSH UTILITIES - Pre-configured gradient brushes
// ============================================================

object GradientBrushes {
    fun primaryHorizontal(darkTheme: Boolean): Brush = Brush.horizontalGradient(
        colors = if (darkTheme) GradientColors.Dark.primaryGradient 
                 else GradientColors.Light.primaryGradient
    )
    
    fun primaryVertical(darkTheme: Boolean): Brush = Brush.verticalGradient(
        colors = if (darkTheme) GradientColors.Dark.primaryGradient 
                 else GradientColors.Light.primaryGradient
    )
    
    fun surfaceVertical(darkTheme: Boolean): Brush = Brush.verticalGradient(
        colors = if (darkTheme) GradientColors.Dark.surfaceGradient 
                 else GradientColors.Light.surfaceGradient
    )
    
    fun cardVertical(darkTheme: Boolean): Brush = Brush.verticalGradient(
        colors = if (darkTheme) GradientColors.Dark.cardGradient 
                 else GradientColors.Light.cardGradient
    )
    
    fun heroVertical(darkTheme: Boolean): Brush = Brush.verticalGradient(
        colors = if (darkTheme) GradientColors.Dark.heroGradient 
                 else GradientColors.Light.heroGradient
    )
    
    fun shimmer(darkTheme: Boolean): Brush = Brush.linearGradient(
        colors = if (darkTheme) GradientColors.Dark.shimmerGradient 
                 else GradientColors.Light.shimmerGradient
    )
    
    fun successHorizontal(darkTheme: Boolean): Brush = Brush.horizontalGradient(
        colors = if (darkTheme) GradientColors.Dark.successGradient 
                 else GradientColors.Light.successGradient
    )
    
    fun errorHorizontal(darkTheme: Boolean): Brush = Brush.horizontalGradient(
        colors = if (darkTheme) GradientColors.Dark.errorGradient 
                 else GradientColors.Light.errorGradient
    )
}

// ============================================================
// COMPOSITION LOCALS - For accessing colors in composables
// ============================================================

data class GradientColorScheme(
    val primary: List<Color>,
    val secondary: List<Color>,
    val accent: List<Color>,
    val surface: List<Color>,
    val card: List<Color>,
    val elevatedSurface: List<Color>,
    val shimmer: List<Color>,
    val hero: List<Color>,
    val codeBackground: List<Color>,
    val success: List<Color>,
    val error: List<Color>
)

val LocalGradientColors = staticCompositionLocalOf {
    GradientColorScheme(
        primary = GradientColors.Dark.primaryGradient,
        secondary = GradientColors.Dark.secondaryGradient,
        accent = GradientColors.Dark.accentGradient,
        surface = GradientColors.Dark.surfaceGradient,
        card = GradientColors.Dark.cardGradient,
        elevatedSurface = GradientColors.Dark.elevatedSurfaceGradient,
        shimmer = GradientColors.Dark.shimmerGradient,
        hero = GradientColors.Dark.heroGradient,
        codeBackground = GradientColors.Dark.codeBackgroundGradient,
        success = GradientColors.Dark.successGradient,
        error = GradientColors.Dark.errorGradient
    )
}

@Composable
fun PocketDevTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val gradientColors = if (darkTheme) {
        GradientColorScheme(
            primary = GradientColors.Dark.primaryGradient,
            secondary = GradientColors.Dark.secondaryGradient,
            accent = GradientColors.Dark.accentGradient,
            surface = GradientColors.Dark.surfaceGradient,
            card = GradientColors.Dark.cardGradient,
            elevatedSurface = GradientColors.Dark.elevatedSurfaceGradient,
            shimmer = GradientColors.Dark.shimmerGradient,
            hero = GradientColors.Dark.heroGradient,
            codeBackground = GradientColors.Dark.codeBackgroundGradient,
            success = GradientColors.Dark.successGradient,
            error = GradientColors.Dark.errorGradient
        )
    } else {
        GradientColorScheme(
            primary = GradientColors.Light.primaryGradient,
            secondary = GradientColors.Light.secondaryGradient,
            accent = GradientColors.Light.accentGradient,
            surface = GradientColors.Light.surfaceGradient,
            card = GradientColors.Light.cardGradient,
            elevatedSurface = GradientColors.Light.elevatedSurfaceGradient,
            shimmer = GradientColors.Light.shimmerGradient,
            hero = GradientColors.Light.heroGradient,
            codeBackground = GradientColors.Light.codeBackgroundGradient,
            success = GradientColors.Light.successGradient,
            error = GradientColors.Light.errorGradient
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
