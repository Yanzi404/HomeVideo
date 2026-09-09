package art.ayachinene.homevideo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import art.ayachinene.homevideo.ui.HomeScreen
import art.ayachinene.homevideo.ui.theme.HomeVideoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeVideoTheme {
                HomeScreen()
            }
        }
    }
}
