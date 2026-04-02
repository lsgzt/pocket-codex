package com.pocketdev.app.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Premium Animation Utilities for Pocket-Codex
 * 
 * This file contains sophisticated animation specifications, easing curves,
 * and utility functions for creating premium, polished UI animations.
 * Inspired by Rosemoe sora-editor and Acode animations.
 */

// ============================================================
// ANIMATION CONSTANTS - Consistent timing across the app
// ============================================================

/**
 * Duration constants for different animation types
 */
object AnimationDuration {
    // Quick interactions (button presses, toggles)
    const val QUICK = 150
    
    // Standard transitions (cards, sheets)
    const val STANDARD = 300
    
    // Emphasized animations (hero elements, modals)
    const val EMPHASIZED = 500
    
    // Slow animations for dramatic effect
    const val SLOW = 800
    
    // Shimmer effect duration
    const val SHIMMER = 1500
    
    // Stagger delay for list items
    const val STAGGER_DELAY = 50
}

/**
 * Animation delay constants for staggered animations
 */
object AnimationDelay {
    const val STAGGER_SMALL = 30
    const val STAGGER_MEDIUM = 50
    const val STAGGER_LARGE = 80
    const val STAGGER_XLARGE = 120
}

// ============================================================
// SPRING ANIMATION SPECS - Natural, physics-based animations
// ============================================================

/**
 * Premium spring specifications for different use cases
 */
object SpringSpecs {
    
    // Float spring specs
    val smooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    val gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessVeryLow
    )
    
    val emphasized = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val card = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 400f
    )
    
    val buttonPress = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 600f
    )
    
    // Dp spring specs (for animateDpAsState)
    val smoothDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val gentleDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessVeryLow
    )
    
    val bouncyDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    // IntOffset spring specs (for animateIntOffsetAsState)
    val smoothIntOffset = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val bouncyIntOffset = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}

// ============================================================
// EASING CURVES - Custom cubic bezier easing functions
// ============================================================

/**
 * Premium easing curves for sophisticated animations
 */
object EasingCurves {
    
    /**
     * Standard ease out - decelerate at the end
     * Good for entering elements
     */
    val easeOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    
    /**
     * Standard ease in - accelerate at the start
     * Good for exiting elements
     */
    val easeIn = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    
    /**
     * Ease in out - slow start and end
     * Good for continuous animations
     */
    val easeInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    /**
     * Premium ease - custom curve for premium feel
     * Slightly more emphasized than standard
     */
    val premiumEase = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    /**
     * Emphasized ease - more dramatic deceleration
     * Good for hero elements
     */
    val emphasizedEase = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    
    /**
     * Anticipate ease - slight pull back before moving
     * Good for elements that need emphasis
     */
    val anticipateEase = CubicBezierEasing(0.36f, 0.0f, 0.66f, -0.56f)
    
    /**
     * Overshoot ease - go past target then settle
     * Good for attention-grabbing animations
     */
    val overshootEase = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
}

// ============================================================
// TWEEN ANIMATION SPECS - Time-based animations with easing
// ============================================================

/**
 * Pre-configured tween specifications for common animations
 */
object TweenSpecs {
    
    /**
     * Quick fade animation
     */
    val fadeQuick = tween<Float>(
        durationMillis = AnimationDuration.QUICK,
        easing = EasingCurves.easeOut
    )
    
    /**
     * Standard fade animation
     */
    val fadeStandard = tween<Float>(
        durationMillis = AnimationDuration.STANDARD,
        easing = EasingCurves.easeOut
    )
    
    /**
     * Scale animation with premium ease
     */
    val scaleIn = tween<Float>(
        durationMillis = AnimationDuration.STANDARD,
        easing = EasingCurves.premiumEase
    )
    
    /**
     * Scale out animation
     */
    val scaleOut = tween<Float>(
        durationMillis = AnimationDuration.STANDARD,
        easing = EasingCurves.easeIn
    )
    
    /**
     * Slide animation
     */
    val slide = tween<IntOffset>(
        durationMillis = AnimationDuration.STANDARD,
        easing = EasingCurves.easeOut
    )
    
    /**
     * Quick slide for snappy interactions
     */
    val slideQuick = tween<IntOffset>(
        durationMillis = AnimationDuration.QUICK,
        easing = EasingCurves.easeOut
    )
    
    /**
     * Shimmer animation for loading states
     */
    val shimmer = tween<Float>(
        durationMillis = AnimationDuration.SHIMMER,
        easing = LinearEasing
    )
    
    /**
     * Color transition animation
     */
    val colorTransition = tween<Float>(
        durationMillis = AnimationDuration.STANDARD,
        easing = EasingCurves.easeInOut
    )
}

// ============================================================
// ENTER/EXIT TRANSITIONS - Reusable transition configurations
// ============================================================

/**
 * Pre-configured enter transitions
 */
object EnterTransitions {
    
    /**
     * Fade in animation
     */
    val fadeIn: EnterTransition = fadeIn(
        animationSpec = TweenSpecs.fadeStandard
    )
    
    /**
     * Scale in from center
     */
    val scaleIn: EnterTransition = scaleIn(
        animationSpec = TweenSpecs.scaleIn,
        initialScale = 0.9f
    )
    
    /**
     * Slide in from bottom (common for cards)
     */
    val slideInFromBottom: EnterTransition = slideInVertically(
        animationSpec = TweenSpecs.slide,
        initialOffsetY = { it / 4 }
    )
    
    /**
     * Slide in from top
     */
    val slideInFromTop: EnterTransition = slideInVertically(
        animationSpec = TweenSpecs.slide,
        initialOffsetY = { -it / 4 }
    )
    
    /**
     * Slide in from left
     */
    val slideInFromLeft: EnterTransition = slideInHorizontally(
        animationSpec = TweenSpecs.slide,
        initialOffsetX = { -it / 4 }
    )
    
    /**
     * Slide in from right
     */
    val slideInFromRight: EnterTransition = slideInHorizontally(
        animationSpec = TweenSpecs.slide,
        initialOffsetX = { it / 4 }
    )
    
    /**
     * Expand in from center (combined scale + fade)
     */
    val expandIn: EnterTransition = fadeIn(
        animationSpec = TweenSpecs.fadeStandard
    ) + scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        initialScale = 0.8f
    )
    
    /**
     * Premium card enter - slide up with fade
     */
    val cardEnter: EnterTransition = fadeIn(
        animationSpec = tween(AnimationDuration.STANDARD, easing = EasingCurves.easeOut)
    ) + slideInVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        initialOffsetY = { 40 }
    )
    
    /**
     * Combined fade + scale for modals
     */
    val modalEnter: EnterTransition = fadeIn(
        animationSpec = tween(AnimationDuration.EMPHASIZED, easing = EasingCurves.easeOut)
    ) + scaleIn(
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        initialScale = 0.85f
    )
}

/**
 * Pre-configured exit transitions
 */
object ExitTransitions {
    
    /**
     * Fade out animation
     */
    val fadeOut: ExitTransition = fadeOut(
        animationSpec = TweenSpecs.fadeQuick
    )
    
    /**
     * Scale out to center
     */
    val scaleOut: ExitTransition = scaleOut(
        animationSpec = TweenSpecs.scaleOut,
        targetScale = 0.9f
    )
    
    /**
     * Slide out to bottom
     */
    val slideOutToBottom: ExitTransition = slideOutVertically(
        animationSpec = TweenSpecs.slideQuick,
        targetOffsetY = { it / 4 }
    )
    
    /**
     * Slide out to top
     */
    val slideOutToTop: ExitTransition = slideOutVertically(
        animationSpec = TweenSpecs.slideQuick,
        targetOffsetY = { -it / 4 }
    )
    
    /**
     * Shrink out (combined scale + fade)
     */
    val shrinkOut: ExitTransition = fadeOut(
        animationSpec = tween(AnimationDuration.QUICK)
    ) + scaleOut(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        targetScale = 0.8f
    )
    
    /**
     * Premium card exit - slide down with fade
     */
    val cardExit: ExitTransition = fadeOut(
        animationSpec = tween(AnimationDuration.QUICK, easing = EasingCurves.easeIn)
    ) + slideOutVertically(
        animationSpec = tween(AnimationDuration.QUICK, easing = EasingCurves.easeIn),
        targetOffsetY = { 20 }
    )
    
    /**
     * Combined fade + scale for modals
     */
    val modalExit: ExitTransition = fadeOut(
        animationSpec = tween(AnimationDuration.STANDARD, easing = EasingCurves.easeIn)
    ) + scaleOut(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 400f
        ),
        targetScale = 0.9f
    )
}

// ============================================================
// COMBINED TRANSITIONS - Ready-to-use enter/exit pairs
// ============================================================

/**
 * Pre-configured transition pairs for AnimatedVisibility
 */
object Transitions {
    
    /**
     * Simple fade in/out
     */
    val fade = fadeIn(
        animationSpec = TweenSpecs.fadeStandard
    ) togetherWith fadeOut(
        animationSpec = TweenSpecs.fadeQuick
    )
    
    /**
     * Scale in/out transition
     */
    val scale = scaleIn(
        animationSpec = TweenSpecs.scaleIn,
        initialScale = 0.8f
    ) togetherWith scaleOut(
        animationSpec = TweenSpecs.scaleOut,
        targetScale = 0.8f
    )
    
    /**
     * Slide up and fade (common for cards and sheets)
     */
    val slideUpWithFade = EnterTransitions.cardEnter togetherWith ExitTransitions.cardExit
    
    /**
     * Premium modal transition
     */
    val modal = EnterTransitions.modalEnter togetherWith ExitTransitions.modalExit
    
    /**
     * Expand/shrink transition
     */
    val expandShrink = EnterTransitions.expandIn togetherWith ExitTransitions.shrinkOut
    
    /**
     * Slide from right to left (for navigation)
     */
    val slideRightToLeft = slideInHorizontally(
        animationSpec = TweenSpecs.slide,
        initialOffsetX = { it }
    ) togetherWith slideOutHorizontally(
        animationSpec = TweenSpecs.slideQuick,
        targetOffsetX = { -it }
    )
    
    /**
     * Slide from left to right (for back navigation)
     */
    val slideLeftToRight = slideInHorizontally(
        animationSpec = TweenSpecs.slide,
        initialOffsetX = { -it }
    ) togetherWith slideOutHorizontally(
        animationSpec = TweenSpecs.slideQuick,
        targetOffsetX = { it }
    )
}

// ============================================================
// STAGGERED ANIMATION UTILITIES
// ============================================================

/**
 * Calculate staggered animation delay for list items
 */
@Composable
fun staggeredAnimationDelay(
    index: Int,
    baseDelay: Int = AnimationDelay.STAGGER_MEDIUM
): Int = remember(index, baseDelay) { index * baseDelay }

/**
 * Create a staggered enter transition for list items
 */
@Composable
fun staggeredEnterTransition(index: Int): EnterTransition {
    val delay = staggeredAnimationDelay(index)
    return fadeIn(
        animationSpec = tween(
            durationMillis = AnimationDuration.STANDARD,
            delayMillis = delay,
            easing = EasingCurves.easeOut
        )
    ) + slideInVertically(
        animationSpec = tween(
            durationMillis = AnimationDuration.STANDARD,
            delayMillis = delay,
            easing = EasingCurves.easeOut
        ),
        initialOffsetY = { 40 }
    )
}

/**
 * Create a staggered exit transition for list items
 */
@Composable
fun staggeredExitTransition(index: Int): ExitTransition {
    val delay = staggeredAnimationDelay(index, AnimationDelay.STAGGER_SMALL)
    return fadeOut(
        animationSpec = tween(
            durationMillis = AnimationDuration.QUICK,
            delayMillis = delay,
            easing = EasingCurves.easeIn
        )
    ) + slideOutVertically(
        animationSpec = tween(
            durationMillis = AnimationDuration.QUICK,
            delayMillis = delay,
            easing = EasingCurves.easeIn
        ),
        targetOffsetY = { 20 }
    )
}

// ============================================================
// INTERACTION STATE ANIMATIONS
// ============================================================

/**
 * Press scale modifier for interactive elements
 */
@Composable
fun Modifier.pressScale(
    pressed: Boolean,
    scale: Float = 0.96f
): Modifier = this then Modifier
    .scale(
        animateFloatAsState(
            targetValue = if (pressed) scale else 1f,
            animationSpec = SpringSpecs.buttonPress,
            label = "pressScale"
        ).value
    )

/**
 * Hover elevation modifier for cards
 */
@Composable
fun Modifier.hoverElevation(
    isHovered: Boolean,
    baseElevation: Float = 2f,
    hoverElevation: Float = 8f
): Modifier {
    val elevation by animateDpAsState(
        targetValue = if (isHovered) hoverElevation.dp else baseElevation.dp,
        animationSpec = SpringSpecs.gentleDp,
        label = "hoverElevation"
    )
    return this
}

// ============================================================
// INFINITE TRANSITION UTILITIES
// ============================================================

/**
 * Create a pulsing animation for loading states
 */
@Composable
fun rememberPulseAnimation(
    initialValue: Float = 0.4f,
    targetValue: Float = 1f,
    durationMs: Int = AnimationDuration.STANDARD * 2
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return infiniteTransition.animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EasingCurves.easeInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    ).value
}

/**
 * Create a shimmer translation animation
 */
@Composable
fun rememberShimmerTranslation(
    durationMs: Int = AnimationDuration.SHIMMER
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    return infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    ).value
}

/**
 * Create a breathing animation for subtle emphasis
 */
@Composable
fun rememberBreathingAnimation(
    minValue: Float = 0.95f,
    maxValue: Float = 1.05f,
    durationMs: Int = AnimationDuration.SLOW
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    return infiniteTransition.animateFloat(
        initialValue = minValue,
        targetValue = maxValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EasingCurves.easeInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    ).value
}

// ============================================================
// SHARED ELEMENT TRANSITION CONFIGURATIONS
// ============================================================

/**
 * Configuration for shared element transitions
 */
object SharedTransitionConfig {
    
    /**
     * Default shared element bounds animation
     */
    val boundsAnimationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    /**
     * Default shared element fade animation
     */
    val fadeAnimationSpec = tween<Float>(
        durationMillis = AnimationDuration.STANDARD,
        easing = EasingCurves.easeInOut
    )
    
    /**
     * Card to detail shared transition
     */
    val cardToDetail = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 380f
    )
}

// ============================================================
// EXTENSION FUNCTIONS
// ============================================================

/**
 * Apply a smooth press effect to any modifier
 */
@Composable
fun Modifier.animatePressed(
    pressed: Boolean,
    scale: Float = 0.95f
): Modifier {
    val animatedScale by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = SpringSpecs.buttonPress,
        label = "pressedScale"
    )
    return this.scale(animatedScale)
}

/**
 * Apply a smooth hover effect to any modifier
 */
@Composable
fun Modifier.animateHover(
    isHovered: Boolean,
    scale: Float = 1.02f
): Modifier {
    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered) scale else 1f,
        animationSpec = SpringSpecs.gentle,
        label = "hoverScale"
    )
    return this.scale(animatedScale)
}

/**
 * Apply a bounce animation for attention
 */
@Composable
fun rememberBounceAnimation(
    trigger: Boolean,
    bounceHeight: Int = 10
): IntOffset {
    val offset by animateIntOffsetAsState(
        targetValue = if (trigger) IntOffset(0, -bounceHeight) else IntOffset.Zero,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "bounceOffset"
    )
    return offset
}
