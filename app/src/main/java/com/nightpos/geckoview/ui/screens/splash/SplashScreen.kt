package com.nightpos.geckoview.ui.screens.splash

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightpos.geckoview.R
import com.nightpos.geckoview.ui.theme.NeonPink
import com.nightpos.geckoview.ui.theme.NeonPurple
import com.nightpos.geckoview.ui.theme.NightBlack
import com.nightpos.geckoview.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * In-app branded launch screen shown right after the system splash
 * (`androidx.core.splashscreen`) hands off to Compose. Gives the WebView/network
 * stack a brief, intentional moment to warm up while reinforcing the brand with
 * a logo entrance animation, before transitioning to the dashboard.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    minDurationMillis: Long = 1400L,
) {
    var startAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "splash-logo-scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "splash-logo-alpha",
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(minDurationMillis)
        onFinished()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NightBlack,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(alpha = 0.18f), NightBlack),
                        radius = 900f,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .clip(CircleShape)
                        .background(NightBlack),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = stringResource(R.string.content_desc_app_logo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonPink,
                    modifier = Modifier.graphicsLayer { this.alpha = alpha },
                )

                Spacer(modifier = Modifier.height(40.dp))

                CircularProgressIndicator(color = NeonPurple, modifier = Modifier.size(36.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.splash_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}
