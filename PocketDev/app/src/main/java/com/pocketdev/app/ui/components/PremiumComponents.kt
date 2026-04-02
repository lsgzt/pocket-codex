package com.pocketdev.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketdev.app.ui.theme.GradientBrushes
import com.pocketdev.app.ui.theme.LocalGradientColors
import com.pocketdev.app.ui.utils.*

// ============================================================
// GRADIENT BUTTON - Premium button with shimmer effect
// ============================================================

/**
 * A premium gradient button with optional shimmer loading effect
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    gradientColors: List<Color>? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val gradientColorsResolved = gradientColors 
        ?: LocalGradientColors.current.primary
    
    // Animated scale for press feedback
    val animatedScale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> 0.96f
            isHovered -> 1.02f
            else -> 1f
        },
        animationSpec = SpringSpecs.buttonPress,
        label = "buttonScale"
    )
    
    // Animated alpha for enabled state
    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        animationSpec = TweenSpecs.fadeQuick,
        label = "buttonAlpha"
    )
    
    Box(
        modifier = modifier
            .scale(animatedScale)
            .graphicsLayer { alpha = animatedAlpha }
            .clip(shape)
            .background(Brush.horizontalGradient(gradientColorsResolved))
            .then(
                if (loading) {
                    Modifier.shimmerEffect(shape)
                } else {
                    Modifier
                }
            )
            .indication(
                interactionSource = interactionSource,
                indication = rememberRipple()
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        
        // Invisible clickable box
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
        ) {
            // Handle click through Surface
        }
    }
    
    // Actual clickable surface (invisible)
    Surface(
        onClick = onClick,
        modifier = modifier
            .scale(animatedScale)
            .graphicsLayer { alpha = animatedAlpha }
            .clip(shape),
        enabled = enabled && !loading,
        color = Color.Transparent,
        shape = shape,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColorsResolved))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

// ============================================================
// GLASS SURFACE - Frosted glass effect
// ============================================================

/**
 * A surface with glass-like frosted effect
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 20.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    content: @Composable BoxScope.() -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = SpringSpecs.gentle,
        label = "glassAlpha"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer { alpha = animatedAlpha }
            .clip(shape)
            .background(backgroundColor)
            .drawBehind {
                // Subtle border glow
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(width = 1.dp.toPx()),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
                )
            }
    ) {
        content()
    }
}

/**
 * A more advanced glass surface with gradient background
 */
@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val animatedElevation by animateDpAsState(
        targetValue = when {
            isPressed -> 2.dp
            isHovered -> 8.dp
            else -> 4.dp
        },
        animationSpec = SpringSpecs.gentleDp,
        label = "glassElevation"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = SpringSpecs.buttonPress,
        label = "glassScale"
    )
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gradientColors = LocalGradientColors.current
    
    Card(
        modifier = modifier.scale(animatedScale),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        onClick = onClick ?: {},
        interactionSource = if (onClick != null) interactionSource else remember { MutableInteractionSource() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            surfaceColor.copy(alpha = 0.95f),
                            surfaceColor.copy(alpha = 0.85f)
                        )
                    )
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
}

// ============================================================
// ANIMATED CARD - Card with elevation and scale animations
// ============================================================

/**
 * A card with animated elevation, scale, and smooth interactions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    baseElevation: Dp = 2.dp,
    hoverElevation: Dp = 8.dp,
    pressedElevation: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // Animated elevation
    val animatedElevation by animateDpAsState(
        targetValue = when {
            !enabled -> baseElevation
            isPressed -> pressedElevation
            isHovered -> hoverElevation
            else -> baseElevation
        },
        animationSpec = SpringSpecs.gentleDp,
        label = "cardElevation"
    )
    
    // Animated scale
    val animatedScale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> 0.97f
            isHovered -> 1.02f
            else -> 1f
        },
        animationSpec = SpringSpecs.card,
        label = "cardScale"
    )
    
    // Animated shadow opacity
    val shadowAlpha by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.15f
            isHovered -> 0.2f
            else -> 0.1f
        },
        animationSpec = TweenSpecs.fadeStandard,
        label = "shadowAlpha"
    )
    
    Card(
        onClick = onClick,
        modifier = modifier.scale(animatedScale),
        enabled = enabled,
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
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
// LOADING INDICATORS - Smooth animated loading states
// ============================================================

/**
 * A smooth pulsing loading indicator
 */
@Composable
fun PulsingLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp
) {
    val pulseScale = rememberBreathingAnimation(
        minValue = 0.8f,
        maxValue = 1.2f,
        durationMs = 800
    )
    
    val pulseAlpha = rememberPulseAnimation(
        initialValue = 0.4f,
        targetValue = 1f,
        durationMs = 800
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulse ring
        Box(
            modifier = Modifier
                .size(size * pulseScale)
                .graphicsLayer { alpha = 1f - pulseAlpha }
                .border(
                    width = 2.dp,
                    color = color.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )
        
        // Inner solid circle
        Box(
            modifier = Modifier
                .size(size * 0.6f)
                .graphicsLayer { alpha = pulseAlpha }
                .background(color, CircleShape)
        )
    }
}

/**
 * A dot-based loading indicator with staggered animation
 */
@Composable
fun DotsLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 8.dp,
    spacing: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val animatedScale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = index * 100, easing = EasingCurves.easeInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            
            val animatedAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = index * 100, easing = EasingCurves.easeInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotAlpha$index"
            )
            
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(animatedScale)
                    .graphicsLayer { alpha = animatedAlpha }
                    .background(color, CircleShape)
            )
        }
    }
}

/**
 * A skeleton loading placeholder with shimmer effect
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    highlightColor: Color = MaterialTheme.colorScheme.surface
) {
    val shimmerTranslation = rememberShimmerTranslation()
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(baseColor)
            .drawBehind {
                // Shimmer gradient
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            baseColor,
                            highlightColor,
                            baseColor
                        ),
                        start = Offset(shimmerTranslation, 0f),
                        end = Offset(shimmerTranslation + size.width, size.height)
                    )
                )
            }
    )
}

// ============================================================
// ANIMATED ICON - Icons with smooth state transitions
// ============================================================

/**
 * An animated icon that smoothly transitions between states
 */
@Composable
fun AnimatedStateIcon(
    isActive: Boolean,
    activeIcon: @Composable () -> Unit,
    inactiveIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = SpringSpecs.bouncy
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = animationSpec,
        label = "iconState"
    )
    
    Box(modifier = modifier) {
        // Inactive icon
        Box(
            modifier = Modifier.graphicsLayer { alpha = 1f - animatedProgress }
        ) {
            inactiveIcon()
        }
        
        // Active icon
        Box(
            modifier = Modifier.graphicsLayer { alpha = animatedProgress }
        ) {
            activeIcon()
        }
    }
}

/**
 * An icon with rotation animation
 */
@Composable
fun RotatingIcon(
    isRotating: Boolean,
    modifier: Modifier = Modifier,
    rotationDegrees: Float = 360f,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = rotationDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val staticRotation by animateFloatAsState(
        targetValue = 0f,
        animationSpec = TweenSpecs.fadeQuick,
        label = "staticRotation"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            rotationZ = if (isRotating) rotation else staticRotation
        }
    ) {
        content()
    }
}

// ============================================================
// SHIMMER EFFECT - Modifier extension
// ============================================================

/**
 * Modifier extension to add shimmer effect to any composable
 */
@Composable
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier {
    val shimmerTranslation = rememberShimmerTranslation()
    val shimmerAlpha = rememberPulseAnimation(0.3f, 0.6f, 1500)
    
    return this then Modifier
        .graphicsLayer { alpha = shimmerAlpha }
        .drawBehind {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    ),
                    start = Offset(shimmerTranslation, 0f),
                    end = Offset(shimmerTranslation + size.width, size.height)
                )
            )
        }
}

// ============================================================
// ANIMATED BADGE - Notification badge with animation
// ============================================================

/**
 * An animated notification badge
 */
@Composable
fun AnimatedBadge(
    count: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = MaterialTheme.colorScheme.onError
) {
    AnimatedVisibility(
        visible = count > 0,
        enter = scaleIn(animationSpec = SpringSpecs.bouncy) + fadeIn(),
        exit = scaleOut(animationSpec = SpringSpecs.snappy) + fadeOut()
    ) {
        val pulseScale = rememberBreathingAnimation(0.95f, 1.05f, 600)
        
        Surface(
            modifier = modifier.scale(pulseScale),
            shape = CircleShape,
            color = backgroundColor,
            contentColor = contentColor
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                color = contentColor
            )
        }
    }
}

// ============================================================
// ANIMATED PROGRESS INDICATOR
// ============================================================

/**
 * A smooth animated progress bar
 */
@Composable
fun AnimatedLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    cornerRadius: Dp = 4.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = 500,
            easing = EasingCurves.easeOut
        ),
        label = "progress"
    )
    
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(color, color.copy(alpha = 0.8f))
                    )
                )
        )
    }
}

// ============================================================
// RIPPLE EFFECT INDICATOR
// ============================================================

/**
 * A card with ripple effect indication
 */
@Composable
fun RippleCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rippleColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Surface(
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(content = content)
    }
}

// ============================================================
// PREVIEW COMPONENTS
// ============================================================

/**
 * A premium card for displaying preview content
 */
@Composable
fun PreviewCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    AnimatedCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
