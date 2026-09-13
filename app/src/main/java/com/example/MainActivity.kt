package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.apps.AppSelectionScreen
import com.example.ui.challenge.ChallengeScreen
import com.example.ui.challenge.SuccessScreen
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.Screen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.stats.StatisticsScreen
import com.example.ui.theme.TouchGrassTheme
import com.example.ui.viewmodel.TouchGrassViewModel

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_CHALLENGE = "extra_start_challenge"
    }

    private val viewModel: TouchGrassViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val autoStartChallenge = intent.getBooleanExtra(EXTRA_START_CHALLENGE, false)

        setContent {
            TouchGrassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TouchGrassAppRoot(
                        viewModel = viewModel,
                        autoStartChallenge = autoStartChallenge
                    )
                }
            }
        }
    }
}

@Composable
fun TouchGrassAppRoot(
    viewModel: TouchGrassViewModel,
    autoStartChallenge: Boolean = false
) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsState()

    val startDestination = if (autoStartChallenge) {
        Screen.Challenge.route
    } else if (!state.settings.onboardingCompleted) {
        Screen.Onboarding.route
    } else {
        Screen.Home.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToChallenge = {
                    navController.navigate(Screen.Challenge.route)
                },
                onNavigateToApps = {
                    navController.navigate(Screen.AppSelection.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Statistics.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Challenge.route) {
            ChallengeScreen(
                viewModel = viewModel,
                onChallengeCompleted = {
                    navController.navigate(Screen.Success.route) {
                        popUpTo(Screen.Challenge.route) { inclusive = true }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Success.route) {
            SuccessScreen(
                viewModel = viewModel,
                onContinue = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Success.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AppSelection.route) {
            AppSelectionScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "🌱 Touch Grass: $name", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TouchGrassTheme { Greeting("Android") }
}

