package com.pocketdev.app.ui.navigation

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
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

@OptIn(ExperimentalMaterial3Api::class)
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
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(item.icon, contentDescription = item.label)
                        },
                        label = {
                            Text(
                                item.label,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors()
                    )
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
            composable(route = BottomNavItem.Editor.route) {
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

            composable(route = BottomNavItem.Projects.route) {
                ProjectsScreen(
                    viewModel = editorViewModel,
                    onOpenProject = {
                        navController.navigate(BottomNavItem.Editor.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(route = BottomNavItem.Examples.route) {
                ExamplesScreen(
                    onLoadExample = { example ->
                        editorViewModel.newFile(example.language)
                        editorViewModel.updateCode(example.code)
                        navController.navigate(BottomNavItem.Editor.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(route = BottomNavItem.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
