package com.pocketdev.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketdev.app.ui.utils.*

/**
 * Optimized Premium Components for All Device Tiers
 * 
 * These components automatically adapt to device capabilities:
 * - LOW: Minimal animations, no complex effects
 * - MEDIUM: Standard animations, simple effects
 * - HIGH: Full premium experience with all effects
 * 
 * Optimizations applied:
 * - Tween instead of spring animations
 * - Reduced animation duration
 * - No blur/complex shadows on low-end
 * - Hardware layer optimization
 * - Minimal recomposition
 */

// ============================================================
// OPTIMIZED ANIMATED CARD
// ============================================================

/**
 * Performance-optimized card with minimal but premium feel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    baseElevation: Dp = 2.dp,
    pressedElevation: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val tier = rememberPerformanceTier()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Optimized scale animation (tween instead of spring)
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = OptimizedAnimations.rememberButtonPressSpec(tier),
        label = "cardScale"
    )
    
    // Optimized elevation animation
    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed && enabled) pressedElevation else baseElevation,
        animationSpec = OptimizedAnimations.rememberElevationSpec(tier),
        label = "cardElevation"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .scale(animatedScale)
            .then(
                // Use graphicsLayer for hardware acceleration on supported devices
                if (tier != DevicePerformance.Tier.LOW) {
                    Modifier.graphicsLayer { 
                        shadowElevation = animatedElevation.value
                        clip = true
                    }
                } else {
                    Modifier
                }
            ),
        enabled = enabled,
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

// ============================================================
// OPTIMIZED GRADIENT BUTTON
// ============================================================

/**
 * Performance-optimized gradient button
 */
@Composable
fun OptimizedGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    gradientColors: List<Color>? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val tier = rememberPerformanceTier()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Simple scale animation
    val animatedScale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> 0.97f
            else -> 1f
        },
        animationSpec = OptimizedAnimations.rememberButtonPressSpec(tier),
        label = "buttonScale"
    )
    
    // Simple alpha animation
    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        animationSpec = OptimizedAnimations.rememberFadeSpec(tier),
        label = "buttonAlpha"
    )
    
    val colors = gradientColors ?: listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .scale(animatedScale)
            .graphicsLayer { alpha = animatedAlpha },
        enabled = enabled && !loading,
        shape = shape,
        color = Color.Transparent,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(colors))
                .padding(contentPadding)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

// ============================================================
// OPTIMIZED GLASS SURFACE
// ============================================================

/**
 * Simplified glass effect - optimized for performance
 * Uses solid colors with subtle border instead of blur on low-end devices
 */
@Composable
fun OptimizedGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable BoxScope.() -> Unit
) {
    val tier = rememberPerformanceTier()
    val enableComplexEffects = rememberEnableComplexEffects()
    
    // Simple fade-in animation
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = OptimizedAnimations.rememberFadeSpec(tier),
        label = "glassAlpha"
    )

    Surface(
        modifier = modifier.graphicsLayer { alpha = animatedAlpha },
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(
            alpha = if (enableComplexEffects) 0.85f else 0.95f
        ),
        border = if (tier != DevicePerformance.Tier.LOW) {
            androidx.compose.foundation.BorderStroke(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        } else null,
        tonalElevation = if (tier == DevicePerformance.Tier.HIGH) 2.dp else 0.dp
    ) {
        Box(content = content)
    }
}

// ============================================================
// OPTIMIZED LOADING INDICATOR
// ============================================================

/**
 * Minimal loading indicator - no complex animations on low-end
 */
@Composable
fun OptimizedLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 32.dp
) {
    val tier = rememberPerformanceTier()
    
    when (tier) {
        DevicePerformance.Tier.LOW -> {
            // Simple static indicator for low-end
            Surface(
                modifier = modifier.size(size),
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.3f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⋯",
                        style = MaterialTheme.typography.titleMedium,
                        color = color
                    )
                }
            }
        }
        else -> {
            // Standard loading indicator for mid/high-end
            CircularProgressIndicator(
                modifier = modifier.size(size),
                color = color,
                strokeWidth = 2.dp
            )
        }
    }
}

// ============================================================
// OPTIMIZED ANIMATED VISIBILITY
// ============================================================

/**
 * Performance-optimized animated visibility
 */
@Composable
fun OptimizedAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tier = rememberPerformanceTier()
    
    when (tier) {
        DevicePerformance.Tier.LOW -> {
            // Instant visibility change on low-end
            if (visible) {
                content()
            }
        }
        DevicePerformance.Tier.MEDIUM -> {
            // Simple fade animation
            AnimatedVisibility(
                visible = visible,
                modifier = modifier,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                content()
            }
        }
        DevicePerformance.Tier.HIGH -> {
            // Full animation with scale
            AnimatedVisibility(
                visible = visible,
                modifier = modifier,
                enter = fadeIn(animationSpec = tween(200)) + 
                        scaleIn(animationSpec = tween(200), initialScale = 0.95f),
                exit = fadeOut(animationSpec = tween(150)) + 
                       scaleOut(animationSpec = tween(150), targetScale = 0.95f)
            ) {
                content()
            }
        }
    }
}

// ============================================================
// OPTIMIZED BADGE
// ============================================================

/**
 * Simple badge without complex animations
 */
@Composable
fun OptimizedBadge(
    count: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = MaterialTheme.colorScheme.onError
) {
    if (count > 0) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraSmall,
            color = backgroundColor,
            contentColor = contentColor
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// ============================================================
// OPTIMIZED STAGGERED LIST
// ============================================================

/**
 * Helper for staggered list animations
 * Returns delay in milliseconds based on index and device tier
 */
@Composable
fun rememberStaggerDelay(index: Int): Int {
    val tier = rememberPerformanceTier()
    
    return remember(index, tier) {
        when (tier) {
            DevicePerformance.Tier.LOW -> 0  // No stagger on low-end
            DevicePerformance.Tier.MEDIUM -> index * 20  // Faster stagger
            DevicePerformance.Tier.HIGH -> index * 40   // Full stagger
        }
    }
}
