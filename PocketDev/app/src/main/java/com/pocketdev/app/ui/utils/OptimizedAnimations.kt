package com.pocketdev.app.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Optimized Animation Specifications for All Device Tiers
 * 
 * Provides performance-aware animations that automatically adjust
 * complexity based on device capabilities.
 * 
 * Key optimizations for low-end devices:
 * - Uses tween instead of spring (no physics calculations)
 * - Shorter durations
 * - Simpler easing curves
 * - No overshoot/bounce effects
 */
object OptimizedAnimations {

    // ============================================================
    // DURATION CONSTANTS - Tier-aware
    // ============================================================

    object Duration {
        const val INSTANT = 50      // Near-instant feedback
        const val QUICK = 100       // Quick interactions
        const val STANDARD = 200    // Standard transitions
        const val SLOW = 300        // Slower, deliberate animations
        
        // Multipliers for different tiers
        fun forTier(base: Int, tier: DevicePerformance.Tier): Int {
            return when (tier) {
                DevicePerformance.Tier.LOW -> (base * 0.5f).toInt().coerceAtLeast(50)
                DevicePerformance.Tier.MEDIUM -> (base * 0.75f).toInt()
                DevicePerformance.Tier.HIGH -> base
            }
        }
    }

    // ============================================================
    // EASING CURVES - Simple and efficient
    // ============================================================

    object Easing {
        // Standard ease-out (most efficient)
        val standard = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
        
        // Quick ease-out
        val quick = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
        
        // Linear (most efficient)
        val linear = LinearEasing
    }

    // ============================================================
    // PERFORMANCE-AWARE ANIMATION SPECS
    // ============================================================

    /**
     * Get optimal fade animation spec
     */
    @Composable
    fun rememberFadeSpec(tier: DevicePerformance.Tier = rememberPerformanceTier()): FiniteAnimationSpec<Float> {
        val duration = Duration.forTier(Duration.STANDARD, tier)
        return remember(duration) {
            tween<Float>(
                durationMillis = duration,
                easing = Easing.standard
            )
        }
    }

    /**
     * Get optimal scale animation spec
     */
    @Composable
    fun rememberScaleSpec(tier: DevicePerformance.Tier = rememberPerformanceTier()): FiniteAnimationSpec<Float> {
        val duration = Duration.forTier(Duration.QUICK, tier)
        return remember(duration) {
            tween<Float>(
                durationMillis = duration,
                easing = Easing.quick
            )
        }
    }

    /**
     * Get optimal slide animation spec for IntOffset
     */
    @Composable
    fun rememberSlideSpec(tier: DevicePerformance.Tier = rememberPerformanceTier()): FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset> {
        val duration = Duration.forTier(Duration.STANDARD, tier)
        return remember(duration) {
            tween(
                durationMillis = duration,
                easing = Easing.standard
            )
        }
    }

    /**
     * Get optimal Dp animation spec
     */
    @Composable
    fun rememberDpSpec(tier: DevicePerformance.Tier = rememberPerformanceTier()): FiniteAnimationSpec<androidx.compose.ui.unit.Dp> {
        val duration = Duration.forTier(Duration.STANDARD, tier)
        return remember(duration) {
            tween(
                durationMillis = duration,
                easing = Easing.standard
            )
        }
    }

    // ============================================================
    // PRE-BUILT OPTIMIZED SPECS
    // ============================================================

    /**
     * Optimized button press spec
     */
    @Composable
    fun rememberButtonPressSpec(tier: DevicePerformance.Tier = rememberPerformanceTier()): FiniteAnimationSpec<Float> {
        val duration = Duration.forTier(Duration.QUICK, tier)
        return remember(duration) {
            tween<Float>(
                durationMillis = duration,
                easing = Easing.quick
            )
        }
    }

    /**
     * Optimized card elevation spec
     */
    @Composable
    fun rememberElevationSpec(tier: DevicePerformance.Tier = rememberPerformanceTier()): FiniteAnimationSpec<androidx.compose.ui.unit.Dp> {
        val duration = Duration.forTier(Duration.STANDARD, tier)
        return remember(duration) {
            tween(
                durationMillis = duration,
                easing = Easing.standard
            )
        }
    }

    // ============================================================
    // SIMPLIFIED ANIMATION HELPERS
    // ============================================================

    /**
     * Simple press scale effect - optimized for all devices
     */
    @Composable
    fun rememberPressScale(isPressed: Boolean, tier: DevicePerformance.Tier = rememberPerformanceTier()): Float {
        val spec = rememberButtonPressSpec(tier)
        return animateFloatAsState(
            targetValue = if (isPressed) 0.97f else 1f,
            animationSpec = spec,
            label = "pressScale"
        ).value
    }

    /**
     * Simple hover scale effect - optimized for all devices
     */
    @Composable
    fun rememberHoverScale(isHovered: Boolean, tier: DevicePerformance.Tier = rememberPerformanceTier()): Float {
        // Skip hover animations on low-end devices
        if (tier == DevicePerformance.Tier.LOW) return 1f
        
        val spec = rememberScaleSpec(tier)
        return animateFloatAsState(
            targetValue = if (isHovered) 1.02f else 1f,
            animationSpec = spec,
            label = "hoverScale"
        ).value
    }

    /**
     * Simple fade-in alpha - optimized for all devices
     */
    @Composable
    fun rememberFadeAlpha(visible: Boolean, tier: DevicePerformance.Tier = rememberPerformanceTier()): Float {
        val spec = rememberFadeSpec(tier)
        return animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = spec,
            label = "fadeAlpha"
        ).value
    }
}

// ============================================================
// EXTENSION FUNCTIONS FOR EASY USE
// ============================================================

/**
 * Get performance-aware animation duration
 */
@Composable
fun Int.optimizedDuration(): Int {
    val tier = rememberPerformanceTier()
    return OptimizedAnimations.Duration.forTier(this, tier)
}
