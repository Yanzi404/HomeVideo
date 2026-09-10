package art.ayachinene.homevideo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import art.ayachinene.homevideo.ui.theme.PlaceholderColors

@Composable
fun ThumbnailPlaceholder(
    name: String,
    modifier: Modifier = Modifier,
    width: Dp = 240.dp
) {
    val letter = name.firstOrNull()?.uppercaseChar() ?: '?'
    val colorIndex = letter.code % PlaceholderColors.size
    val bgColor = PlaceholderColors[colorIndex]

    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.displayMedium,
            color = Color.White
        )
    }
}
