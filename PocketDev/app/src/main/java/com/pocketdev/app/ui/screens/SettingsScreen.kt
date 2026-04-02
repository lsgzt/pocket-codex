package com.pocketdev.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pocketdev.app.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val tabSize by viewModel.tabSize.collectAsStateWithLifecycle()
    val autoSave by viewModel.autoSave.collectAsStateWithLifecycle()
    val autocomplete by viewModel.autocomplete.collectAsStateWithLifecycle()
    val ghostSuggestions by viewModel.ghostSuggestions.collectAsStateWithLifecycle()
    val lineNumbers by viewModel.lineNumbers.collectAsStateWithLifecycle()
    val wordWrap by viewModel.wordWrap.collectAsStateWithLifecycle()
    val apiKeyState by viewModel.apiKeyState.collectAsStateWithLifecycle()
    val aiModel by viewModel.aiModel.collectAsStateWithLifecycle()

    val onSetTheme = remember { { value: String -> viewModel.setTheme(value) } }
    val onSetFontSize = remember { { size: Int -> viewModel.setFontSize(size) } }
    val onSetTabSize = remember { { size: Int -> viewModel.setTabSize(size) } }
    val onSetLineNumbers = remember { { enabled: Boolean -> viewModel.setLineNumbers(enabled) } }
    val onSetWordWrap = remember { { enabled: Boolean -> viewModel.setWordWrap(enabled) } }
    val onSetAutoSave = remember { { enabled: Boolean -> viewModel.setAutoSave(enabled) } }
    val onSetAutocomplete = remember { { enabled: Boolean -> viewModel.setAutocomplete(enabled) } }
    val onSetGhostSuggestions = remember { { enabled: Boolean -> viewModel.setGhostSuggestions(enabled) } }
    val onSetApiKey = remember { { key: String -> viewModel.setApiKey(key) } }
    val onClearApiKey = remember { { viewModel.clearApiKey() } }
    val onSetAiModel = remember { { model: String -> viewModel.setAiModel(model) } }
    val onResetDefaults = remember { { viewModel.resetToDefaults() } }

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showApiKeyInfo by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    // Animated entrance
    var isLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        isLoaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Animated sections
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(300)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 },
                modifier = Modifier
            ) {
                // AI Section
                PremiumSettingsSectionHeader(
                    title = "🤖 AI Integration (Groq)",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // API Key status card with animated entrance
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 100)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumApiKeyCard(
                    apiKeyState = apiKeyState,
                    onSetApiKey = { showApiKeyDialog = true },
                    onClearApiKey = onClearApiKey
                )
            }
            
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 150)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsItem(
                    icon = Icons.Default.Info,
                    title = "How to get Groq API Key",
                    subtitle = "Free API key from console.groq.com",
                    onClick = { showApiKeyInfo = true }
                )
            }

            // AI Model Selection
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                var modelText by remember(aiModel) { mutableStateOf(aiModel) }
                PremiumModelInputCard(
                    modelText = modelText,
                    aiModel = aiModel,
                    onModelTextChange = { modelText = it },
                    onSaveModel = { if (modelText.isNotBlank()) onSetAiModel(modelText.trim()) }
                )
            }

            PremiumDivider()

            // Editor Section
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 250)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsSectionHeader(
                    title = "✏️ Editor",
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Theme selector
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 300)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumThemeSelector(
                    currentTheme = theme,
                    onSetTheme = onSetTheme
                )
            }

            // Font size with animated slider
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 350)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumFontSizeSlider(
                    fontSize = fontSize,
                    onSetFontSize = onSetFontSize
                )
            }

            // Tab size
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 400)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumTabSizeSelector(
                    tabSize = tabSize,
                    onSetTabSize = onSetTabSize
                )
            }

            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 450)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsSwitchItem(
                    icon = Icons.Default.FormatListNumbered,
                    title = "Line Numbers",
                    subtitle = "Show line numbers in editor",
                    checked = lineNumbers,
                    onCheckedChange = onSetLineNumbers
                )
            }

            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 500)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsSwitchItem(
                    icon = Icons.Default.WrapText,
                    title = "Word Wrap",
                    subtitle = "Wrap long lines to fit screen",
                    checked = wordWrap,
                    onCheckedChange = onSetWordWrap
                )
            }

            PremiumDivider()

            // Features Section
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 550)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsSectionHeader(
                    title = "⚙️ Features",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 600)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsSwitchItem(
                    icon = Icons.Default.Save,
                    title = "Auto Save",
                    subtitle = "Save changes automatically every 30 seconds",
                    checked = autoSave,
                    onCheckedChange = onSetAutoSave
                )
            }

            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 650)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsSwitchItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "Autocomplete",
                    subtitle = "Show code completion suggestions",
                    checked = autocomplete,
                    onCheckedChange = onSetAutocomplete
                )
            }

            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 700)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsSwitchItem(
                    icon = Icons.Default.Psychology,
                    title = "AI Ghost Suggestions",
                    subtitle = "Show inline AI code suggestions",
                    checked = ghostSuggestions,
                    onCheckedChange = onSetGhostSuggestions
                )
            }

            PremiumDivider()

            // About Section
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 750)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsSectionHeader(
                    title = "ℹ️ About",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 800)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsItem(
                    icon = Icons.Default.Info,
                    title = "PocketDev",
                    subtitle = "Version 1.0.0 — Mobile coding for students"
                )
            }

            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 850)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsItem(
                    icon = Icons.Default.Code,
                    title = "Supported Languages",
                    subtitle = "Python, JavaScript, HTML, CSS, Java, C++, Kotlin, JSON"
                )
            }

            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 900)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsItem(
                    icon = Icons.Default.PlayCircle,
                    title = "Execution Engines",
                    subtitle = "Python: Chaquopy • JavaScript: Rhino • HTML: WebView"
                )
            }

            PremiumDivider()

            // Reset
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 950)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.RestartAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reset to Defaults")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        PremiumApiKeyDialog(
            onSave = { key ->
                onSetApiKey(key)
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }

    // Reset dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.RestartAlt, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset Settings?") },
            text = { Text("This will reset all settings to their defaults. Your projects and API key will not be affected.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // API Key info dialog
    if (showApiKeyInfo) {
        AlertDialog(
            onDismissRequest = { showApiKeyInfo = false },
            icon = { Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Getting a Groq API Key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Follow these steps to get your free Groq API key:")
                    Text("1. Visit console.groq.com in your browser")
                    Text("2. Create a free account or sign in")
                    Text("3. Go to API Keys section")
                    Text("4. Click \"Create API Key\"")
                    Text("5. Copy the key (starts with gsk_)")
                    Text("6. Paste it in the API Key field here")
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "💡 Groq offers a generous free tier — no credit card required!",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showApiKeyInfo = false }) { Text("Got it!") }
            }
        )
    }
}

// Premium animated section header
@Composable
fun PremiumSettingsSectionHeader(
    title: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.3f),
                            color.copy(alpha = 0.0f)
                        )
                    )
                )
        )
    }
}

// Premium divider with fade effect
@Composable
fun PremiumDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.0f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.0f)
                    )
                )
            )
    )
}

// Premium API Key card with animated status
@Composable
fun PremiumApiKeyCard(
    apiKeyState: SettingsViewModel.ApiKeyState,
    onSetApiKey: () -> Unit,
    onClearApiKey: () -> Unit
) {
    val cardColor = when (apiKeyState) {
        is SettingsViewModel.ApiKeyState.Set -> MaterialTheme.colorScheme.secondaryContainer
        is SettingsViewModel.ApiKeyState.Error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val iconColor = when (apiKeyState) {
        is SettingsViewModel.ApiKeyState.Set -> MaterialTheme.colorScheme.secondary
        is SettingsViewModel.ApiKeyState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    // Animated scale on state change
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated icon
            val iconScale by animateFloatAsState(
                targetValue = if (apiKeyState is SettingsViewModel.ApiKeyState.Set) 1.1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "iconScale"
            )
            
            Icon(
                when (apiKeyState) {
                    is SettingsViewModel.ApiKeyState.Set -> Icons.Default.CheckCircle
                    is SettingsViewModel.ApiKeyState.Error -> Icons.Default.Error
                    else -> Icons.Default.Key
                },
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.scale(iconScale)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (apiKeyState) {
                        is SettingsViewModel.ApiKeyState.Set -> "API Key Configured"
                        is SettingsViewModel.ApiKeyState.Error -> "Invalid API Key"
                        else -> "No API Key Set"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = when (apiKeyState) {
                        is SettingsViewModel.ApiKeyState.Set ->
                            (apiKeyState as SettingsViewModel.ApiKeyState.Set).maskedKey
                        is SettingsViewModel.ApiKeyState.Error ->
                            (apiKeyState as SettingsViewModel.ApiKeyState.Error).message
                        else -> "Required for AI features (Fix Bug, Explain, Improve)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSetApiKey,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (apiKeyState is SettingsViewModel.ApiKeyState.Set) "Change Key" else "Set API Key")
            }
            if (apiKeyState is SettingsViewModel.ApiKeyState.Set) {
                OutlinedButton(
                    onClick = onClearApiKey,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }
    }
}

// Premium settings item with hover animation
@Composable
fun PremiumSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "elevation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        tonalElevation = elevation
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onClick != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Premium switch item with animated toggle
@Composable
fun PremiumSettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val iconScale by animateFloatAsState(
        targetValue = if (checked) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )
    
    val iconColor by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .scale(iconScale)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (checked) androidx.compose.ui.text.font.FontWeight.Medium 
                    else androidx.compose.ui.text.font.FontWeight.Normal
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
            } else null
        )
    }
}

// Premium theme selector
@Composable
fun PremiumThemeSelector(
    currentTheme: String,
    onSetTheme: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val themeOptions = remember { 
        listOf(
            "dark" to Pair("🌙", "Dark"),
            "light" to Pair("☀️", "Light"),
            "auto" to Pair("🔄", "System")
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Theme", style = MaterialTheme.typography.bodyLarge)
            Text(
                when (currentTheme) {
                    "light" -> "☀️ Light Mode"
                    "auto" -> "🔄 Follow System"
                    else -> "🌙 Dark Mode"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(when (currentTheme) {
                    "light" -> "☀️ Light"
                    "auto" -> "🔄 Auto"
                    else -> "🌙 Dark"
                })
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                themeOptions.forEach { (value, pair) ->
                    DropdownMenuItem(
                        text = { Text("${pair.first} ${pair.second}") },
                        onClick = { onSetTheme(value); expanded = false },
                        leadingIcon = if (currentTheme == value) {
                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                        } else null
                    )
                }
            }
        }
    }
}

// Premium font size slider
@Composable
fun PremiumFontSizeSlider(
    fontSize: Int,
    onSetFontSize: (Int) -> Unit
) {
    var sliderValue by remember(fontSize) { mutableFloatStateOf(fontSize.toFloat()) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.TextFields,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Font Size", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${fontSize}sp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onSetFontSize(sliderValue.toInt()) },
                valueRange = 10f..22f,
                steps = 11,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Premium tab size selector
@Composable
fun PremiumTabSizeSelector(
    tabSize: Int,
    onSetTabSize: (Int) -> Unit
) {
    val tabSizes = remember { listOf(2, 4, 8) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.SpaceBar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Tab Size", style = MaterialTheme.typography.bodyLarge)
            Text(
                "$tabSize spaces",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            tabSizes.forEach { size ->
                val selected = tabSize == size
                
                // Animated scale for selected chip
                val chipScale by animateFloatAsState(
                    targetValue = if (selected) 1.05f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "chipScale"
                )
                
                FilterChip(
                    selected = selected,
                    onClick = { onSetTabSize(size) },
                    label = { Text("$size") },
                    modifier = Modifier.scale(chipScale)
                )
            }
        }
    }
}

// Premium model input card
@Composable
fun PremiumModelInputCard(
    modelText: String,
    aiModel: String,
    onModelTextChange: (String) -> Unit,
    onSaveModel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("AI Model", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Enter exact model name from console.groq.com/docs/models",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = modelText,
                onValueChange = onModelTextChange,
                label = { Text("Model Name") },
                placeholder = { Text("e.g. llama-3.3-70b-versatile") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    AnimatedContent(
                        targetState = modelText != aiModel && modelText.isNotBlank(),
                        transitionSpec = {
                            scaleIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                            scaleOut(animationSpec = tween(100))
                        },
                        label = "saveIcon"
                    ) { showSave ->
                        if (showSave) {
                            IconButton(onClick = onSaveModel) {
                                Icon(Icons.Default.Check, "Save model", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    "Visit console.groq.com/docs/models for available model names",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Premium API Key dialog
@Composable
fun PremiumApiKeyDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    
    // Animated visibility toggle
    val visibilityIconScale by animateFloatAsState(
        targetValue = if (showKey) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "visibilityIconScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Default.Key, 
                null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            ) 
        },
        title = { Text("Set Groq API Key") },
        text = {
            Column {
                Text(
                    "Enter your Groq API key to enable AI features.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("gsk_...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (showKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { showKey = !showKey },
                            modifier = Modifier.scale(visibilityIconScale)
                        ) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKey) "Hide" else "Show"
                            )
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "🔒 Your key is encrypted and stored securely on your device.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (apiKey.isNotBlank()) onSave(apiKey.trim()) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
