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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import com.pocketdev.app.api.AiProvider
import com.pocketdev.app.api.ModelCatalog
import com.pocketdev.app.api.ModelInfo
import com.pocketdev.app.ui.utils.DevicePerformance
import com.pocketdev.app.ui.utils.rememberPerformanceTier
import com.pocketdev.app.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

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
    val aiProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val thinkingMode by viewModel.thinkingMode.collectAsStateWithLifecycle()

    val onSetTheme = remember { { value: String -> viewModel.setTheme(value) } }
    val onSetFontSize = remember { { size: Int -> viewModel.setFontSize(size) } }
    val onSetTabSize = remember { { size: Int -> viewModel.setTabSize(size) } }
    val onSetLineNumbers = remember { { enabled: Boolean -> viewModel.setLineNumbers(enabled) } }
    val onSetWordWrap = remember { { enabled: Boolean -> viewModel.setWordWrap(enabled) } }
    val onSetAutoSave = remember { { enabled: Boolean -> viewModel.setAutoSave(enabled) } }
    val onSetAutocomplete = remember { { enabled: Boolean -> viewModel.setAutocomplete(enabled) } }
    val onSetGhostSuggestions = remember { { enabled: Boolean -> viewModel.setGhostSuggestions(enabled) } }
    val onSetApiKey = remember(aiProvider) { { key: String -> viewModel.setApiKey(aiProvider, key) } }
    val onClearApiKey = remember(aiProvider) { { viewModel.clearApiKey(aiProvider) } }
    val onSetAiModel = remember { { model: String -> viewModel.setAiModel(model) } }
    val onSetAiProvider = remember { { provider: AiProvider -> viewModel.setAiProvider(provider) } }
    val onSetThinkingMode = remember { { enabled: Boolean -> viewModel.setThinkingMode(enabled) } }
    val onResetDefaults = remember { { viewModel.resetToDefaults() } }

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showApiKeyInfo by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val tier = rememberPerformanceTier()
    
    // Animated entrance
    var isLoaded by remember { mutableStateOf(tier == DevicePerformance.Tier.LOW) }
    LaunchedEffect(tier) {
        if (tier != DevicePerformance.Tier.LOW) {
            delay(100)
            isLoaded = true
        }
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
                    title = "🤖 AI Integration",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Provider selector (Groq / OpenRouter / Gemini)
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 50)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumProviderSelector(
                    selectedProvider = aiProvider,
                    onSelectProvider = onSetAiProvider
                )
            }

            // API Key status card for the selected provider
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 100)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumApiKeyCard(
                    provider = aiProvider,
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
                    title = "How to get a ${aiProvider.displayName} API Key",
                    subtitle = "Free keys from ${aiProvider.consoleUrl.removePrefix("https://")}",
                    onClick = { showApiKeyInfo = true }
                )
            }

            // AI Model picker (curated catalog + custom input)
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumModelPickerCard(
                    provider = aiProvider,
                    aiModel = aiModel,
                    onSelectModel = onSetAiModel,
                    onSetCustomModel = onSetAiModel
                )
            }

            // Thinking / reasoning mode toggle
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 250)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 4 }
            ) {
                PremiumSettingsSwitchItem(
                    icon = Icons.Default.Psychology,
                    title = "Thinking Mode",
                    subtitle = "Let reasoning models show their thought process 🧠",
                    checked = thinkingMode,
                    onCheckedChange = onSetThinkingMode
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
            provider = aiProvider,
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
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showApiKeyInfo = false },
            icon = { Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Getting a ${aiProvider.displayName} API Key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Follow these steps to get your ${aiProvider.displayName} API key:")
                    Text("1. Visit ${aiProvider.consoleUrl.removePrefix("https://")} in your browser")
                    Text("2. Create a free account or sign in")
                    Text("3. Go to the API Keys section")
                    Text("4. Click \"Create API Key\"")
                    Text("5. Copy the key (looks like ${aiProvider.keyPrefixHint})")
                    Text("6. Paste it in the API Key field here")
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "💡 ${aiProvider.tagline}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(aiProvider.consoleUrl)))
                    }
                }) { Text("Open in Browser") }
            },
            dismissButton = {
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
    provider: AiProvider,
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
                        is SettingsViewModel.ApiKeyState.Set -> "${provider.displayName} Key Configured"
                        is SettingsViewModel.ApiKeyState.Error -> "Invalid ${provider.displayName} Key"
                        else -> "No ${provider.displayName} Key Set"
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

// Premium provider selector (Groq / OpenRouter / Gemini)
@Composable
fun PremiumProviderSelector(
    selectedProvider: AiProvider,
    onSelectProvider: (AiProvider) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("AI Provider", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Choose who powers your AI features",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiProvider.entries.forEach { provider ->
                val selected = provider == selectedProvider

                val cardScale by animateFloatAsState(
                    targetValue = if (selected) 1.02f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "providerScale"
                )

                Card(
                    onClick = { if (!selected) onSelectProvider(provider) },
                    modifier = Modifier
                        .weight(1f)
                        .scale(cardScale),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (selected) androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    ) else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (provider) {
                                AiProvider.GROQ -> Icons.Default.Bolt
                                AiProvider.OPENROUTER -> Icons.Default.Hub
                                AiProvider.GEMINI -> Icons.Default.AutoAwesome
                            },
                            contentDescription = provider.displayName,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            provider.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            provider.tagline,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

// Premium model picker card: curated catalog + custom model id
@Composable
fun PremiumModelPickerCard(
    provider: AiProvider,
    aiModel: String,
    onSelectModel: (String) -> Unit,
    onSetCustomModel: (String) -> Unit
) {
    val catalog = remember(provider) { ModelCatalog.models[provider].orEmpty() }
    var showCustomInput by remember { mutableStateOf(false) }
    var modelText by remember(aiModel, provider) { mutableStateOf(aiModel) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Model", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Pick a ${provider.displayName} model — 🧠 marks thinking models",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            catalog.forEach { model ->
                val selected = model.id == aiModel
                val rowScale by animateFloatAsState(
                    targetValue = if (selected) 1f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "modelRowScale"
                )
                Surface(
                    onClick = { onSelectModel(model.id) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    border = if (selected) androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    ) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .scale(rowScale)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectModel(model.id) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    model.label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (model.free) {
                                    Spacer(Modifier.width(6.dp))
                                    AssistChip(
                                        onClick = { onSelectModel(model.id) },
                                        label = { Text("Free", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                            if (model.description.isNotBlank()) {
                                Text(
                                    model.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Custom model input toggle
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = { showCustomInput = !showCustomInput },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
            ) {
                Icon(
                    if (showCustomInput) Icons.Default.ExpandLess else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (showCustomInput) "Hide custom model ID" else "Use a custom model ID",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            AnimatedVisibility(
                visible = showCustomInput,
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(200))
            ) {
                Column {
                    OutlinedTextField(
                        value = modelText,
                        onValueChange = { modelText = it },
                        label = { Text("Custom model ID") },
                        placeholder = { Text(ModelCatalog.defaultModel(provider)) },
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
                                    IconButton(onClick = { if (modelText.isNotBlank()) onSetCustomModel(modelText.trim()) }) {
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
                            "Type the exact model id from ${provider.consoleUrl.removePrefix("https://")}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Premium API Key dialog
@Composable
fun PremiumApiKeyDialog(
    provider: AiProvider,
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
        title = { Text("Set ${provider.displayName} API Key") },
        text = {
            Column {
                Text(
                    "Enter your ${provider.displayName} API key to enable AI features.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text(provider.keyPrefixHint) },
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
