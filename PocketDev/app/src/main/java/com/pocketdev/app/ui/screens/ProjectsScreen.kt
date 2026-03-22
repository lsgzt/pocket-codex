package com.pocketdev.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketdev.app.data.models.Project
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

    val onOpenNewProjectDialog = remember { { showNewProjectDialog = true } }
    val onSearchQueryChange = remember { { query: String -> viewModel.setSearchQuery(query) } }
    val onClearSearch = remember { { viewModel.setSearchQuery("") } }
    val onDismissDeleteDialog = remember { { showDeleteDialog = null } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Projects") },
                actions = {
                    IconButton(onClick = onOpenNewProjectDialog) {
                        Icon(Icons.Default.Add, "New Project")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search projects...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Clear, "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (projects.isEmpty()) {
                EmptyProjectsView(
                    onCreateNew = onOpenNewProjectDialog
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        val onOpen = remember(project) {
                            {
                                viewModel.loadProject(project)
                                onOpenProject()
                            }
                        }
                        val onDelete = remember(project) { { showDeleteDialog = project } }
                        val onDuplicate = remember(project) { { viewModel.duplicateProject(project) } }

                        ProjectCard(
                            project = project,
                            onClick = onOpen,
                            onDelete = onDelete,
                            onDuplicate = onDuplicate
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation
    showDeleteDialog?.let { project ->
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Project?") },
            text = { Text("Are you sure you want to delete \"${project.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(project)
                        onDismissDeleteDialog()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) { Text("Cancel") }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(project.modifiedAt) { dateFormat.format(Date(project.modifiedAt)) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language icon
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = project.language.icon,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = project.language.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, null) },
                        onClick = { onClick(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = { onDuplicate(); showMenu = false }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyProjectsView(onCreateNew: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📁", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(16.dp))
            Text(
                "No projects yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Create a new project to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onCreateNew) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("New Project")
            }
        }
    }
}

@Composable
fun NewProjectDialog(
    onCreate: (String, com.pocketdev.app.data.models.Language) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(com.pocketdev.app.data.models.Language.PYTHON) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    val allLanguages = remember { com.pocketdev.app.data.models.Language.values().toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Language:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Box {
                    OutlinedButton(
                        onClick = { showLanguageMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${selectedLanguage.icon} ${selectedLanguage.displayName}")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        allLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text("${lang.icon} ${lang.displayName}") },
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
            Button(
                onClick = {
                    if (name.isNotBlank()) onCreate(name.trim(), selectedLanguage)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
