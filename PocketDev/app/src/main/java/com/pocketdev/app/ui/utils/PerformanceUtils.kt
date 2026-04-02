package com.pocketdev.app.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Performance Optimization Utilities for Pocket-Codex
 * 
 * This file contains performance optimization techniques inspired by
 * Acode, Sora-editor, and Jetpack Compose best practices.
 * 
 * Key optimizations:
 * - Stable classes to minimize recomposition
 * - GPU layer caching for smooth animations
 * - Derived state for efficient state computations
 * - Memoization utilities
 */

// ============================================================
// STABLE WRAPPERS - Prevent unnecessary recomposition
// ============================================================

/**
 * A stable wrapper for lambda functions to prevent recomposition
 * when the lambda reference changes but the implementation is the same.
 */
@Stable
class StableLambda<T>(private val block: () -> T) {
    operator fun invoke(): T = block()
    
    override fun equals(other: Any?): Boolean {
        return other is StableLambda<*>
    }
    
    override fun hashCode(): Int = javaClass.hashCode()
}

/**
 * A stable wrapper for functions with one parameter
 */
@Stable
class StableFunction1<T, R>(private val block: (T) -> R) {
    operator fun invoke(param: T): R = block(param)
    
    override fun equals(other: Any?): Boolean = other is StableFunction1<*, *>
    override fun hashCode(): Int = javaClass.hashCode()
}

/**
 * A stable wrapper for functions with two parameters
 */
@Stable
class StableFunction2<T1, T2, R>(private val block: (T1, T2) -> R) {
    operator fun invoke(param1: T1, param2: T2): R = block(param1, param2)
    
    override fun equals(other: Any?): Boolean = other is StableFunction2<*, *, *>
    override fun hashCode(): Int = javaClass.hashCode()
}

// ============================================================
// COMPOSABLE HELPERS - Memoization and optimization
// ============================================================

/**
 * Remember a stable lambda to prevent recomposition
 */
@Composable
fun <T> rememberStableLambda(block: () -> T): StableLambda<T> {
    return remember { StableLambda(block) }
}

/**
 * Remember a stable function with one parameter
 */
@Composable
fun <T, R> rememberStableFunction(block: (T) -> R): StableFunction1<T, R> {
    return remember { StableFunction1(block) }
}

/**
 * Remember a stable function with two parameters
 */
@Composable
fun <T1, T2, R> rememberStableFunction(block: (T1, T2) -> R): StableFunction2<T1, T2, R> {
    return remember { StableFunction2(block) }
}

// ============================================================
// GPU LAYER OPTIMIZATION
// ============================================================

/**
 * Apply GPU layer optimization for smooth animations
 * This renders the composable on a separate GPU layer,
 * preventing expensive recomposition during animations.
 */
fun Modifier.gpuLayer(): Modifier = this.graphicsLayer {
    // Enable hardware layer for smooth animations
    // This caches the layer on the GPU
}

/**
 * Apply GPU layer with clipping optimization
 */
fun Modifier.gpuLayerWithClip(clip: Boolean = true): Modifier = this.graphicsLayer {
    this.clip = clip
}

/**
 * Apply GPU layer with alpha for fade animations
 */
@Composable
fun Modifier.gpuLayerWithAlpha(alpha: Float): Modifier = this.graphicsLayer {
    this.alpha = alpha
}

/**
 * Apply GPU layer with scale for zoom animations
 */
@Composable
fun Modifier.gpuLayerWithScale(scale: Float): Modifier = this.graphicsLayer {
    this.scaleX = scale
    this.scaleY = scale
}

// ============================================================
// DEBOUNCE AND THROTTLE UTILITIES
// ============================================================

/**
 * A stable holder for debounced values
 */
@Stable
class DebouncedValue<T>(private val value: T, private val timestamp: Long) {
    fun get(): T = value
    fun getTimestamp(): Long = timestamp
}

/**
 * A stable holder for throttled operations
 */
@Stable
class ThrottledOperation<T>(
    private val operation: () -> T,
    private val lastExecutionTime: Long,
    private val minInterval: Long
) {
    fun canExecute(): Boolean = System.currentTimeMillis() - lastExecutionTime >= minInterval
    fun execute(): T = operation()
}

// ============================================================
// STATE REDUCTION HELPERS
// ============================================================

/**
 * Reduce state reads by creating a snapshot of multiple state values
 */
@Stable
data class CompositeState<T1, T2>(
    val first: T1,
    val second: T2
)

/**
 * Create a stable composite state from two state values
 */
@Composable
fun <T1, T2> rememberCompositeState(
    first: T1,
    second: T2
): CompositeState<T1, T2> = remember(first, second) {
    CompositeState(first, second)
}

/**
 * A stable holder for triple state values
 */
@Stable
data class TripleState<T1, T2, T3>(
    val first: T1,
    val second: T2,
    val third: T3
)

/**
 * Create a stable triple state
 */
@Composable
fun <T1, T2, T3> rememberTripleState(
    first: T1,
    second: T2,
    third: T3
): TripleState<T1, T2, T3> = remember(first, second, third) {
    TripleState(first, second, third)
}

// ============================================================
// LIST OPTIMIZATION HELPERS
// ============================================================

/**
 * A stable wrapper for list items to optimize LazyColumn performance
 */
@Stable
data class StableListItem<T>(
    val id: Long,
    val data: T,
    val index: Int
)

/**
 * Create stable list items for LazyColumn optimization
 */
@Composable
fun <T> List<T>.toStableItems(keySelector: (T) -> Long): List<StableListItem<T>> {
    return remember(this) {
        this.mapIndexed { index, item ->
            StableListItem(
                id = keySelector(item),
                data = item,
                index = index
            )
        }
    }
}

// ============================================================
// STRING OPTIMIZATION
// ============================================================

/**
 * A stable holder for computed strings
 */
@Stable
class StableString(private val value: String) {
    override fun toString(): String = value
    override fun equals(other: Any?): Boolean = other is StableString && other.value == value
    override fun hashCode(): Int = value.hashCode()
}

/**
 * Remember a stable string computation
 */
@Composable
fun rememberStableString(computation: () -> String): StableString {
    return remember { StableString(computation()) }
}

// ============================================================
// MEASURE PERFORMANCE HELPER
// ============================================================

/**
 * Inline function to measure compose performance
 * Use only in debug builds
 */
inline fun measureComposePerformance(
    tag: String,
    block: () -> Unit
) {
    val start = System.nanoTime()
    block()
    val duration = (System.nanoTime() - start) / 1_000_000.0
    // Uncomment for debugging: println("[$tag] Compose took ${duration}ms")
}
