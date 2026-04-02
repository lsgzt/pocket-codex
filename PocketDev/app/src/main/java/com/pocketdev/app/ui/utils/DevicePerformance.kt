package com.pocketdev.app.ui.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp

/**
 * Device Performance Detection and Optimization Utility
 * 
 * Detects device capabilities and provides performance tier classification
 * to enable adaptive animations and effects for smooth experience on all devices.
 */
object DevicePerformance {

    /**
     * Performance tiers based on device capabilities
     */
    enum class Tier {
        LOW,      // Low-end devices (e.g., Oppo A15s, <4GB RAM, low-end CPU)
        MEDIUM,   // Mid-range devices (4-6GB RAM, mid-range CPU)
        HIGH      // High-end devices (6GB+ RAM, flagship CPU)
    }

    /**
     * Animation complexity levels based on performance tier
     */
    enum class AnimationComplexity {
        NONE,       // No animations (lowest tier)
        MINIMAL,    // Simple fade/scale only
        STANDARD,   // Standard animations without physics
        FULL        // Full animations with spring physics
    }

    /**
     * Get device performance tier
     */
    fun getPerformanceTier(context: Context): Tier {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        // Check memory
        val totalMem = activityManager.memoryClass

        // Check CPU cores
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // Check if device is low-end based on multiple factors
        val isLowEnd = when {
            // Very low memory (< 3GB)
            totalMem <= 160 -> true
            // Low memory + few cores
            totalMem <= 256 && cpuCores <= 4 -> true
            // Check if system flags device as low-end
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                activityManager.isLowRamDevice
            }
            else -> false
        }

        return when {
            isLowEnd || totalMem <= 160 -> Tier.LOW
            totalMem <= 256 -> Tier.MEDIUM
            else -> Tier.HIGH
        }
    }

    /**
     * Get animation complexity based on performance tier
     */
    fun getAnimationComplexity(tier: Tier): AnimationComplexity {
        return when (tier) {
            Tier.LOW -> AnimationComplexity.MINIMAL
            Tier.MEDIUM -> AnimationComplexity.STANDARD
            Tier.HIGH -> AnimationComplexity.FULL
        }
    }

    /**
     * Get optimal animation duration multiplier (lower = faster on low-end)
     */
    fun getAnimationDurationMultiplier(tier: Tier): Float {
        return when (tier) {
            Tier.LOW -> 0.5f      // Faster animations
            Tier.MEDIUM -> 0.75f  // Slightly faster
            Tier.HIGH -> 1.0f     // Full duration
        }
    }

    /**
     * Check if complex effects (blur, shadows) should be enabled
     */
    fun shouldEnableComplexEffects(tier: Tier): Boolean {
        return tier == Tier.HIGH
    }

    /**
     * Get optimal list prefetch count
     */
    fun getListPrefetchCount(tier: Tier): Int {
        return when (tier) {
            Tier.LOW -> 2
            Tier.MEDIUM -> 4
            Tier.HIGH -> 6
        }
    }
}

/**
 * Composable to remember performance tier
 */
@Composable
fun rememberPerformanceTier(): DevicePerformance.Tier {
    val context = LocalContext.current
    return remember { DevicePerformance.getPerformanceTier(context) }
}

/**
 * Composable to remember animation complexity
 */
@Composable
fun rememberAnimationComplexity(): DevicePerformance.AnimationComplexity {
    val tier = rememberPerformanceTier()
    return remember(tier) { DevicePerformance.getAnimationComplexity(tier) }
}

/**
 * Get elevation animation spec for current tier
 */
@Composable
fun rememberElevationSpec(tier: DevicePerformance.Tier = rememberPerformanceTier()): FiniteAnimationSpec<Dp> {
    val multiplier = DevicePerformance.getAnimationDurationMultiplier(tier)
    return remember(multiplier) {
        tween(
            durationMillis = (200 * multiplier).toInt(),
            easing = FastOutSlowInEasing
        )
    }
}

/**
 * Performance-aware animation duration
 */
@Composable
fun rememberOptimalDuration(baseDuration: Int): Int {
    val tier = rememberPerformanceTier()
    val multiplier = DevicePerformance.getAnimationDurationMultiplier(tier)
    return remember(baseDuration, multiplier) { (baseDuration * multiplier).toInt() }
}

/**
 * Check if complex effects should be shown
 */
@Composable
fun rememberEnableComplexEffects(): Boolean {
    val tier = rememberPerformanceTier()
    return remember(tier) { DevicePerformance.shouldEnableComplexEffects(tier) }
}
