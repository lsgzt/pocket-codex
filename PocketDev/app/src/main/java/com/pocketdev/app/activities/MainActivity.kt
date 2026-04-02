package com.pocketdev.app.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketdev.app.ui.navigation.AppNavigation
import com.pocketdev.app.ui.theme.PocketDevTheme
import com.pocketdev.app.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val theme by settingsViewModel.theme.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            
            val isDark = when (theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemDark
            }

            PocketDevTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PremiumSplashScreen {
                        AppNavigation()
                    }
                }
            }
        }
    }
}

/**
 * Premium animated splash screen with smooth transitions.
 * Displays the app logo with a fade and scale animation,
 * then smoothly transitions to the main content.
 */
@Composable
fun PremiumSplashScreen(
    content: @Composable () -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }
    var showContent by remember { mutableStateOf(false) }
    
    // Animated logo scale
    val logoScale by animateFloatAsState(
        targetValue = if (showSplash) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    
    // Animated content alpha
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(500),
        label = "contentAlpha"
    )

    // Splash screen timing
    LaunchedEffect(Unit) {
        delay(800) // Show splash for 800ms
        showSplash = false
        delay(200) // Brief overlap for smooth transition
        showContent = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content with fade in
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha }
        ) {
            if (showContent) {
                content()
            }
        }
        
        // Splash screen with fade and scale out
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                targetScale = 1.2f
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            SplashScreenContent(
                logoScale = logoScale
            )
        }
    }
}

/**
 * The actual splash screen content with branding.
 */
@Composable
private fun SplashScreenContent(
    logoScale: Float
) {
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated logo icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(logoScale)
                    .background(
                        brush = Brush.linearGradient(colors = gradientColors),
                        shape = MaterialTheme.shapes.extraLarge
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App name with gradient text
            Text(
                text = "PocketDev",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline
            Text(
                text = "Code • Learn • Create",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
