package com.nightpos.app.ui.screens.dashboard

import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nightpos.app.R
import com.nightpos.app.ui.components.MenuCard
import com.nightpos.app.ui.theme.AmberAccent
import com.nightpos.app.ui.theme.ErrorRed
import com.nightpos.app.ui.theme.LimeAccent
import com.nightpos.app.ui.theme.NeonCyan
import com.nightpos.app.ui.theme.NeonPink
import com.nightpos.app.ui.theme.NeonPurple
import com.nightpos.app.ui.theme.NeonVioletLight
import com.nightpos.app.ui.theme.NightBlack
import com.nightpos.app.ui.theme.SuccessGreen

private data class DashboardMenuItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: androidx.compose.ui.graphics.Color,
    val action: DashboardAction,
)

/** High-level navigation intents the dashboard can trigger; the host decides how to fulfil them. */
sealed interface DashboardAction {
    data object OpenPos : DashboardAction
    data object OpenReports : DashboardAction
    data object OpenCustomers : DashboardAction
    data object OpenProducts : DashboardAction
    data object OpenDiscountLoyalty : DashboardAction
    data object OpenGiftCards : DashboardAction
    data object OpenSettings : DashboardAction
    data object Logout : DashboardAction
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    sharedWebView: WebView?,
    onAction: (DashboardAction) -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.logoutCompleted) {
        if (uiState.logoutCompleted) {
            viewModel.consumeLogoutCompleted()
            onLoggedOut()
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            DashboardHeader()

            Spacer(modifier = Modifier.size(24.dp))

            val items = listOf(
                DashboardMenuItem(
                    title = stringResource(R.string.menu_open_pos),
                    description = stringResource(R.string.menu_open_pos_desc),
                    icon = Icons.Filled.ShoppingCart,
                    accent = NeonPurple,
                    action = DashboardAction.OpenPos,
                ),
                DashboardMenuItem(
                    title = stringResource(R.string.menu_reports),
                    description = stringResource(R.string.menu_reports_desc),
                    icon = Icons.Filled.Assessment,
                    accent = AmberAccent,
                    action = DashboardAction.OpenReports,
                ),
                DashboardMenuItem(
                    title = stringResource(R.string.menu_customers),
                    description = stringResource(R.string.menu_customers_desc),
                    icon = Icons.Filled.People,
                    accent = SuccessGreen,
                    action = DashboardAction.OpenCustomers,
                ),
                DashboardMenuItem(
                    title = stringResource(R.string.menu_products),
                    description = stringResource(R.string.menu_products_desc),
                    icon = Icons.Filled.Category,
                    accent = NeonCyan,
                    action = DashboardAction.OpenProducts,
                ),
                DashboardMenuItem(
                    title = stringResource(R.string.menu_discount_loyalty),
                    description = stringResource(R.string.menu_discount_loyalty_desc),
                    icon = Icons.Filled.LocalOffer,
                    accent = NeonVioletLight,
                    action = DashboardAction.OpenDiscountLoyalty,
                ),
                DashboardMenuItem(
                    title = stringResource(R.string.menu_gift_cards),
                    description = stringResource(R.string.menu_gift_cards_desc),
                    icon = Icons.Filled.CardGiftcard,
                    accent = LimeAccent,
                    action = DashboardAction.OpenGiftCards,
                ),
                DashboardMenuItem(
                    title = stringResource(R.string.menu_settings),
                    description = stringResource(R.string.menu_settings_desc),
                    icon = Icons.Filled.Settings,
                    accent = NeonPink,
                    action = DashboardAction.OpenSettings,
                ),
                DashboardMenuItem(
                    title = stringResource(R.string.menu_logout),
                    description = stringResource(R.string.menu_logout_desc),
                    icon = Icons.Filled.Logout,
                    accent = ErrorRed,
                    action = DashboardAction.Logout,
                ),
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 260.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items) { item ->
                    MenuCard(
                        title = item.title,
                        description = item.description,
                        icon = item.icon,
                        accentColor = item.accent,
                        onClick = {
                            if (item.action is DashboardAction.Logout) {
                                viewModel.requestLogout()
                            } else {
                                onAction(item.action)
                            }
                        },
                    )
                }
            }
        }
    }

    if (uiState.showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = { viewModel.confirmLogout(sharedWebView) },
            onDismiss = { viewModel.dismissLogoutDialog() },
        )
    }

    if (uiState.isLoggingOut) {
        LogoutProgressOverlay()
    }
}

@Composable
private fun DashboardHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = NightBlack,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
            modifier = Modifier.size(72.dp),
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
        Spacer(modifier = Modifier.size(20.dp))
        Column {
            Text(
                text = stringResource(R.string.dashboard_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.dashboard_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.logout_dialog_title)) },
        text = { Text(stringResource(R.string.logout_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.logout_confirm), color = ErrorRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.logout_cancel)) }
        },
    )
}

@Composable
private fun LogoutProgressOverlay() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NightBlack.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = NeonPurple)
            Spacer(modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.logout_dialog_title), color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
