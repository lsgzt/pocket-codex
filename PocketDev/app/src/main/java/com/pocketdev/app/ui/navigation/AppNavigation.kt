package com.pocketdev.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnter(): EnterTransition {
    return slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
}

@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExit(): ExitTransition {
    return slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
}

@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.backwardEnter(): EnterTransition {
    return slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
}

@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.backwardExit(): ExitTransition {
    return slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
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
            NavigationBar {
                navItems.forEach { item ->
                    key(item.route) {
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Projects.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(
                route = BottomNavItem.Editor.route,
                enterTransition = { forwardEnter() },
                exitTransition = { forwardExit() },
                popEnterTransition = { backwardEnter() },
                popExitTransition = { backwardExit() }
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
                route = BottomNavItem.Projects.route,
                enterTransition = { forwardEnter() },
                exitTransition = { forwardExit() },
                popEnterTransition = { backwardEnter() },
                popExitTransition = { backwardExit() }
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
                route = BottomNavItem.Examples.route,
                enterTransition = { forwardEnter() },
                exitTransition = { forwardExit() },
                popEnterTransition = { backwardEnter() },
                popExitTransition = { backwardExit() }
            ) {
                ExamplesScreen(
                    onLoadExample = { example ->
                        editorViewModel.newFile(example.language)
                        editorViewModel.updateCode(example.code)
                        navController.navigate(BottomNavItem.Editor.route) {
                            popUpTo(BottomNavItem.Editor.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = BottomNavItem.Settings.route,
                enterTransition = { forwardEnter() },
                exitTransition = { forwardExit() },
                popEnterTransition = { backwardEnter() },
                popExitTransition = { backwardExit() }
            ) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
