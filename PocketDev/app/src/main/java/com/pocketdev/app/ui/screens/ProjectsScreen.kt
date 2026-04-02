package com.pocketdev.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pocketdev.app.data.models.Project
import com.pocketdev.app.ui.components.*
import com.pocketdev.app.ui.theme.LocalGradientColors
import com.pocketdev.app.ui.theme.PremiumTextStyles
import com.pocketdev.app.ui.utils.*
import com.pocketdev.app.viewmodels.EditorViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: EditorViewModel,
    onOpenProject: () -> Unit
) {
    val projects by viewModel.filteredProjects.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf<Project?>(null) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    
    // Performance-aware settings
    val tier = rememberPerformanceTier()
    val enableComplexAnimations = tier != DevicePerformance.Tier.LOW

    val onOpenNewProjectDialog = remember { { showNewProjectDialog = true } }
    val onSearchQueryChange = remember { { query: String -> viewModel.setSearchQuery(query) } }
    val onClearSearch = remember { { viewModel.setSearchQuery("") } }
    val onDismissDeleteDialog = remember { { showDeleteDialog = null } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "My Projects",
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                actions = {
                    FilledTonalButton(
                        onClick = onOpenNewProjectDialog,
                        modifier = Modifier.padding(end = 12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "New Project",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("New", fontWeight = FontWeight.Medium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            // Simple search bar (no heavy animation on load)
            PremiumSearchBar(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                onClear = onClearSearch,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (projects.isEmpty()) {
                EmptyProjectsView(
                    onCreateNew = onOpenNewProjectDialog
                )
            } else {
                // Simple project count (no animation)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${projects.size} project${if (projects.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "• filtered",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = projects,
                        key = { project -> project.id }
                    ) { project ->
                        val onOpen = remember(project) {
                            {
                                viewModel.loadProject(project)
                                onOpenProject()
                            }
                        }
                        val onDelete = remember(project) { { showDeleteDialog = project } }
                        val onDuplicate = remember(project) { { viewModel.duplicateProject(project) } }

                        // Optimized project card
                        OptimizedProjectCard(
                            project = project,
                            onClick = onOpen,
                            onDelete = onDelete,
                            onDuplicate = onDuplicate
                        )
                    }
                    
                    // Bottom padding for FAB space
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { project ->
        AnimatedDeleteDialog(
            project = project,
            onConfirm = {
                viewModel.deleteProject(project)
                onDismissDeleteDialog()
            },
            onDismiss = onDismissDeleteDialog
        )
    }

    // New project dialog
    if (showNewProjectDialog) {
        val onCreateProject = remember(viewModel, onOpenProject) {
            { name: String, language: com.pocketdev.app.data.models.Language ->
                viewModel.newFile(language)
                viewModel.saveProject(name)
                showNewProjectDialog = false
                onOpenProject()
            }
        }
        val onDismissNewProject = remember { { showNewProjectDialog = false } }

        NewProjectDialog(
            onCreate = onCreateProject,
            onDismiss = onDismissNewProject
        )
    }
}

// ============================================================
// PREMIUM SEARCH BAR
// ============================================================

@Composable
private fun PremiumSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsPressedAsState()
    
    val animatedElevation by animateDpAsState(
        targetValue = if (value.isNotBlank()) 4.dp else 2.dp,
        animationSpec = SpringSpecs.gentleDp,
        label = "searchElevation"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = animatedElevation,
        shadowElevation = animatedElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        "Search projects...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                // Basic text field replacement for search
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            
            // Animated clear button
            AnimatedVisibility(
                visible = value.isNotBlank(),
                enter = scaleIn(animationSpec = SpringSpecs.bouncy) + fadeIn(),
                exit = scaleOut(animationSpec = SpringSpecs.snappy) + fadeOut()
            ) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================
// OPTIMIZED PROJECT CARD
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptimizedProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val tier = rememberPerformanceTier()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(project.modifiedAt) { dateFormat.format(Date(project.modifiedAt)) }
    
    // Simple scale animation (tween instead of spring)
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = OptimizedAnimations.rememberButtonPressSpec(tier),
        label = "cardScale"
    )
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language icon (no hover animation on low-end)
            SimpleLanguageIcon(language = project.language)

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Language tag
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = project.language.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    
                    Spacer(Modifier.width(4.dp))
                    
                    Text(
                        text = formattedDate,
                        style = PremiumTextStyles.timestamp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Menu button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                ) {
                    DropdownMenuItem(
                        text = { Text("Open", fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { onClick(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate", fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = MaterialTheme.colorScheme.secondary) },
                        onClick = { onDuplicate(); showMenu = false }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        }
    }
}

// ============================================================
// SIMPLE LANGUAGE ICON (no hover animation)
// ============================================================

@Composable
private fun SimpleLanguageIcon(language: com.pocketdev.app.data.models.Language) {
    val gradientColors = remember(language) {
        when (language) {
            com.pocketdev.app.data.models.Language.PYTHON -> listOf(Color(0xFF3776AB), Color(0xFF4B8BBE))
            com.pocketdev.app.data.models.Language.JAVASCRIPT -> listOf(Color(0xFFF7DF1E), Color(0xFFF0DB4F))
            com.pocketdev.app.data.models.Language.HTML -> listOf(Color(0xFFE34F26), Color(0xFFF06529))
            com.pocketdev.app.data.models.Language.CSS -> listOf(Color(0xFF1572B6), Color(0xFF33A9DC))
            com.pocketdev.app.data.models.Language.JAVA -> listOf(Color(0xFFED8B00), Color(0xFFFFA726))
            com.pocketdev.app.data.models.Language.CPP -> listOf(Color(0xFF00599C), Color(0xFF004482))
            com.pocketdev.app.data.models.Language.KOTLIN -> listOf(Color(0xFF7F52FF), Color(0xFFC711E1))
            com.pocketdev.app.data.models.Language.JSON -> listOf(Color(0xFF4A90A4), Color(0xFF5FA8B3))
        }
    }
    
    Surface(
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = language.icon,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// ============================================================
// ANIMATED DELETE DIALOG
// ============================================================

@Composable
private fun AnimatedDeleteDialog(
    project: Project,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = { 
            Text(
                "Delete Project?",
                fontWeight = FontWeight.SemiBold
            ) 
        },
        text = { 
            Text(
                "Are you sure you want to delete \"${project.name}\"? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onConfirm,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(
                    "Delete",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text(
                    "Cancel",
                    fontWeight = FontWeight.Medium
                ) 
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

// ============================================================
// EMPTY PROJECTS VIEW - Simplified
// ============================================================

@Composable
fun EmptyProjectsView(onCreateNew: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Simple folder icon (no breathing animation)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "📁",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                "No projects yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "Create your first project to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(32.dp))
            
            FilledTonalButton(
                onClick = onCreateNew,
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Create New Project",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================================
// NEW PROJECT DIALOG - Enhanced with animations
// ============================================================

@Composable
fun NewProjectDialog(
    onCreate: (String, com.pocketdev.app.data.models.Language) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(com.pocketdev.app.data.models.Language.PYTHON) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    val allLanguages = remember { com.pocketdev.app.data.models.Language.values().toList() }
    
    // Animate name input validation
    val isNameValid by remember { derivedStateOf { name.isNotBlank() } }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Outlined.CreateNewFolder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = { 
            Text(
                "New Project",
                fontWeight = FontWeight.SemiBold
            ) 
        },
        text = {
            Column {
                // Project name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    isError = name.isEmpty() && name.isNotEmpty(),
                    supportingText = if (name.isEmpty() && name.isNotEmpty()) {
                        { Text("Name is required") }
                    } else null
                )
                
                Spacer(Modifier.height(20.dp))
                
                // Language selector
                Text(
                    "Language",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(Modifier.height(8.dp))
                
                Box {
                    OutlinedButton(
                        onClick = { showLanguageMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        LanguageIconBadge(
                            language = selectedLanguage,
                            isHovered = false
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            selectedLanguage.displayName,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        allLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "${lang.icon} ${lang.displayName}",
                                        fontWeight = FontWeight.Medium
                                    ) 
                                },
                                onClick = {
                                    selectedLanguage = lang
                                    showLanguageMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    if (name.isNotBlank()) onCreate(name.trim(), selectedLanguage)
                },
                enabled = isNameValid
            ) {
                Text(
                    "Create",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text(
                    "Cancel",
                    fontWeight = FontWeight.Medium
                ) 
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}
