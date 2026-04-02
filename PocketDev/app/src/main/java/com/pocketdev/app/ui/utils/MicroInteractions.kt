package com.pocketdev.app.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Premium Micro-Interactions for Pocket-Codex
 * 
 * This file contains sophisticated micro-interaction patterns inspired by
 * premium apps like Acode, Material Design 3, and iOS animations.
 * 
 * Features:
 * - Haptic-like visual feedback
 * - Touch ripple effects
 * - Press animations
 * - Hover states
 * - Focus indicators
 */

// ============================================================
// PRESS EFFECTS - Visual feedback on touch
// ============================================================

/**
 * Apply a press scale effect to any composable
 * Scales down slightly when pressed for tactile feedback
 */
@Composable
fun Modifier.pressScaleEffect(
    pressed: Boolean,
    scale: Float = 0.96f,
    animationSpec: AnimationSpec<Float> = SpringSpecs.buttonPress
): Modifier {
    val animatedScale by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = animationSpec,
        label = "pressScale"
    )
    return this.scale(animatedScale)
}

/**
 * Apply a press alpha effect - dims slightly when pressed
 */
@Composable
fun Modifier.pressAlphaEffect(
    pressed: Boolean,
    alpha: Float = 0.85f,
    animationSpec: AnimationSpec<Float> = TweenSpecs.fadeQuick
): Modifier {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (pressed) alpha else 1f,
        animationSpec = animationSpec,
        label = "pressAlpha"
    )
    return this.alpha(animatedAlpha)
}

/**
 * Apply a combined press effect (scale + alpha)
 */
@Composable
fun Modifier.pressEffect(
    pressed: Boolean,
    scale: Float = 0.97f,
    alpha: Float = 0.9f
): Modifier {
    val animatedScale by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = SpringSpecs.buttonPress,
        label = "pressScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (pressed) alpha else 1f,
        animationSpec = TweenSpecs.fadeQuick,
        label = "pressAlpha"
    )
    return this
        .scale(animatedScale)
        .alpha(animatedAlpha)
}

// ============================================================
// HOVER EFFECTS - Visual feedback on hover
// ============================================================

/**
 * Apply a hover scale effect - grows slightly when hovered
 */
@Composable
fun Modifier.hoverScaleEffect(
    hovered: Boolean,
    scale: Float = 1.02f,
    animationSpec: AnimationSpec<Float> = SpringSpecs.gentle
): Modifier {
    val animatedScale by animateFloatAsState(
        targetValue = if (hovered) scale else 1f,
        animationSpec = animationSpec,
        label = "hoverScale"
    )
    return this.scale(animatedScale)
}

/**
 * Apply a hover glow effect - adds a subtle glow on hover
 */
@Composable
fun Modifier.hoverGlowEffect(
    hovered: Boolean,
    glowRadius: Float = 8f,
    animationSpec: AnimationSpec<Float> = SpringSpecs.gentle
): Modifier {
    val animatedGlow by animateFloatAsState(
        targetValue = if (hovered) glowRadius else 0f,
        animationSpec = animationSpec,
        label = "hoverGlow"
    )
    return this.graphicsLayer {
        // Note: shadowElevation in graphicsLayer creates ambient shadow
        shadowElevation = animatedGlow
    }
}

// ============================================================
// INTERACTION SOURCE BINDINGS - Auto-track press/hover states
// ============================================================

/**
 * Automatically bind to an InteractionSource for press effects
 */
@Composable
fun Modifier.bindPressInteraction(
    interactionSource: InteractionSource,
    scale: Float = 0.97f,
    alpha: Float = 0.9f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    return pressEffect(isPressed, scale, alpha)
}

/**
 * Automatically bind to an InteractionSource for hover effects
 */
@Composable
fun Modifier.bindHoverInteraction(
    interactionSource: InteractionSource,
    scale: Float = 1.02f
): Modifier {
    val isHovered by interactionSource.collectIsHoveredAsState()
    return hoverScaleEffect(isHovered, scale)
}

/**
 * Automatically bind to an InteractionSource for both press and hover
 */
@Composable
fun Modifier.bindInteractionEffects(
    interactionSource: InteractionSource,
    pressScale: Float = 0.97f,
    pressAlpha: Float = 0.9f,
    hoverScale: Float = 1.02f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val animatedScale by animateFloatAsState(
        targetValue = when {
            isPressed -> pressScale
            isHovered -> hoverScale
            else -> 1f
        },
        animationSpec = SpringSpecs.buttonPress,
        label = "interactionScale"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed) pressAlpha else 1f,
        animationSpec = TweenSpecs.fadeQuick,
        label = "interactionAlpha"
    )
    
    return this
        .scale(animatedScale)
        .alpha(animatedAlpha)
}

// ============================================================
// BOUNCE EFFECTS - Playful attention-grabbing animations
// ============================================================

/**
 * Apply a bounce effect on trigger
 */
@Composable
fun Modifier.bounceOnTrigger(
    trigger: Boolean,
    bounceHeight: Int = 8,
    animationSpec: AnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessHigh
    )
): Modifier {
    val offset by animateIntOffsetAsState(
        targetValue = if (trigger) IntOffset(0, -bounceHeight) else IntOffset.Zero,
        animationSpec = animationSpec,
        label = "bounceOffset"
    )
    return this.offset { offset }
}

/**
 * Apply a scale bounce on trigger
 */
@Composable
fun Modifier.scaleBounceOnTrigger(
    trigger: Boolean,
    bounceScale: Float = 1.1f,
    animationSpec: AnimationSpec<Float> = SpringSpecs.bouncy
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (trigger) bounceScale else 1f,
        animationSpec = animationSpec,
        label = "scaleBounce"
    )
    return this.scale(scale)
}

// ============================================================
// SHAKE EFFECT - Error/warning feedback
// ============================================================

/**
 * Apply a shake effect for error feedback
 */
@Composable
fun rememberShakeAnimation(
    trigger: Boolean,
    shakeCount: Int = 3,
    shakeIntensity: Int = 8
): IntOffset {
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -shakeIntensity.toFloat(),
        targetValue = shakeIntensity.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 50,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeOffset"
    )
    
    return if (trigger) {
        IntOffset(shakeOffset.roundToInt(), 0)
    } else {
        IntOffset.Zero
    }
}

/**
 * Modifier extension for shake effect
 */
@Composable
fun Modifier.shakeOnTrigger(
    trigger: Boolean,
    shakeIntensity: Int = 8
): Modifier {
    val offset = rememberShakeAnimation(trigger, shakeIntensity = shakeIntensity)
    return this.offset { offset }
}

// ============================================================
// PULSE EFFECT - Attention-grabbing pulse
// ============================================================

/**
 * Apply a continuous pulse effect
 */
@Composable
fun Modifier.pulseEffect(
    pulseScale: Float = 1.05f,
    durationMs: Int = 1000
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = pulseScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EasingCurves.easeInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    return this.scale(scale)
}

/**
 * Apply a breathing pulse effect (subtle)
 */
@Composable
fun Modifier.breathingEffect(
    minScale: Float = 0.98f,
    maxScale: Float = 1.02f,
    durationMs: Int = 2000
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EasingCurves.easeInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    
    return this.scale(scale)
}

// ============================================================
// ATTENTION SEEKERS - Draw attention to elements
// ============================================================

/**
 * Apply a glow pulse effect
 */
@Composable
fun Modifier.glowPulseEffect(
    maxGlow: Float = 16f,
    durationMs: Int = 1500
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
    
    val glow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = maxGlow,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EasingCurves.easeInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowValue"
    )
    
    return this.graphicsLayer {
        shadowElevation = glow
    }
}

// ============================================================
// TRANSITION HELPERS - Smooth state transitions
// ============================================================

/**
 * Create a smooth transition between states
 */
@Composable
fun <T> smoothTransition(
    targetState: T,
    animationSpec: AnimationSpec<Float> = SpringSpecs.smooth,
    transition: (progress: Float, from: T, to: T) -> Unit
): Float {
    var previousState by remember { mutableStateOf(targetState) }
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = animationSpec,
        label = "transitionProgress"
    )
    
    LaunchedEffect(targetState) {
        if (targetState != previousState) {
            previousState = targetState
        }
    }
    
    return progress
}

// ============================================================
// GESTURE FEEDBACK - Haptic-like visual feedback
// ============================================================

/**
 * Apply gesture feedback effect
 */
@Composable
fun Modifier.gestureFeedback(
    pressed: Boolean,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    
    val feedbackScale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.95f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "feedbackScale"
    )
    
    return this
        .scale(feedbackScale)
        .graphicsLayer {
            // Enable GPU layer for smooth animation
        }
}
