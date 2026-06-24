package com.nightpos.app.ui.screens.login

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nightpos.app.R
import com.nightpos.app.ui.theme.ErrorRed
import com.nightpos.app.ui.theme.NeonPink
import com.nightpos.app.ui.theme.NeonPurple
import com.nightpos.app.ui.theme.NeonPurpleDark
import com.nightpos.app.ui.theme.NightBlack
import com.nightpos.app.ui.theme.NightSurface
import com.nightpos.app.ui.theme.NightSurfaceVariant
import com.nightpos.app.ui.theme.TextPrimary
import com.nightpos.app.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onConnected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var startAnimation by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "login-logo-scale",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "login-content-alpha",
    )

    LaunchedEffect(Unit) { startAnimation = true }

    Surface(modifier = modifier.fillMaxSize(), color = NightBlack) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(alpha = 0.15f), NightBlack),
                        radius = 1000f,
                    ),
                )
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer { scaleX = logoScale; scaleY = logoScale }
                        .clip(CircleShape)
                        .background(NightBlack),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = stringResource(R.string.content_desc_app_logo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonPink,
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha },
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Connect to your server",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha },
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Server URL field
                OutlinedTextField(
                    value = uiState.serverUrl,
                    onValueChange = viewModel::onUrlChanged,
                    label = { Text("Server URL", color = TextSecondary) },
                    placeholder = { Text("https://yourstore.nightpos.com", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    isError = uiState.error != null,
                    supportingText = uiState.error?.let { { Text(it, color = ErrorRed) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { viewModel.connect(onConnected) },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = NightSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonPurple,
                        focusedContainerColor = NightSurface,
                        unfocusedContainerColor = NightSurface,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = contentAlpha },
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Connect button
                Button(
                    onClick = { viewModel.connect(onConnected) },
                    enabled = !uiState.isConnecting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple,
                        contentColor = TextPrimary,
                        disabledContainerColor = NeonPurpleDark,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .graphicsLayer { alpha = contentAlpha },
                ) {
                    if (uiState.isConnecting) {
                        CircularProgressIndicator(
                            color = TextPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(
                            text = "Connect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
