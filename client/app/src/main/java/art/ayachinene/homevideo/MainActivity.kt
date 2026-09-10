package art.ayachinene.homevideo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import art.ayachinene.homevideo.data.ServerPreferences
import art.ayachinene.homevideo.ui.HomeScreen
import art.ayachinene.homevideo.ui.OnboardingScreen
import art.ayachinene.homevideo.ui.PlayerScreen
import art.ayachinene.homevideo.ui.SettingsScreen
import art.ayachinene.homevideo.ui.theme.HomeVideoTheme
import kotlinx.coroutines.flow.first

sealed class Screen {
    data object Onboarding : Screen()
    data object Home : Screen()
    data class Player(val videoPath: String, val videoTitle: String) : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeVideoTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
private fun AppNavigation() {
    val prefs = remember { ServerPreferences(HomeVideoApp.instance) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Onboarding) }

    LaunchedEffect(Unit) {
        val savedIp = prefs.serverIp.first()
        currentScreen = if (savedIp.isNotBlank()) Screen.Home else Screen.Onboarding
    }

    BackHandler(enabled = currentScreen is Screen.Home) {
        // On TV, BACK from home exits the app (default behavior)
    }

    when (val screen = currentScreen) {
        is Screen.Onboarding -> {
            OnboardingScreen(
                onConnected = { currentScreen = Screen.Home }
            )
        }
        is Screen.Home -> {
            HomeScreen(
                onPlayVideo = { path, title ->
                    currentScreen = Screen.Player(path, title)
                },
                onOpenSettings = { currentScreen = Screen.Settings }
            )
        }
        is Screen.Player -> {
            PlayerScreen(
                videoPath = screen.videoPath,
                videoTitle = screen.videoTitle,
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.Settings -> {
            SettingsScreen(
                onBack = { currentScreen = Screen.Home }
            )
        }
    }
}
