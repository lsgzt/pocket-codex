package com.pocketdev.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pocketdev.app.ui.screens.EditorScreen
import com.pocketdev.app.ui.screens.ExamplesScreen
import com.pocketdev.app.ui.screens.ProjectsScreen
import com.pocketdev.app.ui.screens.SettingsScreen
import com.pocketdev.app.viewmodels.EditorViewModel
import com.pocketdev.app.viewmodels.SettingsViewModel

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Editor : BottomNavItem("editor", Icons.Default.Code, "Editor")
    object Projects : BottomNavItem("projects", Icons.Default.FolderOpen, "Projects")
    object Examples : BottomNavItem("examples", Icons.Default.MenuBook, "Examples")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
}

// Premium navigation animation durations
private const val ENTER_DURATION = 350
private const val EXIT_DURATION = 250
private const val FADE_DURATION = 200

/**
 * Premium forward navigation transition with scale and slide.
 * Uses spring-based animations for a natural, bouncy feel.
 */
@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnter(): EnterTransition {
    return slideInHorizontally(
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        initialOffsetX = { fullWidth -> fullWidth / 3 }
    ) + fadeIn(
        animationSpec = tween(ENTER_DURATION)
    ) + scaleIn(
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        initialScale = 0.95f
    )
}

/**
 * Premium forward exit transition - slides out to the left.
 */
@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExit(): ExitTransition {
    return slideOutHorizontally(
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        targetOffsetX = { fullWidth -> -fullWidth / 4 }
    ) + fadeOut(
        animationSpec = tween(EXIT_DURATION)
    ) + scaleOut(
        animationSpec = tween(EXIT_DURATION),
        targetScale = 0.95f
    )
}

/**
 * Premium backward navigation transition - slides in from the left.
 */
@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.backwardEnter(): EnterTransition {
    return slideInHorizontally(
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        initialOffsetX = { fullWidth -> -fullWidth / 4 }
    ) + fadeIn(
        animationSpec = tween(ENTER_DURATION)
    ) + scaleIn(
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        initialScale = 0.95f
    )
}

/**
 * Premium backward exit transition - slides out to the right.
 */
@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.backwardExit(): ExitTransition {
    return slideOutHorizontally(
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        targetOffsetX = { fullWidth -> fullWidth / 3 }
    ) + fadeOut(
        animationSpec = tween(EXIT_DURATION)
    ) + scaleOut(
        animationSpec = tween(EXIT_DURATION),
        targetScale = 0.95f
    )
}

/**
 * Smooth crossfade transition for same-level navigation.
 */
@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.crossfadeEnter(): EnterTransition {
    return fadeIn(
        animationSpec = tween(FADE_DURATION)
    ) + scaleIn(
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        initialScale = 0.98f
    )
}

@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.crossfadeExit(): ExitTransition {
    return fadeOut(
        animationSpec = tween(FADE_DURATION)
    ) + scaleOut(
        animationSpec = tween(FADE_DURATION),
        targetScale = 0.98f
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val editorViewModel: EditorViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val navItems = remember {
        listOf(
            BottomNavItem.Editor,
            BottomNavItem.Projects,
            BottomNavItem.Examples,
            BottomNavItem.Settings
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            PremiumNavigationBar(
                navItems = navItems,
                currentRoute = currentRoute,
                onItemClick = { item ->
                    val targetIndex = navItems.indexOf(item)
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Projects.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            enterTransition = {
                val targetIndex = navItems.indexOfFirst { it.route == targetState.destination.route }
                val fromIndex = navItems.indexOfFirst { it.route == initialState.destination.route }
                
                if (targetIndex > fromIndex) {
                    forwardEnter()
                } else if (targetIndex < fromIndex) {
                    backwardEnter()
                } else {
                    crossfadeEnter()
                }
            },
            exitTransition = {
                val targetIndex = navItems.indexOfFirst { it.route == targetState.destination.route }
                val fromIndex = navItems.indexOfFirst { it.route == initialState.destination.route }
                
                if (targetIndex > fromIndex) {
                    forwardExit()
                } else if (targetIndex < fromIndex) {
                    backwardExit()
                } else {
                    crossfadeExit()
                }
            },
            popEnterTransition = { backwardEnter() },
            popExitTransition = { backwardExit() }
        ) {
            composable(
                route = BottomNavItem.Editor.route
            ) {
                EditorScreen(
                    viewModel = editorViewModel,
                    settingsViewModel = settingsViewModel,
                    onNavigateToProjects = {
                        navController.navigate(BottomNavItem.Projects.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = BottomNavItem.Projects.route
            ) {
                ProjectsScreen(
                    viewModel = editorViewModel,
                    onOpenProject = {
                        navController.navigate(BottomNavItem.Editor.route) {
                            popUpTo(BottomNavItem.Editor.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = BottomNavItem.Examples.route
            ) {
                ExamplesScreen(
                    onLoadExample = { example ->
                        editorViewModel.newFile(example.language)
                        editorViewModel.updateCode(example.code)
                        navController.navigate(BottomNavItem.Editor.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = BottomNavItem.Settings.route
            ) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}

/**
 * Premium navigation bar with animated indicators and icons.
 */
@Composable
private fun PremiumNavigationBar(
    navItems: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    NavigationBar {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item) },
                icon = { 
                    Icon(
                        item.icon, 
                        contentDescription = item.label
                    ) 
                },
                label = { 
                    Text(
                        item.label,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
