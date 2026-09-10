package art.ayachinene.homevideo.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import art.ayachinene.homevideo.ui.theme.CSGOColors

@Composable
fun ShimmerHomeLoading() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    val shimmerColor = CSGOColors.Surface.copy(alpha = alpha)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CSGOColors.Background)
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(horizontal = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerColor)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(24.dp)
                    .padding(horizontal = 48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
            Row(
                modifier = Modifier.padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(240.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerColor)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(24.dp)
                    .padding(horizontal = 48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
            Row(
                modifier = Modifier.padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(240.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerColor)
                    )
                }
            }
        }
    }
}
