package art.ayachinene.homevideo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

// CS:GO / Steam inspired color palette
object CSGOColors {
    val Background = Color(0xFF1B2838)        // Deep blue-gray
    val Surface = Color(0xFF2A475E)           // Lighter blue-gray
    val Card = Color(0xFF1E3A5F)              // Dark blue for cards

    val Primary = Color(0xFFFF6B00)           // CS:GO orange
    val Secondary = Color(0xFF66C0F4)         // Steam blue

    val OnBackground = Color(0xFFFFFFFF)      // White text
    val OnSurface = Color(0xFF8F98A0)         // Secondary text
    val OnSurfaceDisabled = Color(0xFF4C5B68) // Disabled text

    val Error = Color(0xFFB00020)
    val OnError = Color(0xFFFFFFFF)

    val FocusHighlight = Color(0xFFFF6B00)    // Orange glow for focus
}

// 26 colors for placeholder thumbnails (A-Z)
val PlaceholderColors = listOf(
    Color(0xFFE74C3C), Color(0xFFE67E22), Color(0xFFF1C40F),
    Color(0xFF2ECC71), Color(0xFF1ABC9C), Color(0xFF3498DB),
    Color(0xFF9B59B6), Color(0xFFE91E63), Color(0xFF00BCD4),
    Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A),
    Color(0xFFCDDC39), Color(0xFFFFC107), Color(0xFFFF9800),
    Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B),
    Color(0xFF2196F3), Color(0xFF3F51B5), Color(0xFF673AB7),
    Color(0xFF9C27B0), Color(0xFF66C0F4), Color(0xFFFF6B00),
    Color(0xFF00E676), Color(0xFFFF1744)
)

private val CSGODarkColors = darkColorScheme(
    background = CSGOColors.Background,
    surface = CSGOColors.Surface,
    primary = CSGOColors.Primary,
    secondary = CSGOColors.Secondary,
    onBackground = CSGOColors.OnBackground,
    onSurface = CSGOColors.OnSurface,
    error = CSGOColors.Error,
    onError = CSGOColors.OnError
)

@Composable
fun HomeVideoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = CSGODarkColors, content = content)
}
