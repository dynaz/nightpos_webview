package com.nightpos.app.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightpos.app.ui.theme.NeonPurple
import com.nightpos.app.ui.theme.NightBlack

@Composable
fun PinLoginScreen(
    viewModel: PinLoginViewModel,
    onLoginSuccess: (isManager: Boolean, employeeName: String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.pin) {
        if (state.pin.length == 6) {
            viewModel.verifyPin(onLoginSuccess)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBlack),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 48.dp),
        ) {
            Text(
                text = "NightPOS",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NeonPurple,
            )
            Text(
                text = "Enter PIN",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
            )

            if (state.isConnecting) {
                CircularProgressIndicator(color = NeonPurple)
                Text("Connecting to server...", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            } else if (state.connectError != null) {
                Text(state.connectError!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontSize = 13.sp)
            } else {
                PinDots(pinLength = state.pin.length)

                if (state.error != null) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                if (state.isLoading) {
                    CircularProgressIndicator(color = NeonPurple, modifier = Modifier.size(32.dp))
                } else {
                    PinPad(
                        onDigit = viewModel::onKeyPress,
                        onBackspace = viewModel::onBackspace,
                        onClear = viewModel::onClear,
                    )
                }
            }
        }
    }
}

@Composable
private fun PinDots(pinLength: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(6) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < pinLength) NeonPurple
                        else Color.White.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
private fun PinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    PinKey(
                        label = key,
                        onClick = {
                            when (key) {
                                "⌫" -> onBackspace()
                                "C" -> onClear()
                                else -> onDigit(key)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = Color.White,
        ),
        modifier = Modifier.size(80.dp),
    ) {
        if (label == "⌫") {
            Icon(Icons.Filled.Backspace, contentDescription = "backspace", tint = Color.White)
        } else {
            Text(label, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
    }
}
