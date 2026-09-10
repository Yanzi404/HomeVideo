package art.ayachinene.homevideo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import art.ayachinene.homevideo.data.ServerPreferences
import art.ayachinene.homevideo.ui.HomeScreen
import art.ayachinene.homevideo.ui.OnboardingScreen
import art.ayachinene.homevideo.ui.PlayerScreen
import art.ayachinene.homevideo.ui.SettingsScreen
import art.ayachinene.homevideo.ui.theme.CSGOColors
import art.ayachinene.homevideo.ui.theme.HomeVideoTheme
import kotlinx.coroutines.flow.first

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

private sealed class InitState {
    data object Loading : InitState()
    data object NeedsOnboarding : InitState()
    data object ReadyForHome : InitState()
}

@Composable
private fun AppNavigation() {
    val prefs = remember { ServerPreferences(HomeVideoApp.instance) }
    var initState by remember { mutableStateOf<InitState>(InitState.Loading) }

    LaunchedEffect(Unit) {
        val savedIp = prefs.serverIp.first()
        initState = if (savedIp.isNotBlank()) InitState.ReadyForHome else InitState.NeedsOnboarding
    }

    if (initState is InitState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize().background(CSGOColors.Background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CSGOColors.Primary)
        }
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (initState is InitState.NeedsOnboarding) "onboarding" else "home",
        enterTransition = {
            slideInHorizontally { it } + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally { -it } + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally { -it } + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally { it } + fadeOut()
        }
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onConnected = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onPlayVideo = { path, title ->
                    val encodedPath = Uri.encode(path)
                    val encodedTitle = Uri.encode(title)
                    navController.navigate("player?videoPath=$encodedPath&videoTitle=$encodedTitle")
                },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable(
            "player?videoPath={videoPath}&videoTitle={videoTitle}",
            arguments = listOf(
                androidx.navigation.navArgument("videoPath") { nullable = false },
                androidx.navigation.navArgument("videoTitle") { nullable = false }
            )
        ) { backStackEntry ->
            val videoPath = backStackEntry.arguments?.getString("videoPath") ?: ""
            val videoTitle = backStackEntry.arguments?.getString("videoTitle") ?: ""
            PlayerScreen(
                videoPath = Uri.decode(videoPath),
                videoTitle = Uri.decode(videoTitle),
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
