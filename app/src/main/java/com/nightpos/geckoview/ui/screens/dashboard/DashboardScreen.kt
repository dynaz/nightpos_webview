package com.nightpos.geckoview.ui.screens.dashboard

import org.mozilla.geckoview.GeckoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.nightpos.geckoview.R
import com.nightpos.geckoview.ui.components.FALLBACK_OUTLETS
import com.nightpos.geckoview.ui.components.MenuCard
import com.nightpos.geckoview.ui.components.PosOutletFab
import com.nightpos.geckoview.ui.components.toOutlets
import com.nightpos.geckoview.ui.theme.AmberAccent
import com.nightpos.geckoview.ui.theme.ErrorRed
import com.nightpos.geckoview.ui.theme.LimeAccent
import com.nightpos.geckoview.ui.theme.NeonCyan
import com.nightpos.geckoview.ui.theme.NeonPink
import com.nightpos.geckoview.ui.theme.NeonPurple
import com.nightpos.geckoview.ui.theme.NeonPurpleDark
import com.nightpos.geckoview.ui.theme.NeonVioletLight
import com.nightpos.geckoview.ui.theme.NightBlack
import com.nightpos.geckoview.ui.theme.OrangeAccent
import com.nightpos.geckoview.ui.theme.SuccessGreen

private data class DashboardMenuItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: androidx.compose.ui.graphics.Color,
    val action: DashboardAction,
)

/** High-level navigation intents the dashboard can trigger; the host decides how to fulfil them. */
sealed interface DashboardAction {
    data object OpenNposHome : DashboardAction
    data object OpenPos : DashboardAction
    data object OpenReports : DashboardAction
    data object OpenCustomers : DashboardAction
    data object OpenProducts : DashboardAction
    data object OpenDiscountLoyalty : DashboardAction
    data object OpenGiftCards : DashboardAction
    data object OpenEmployees : DashboardAction
    data object OpenPrinters : DashboardAction
    data object OpenPosSettings : DashboardAction
    data object OpenSettings : DashboardAction
    data object Logout : DashboardAction
    data class OpenPosOutlet(val url: String, val name: String) : DashboardAction
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    sharedGeckoView: GeckoView?,
    onAction: (DashboardAction) -> Unit,
    onLoggedOut: () -> Unit,
    baseUrl: String = "https://soho.nightpos.com",
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.logoutCompleted) {
        if (uiState.logoutCompleted) {
            viewModel.consumeLogoutCompleted()
            onLoggedOut()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            DashboardHeader(
                onLogoClick = { onAction(DashboardAction.OpenNposHome) },
                userName = uiState.displayName,
            )

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
                    title = stringResource(R.string.menu_employees),
                    description = stringResource(R.string.menu_employees_desc),
                    icon = Icons.Filled.Badge,
                    accent = OrangeAccent,
                    action = DashboardAction.OpenEmployees,
                ),
                DashboardMenuItem(
                    title = stringResource(R.string.menu_printers),
                    description = stringResource(R.string.menu_printers_desc),
                    icon = Icons.Filled.Print,
                    accent = NeonCyan,
                    action = DashboardAction.OpenPrinters,
                ),
                DashboardMenuItem(
                    title = stringResource(R.string.menu_pos_settings),
                    description = stringResource(R.string.menu_pos_settings_desc),
                    icon = Icons.Filled.Tune,
                    accent = NeonPurpleDark,
                    action = DashboardAction.OpenPosSettings,
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

    // ── Open POS floating action button with outlet radial menu ──────────────
    val outlets = remember(uiState.posConfigs, baseUrl) {
        uiState.posConfigs.toOutlets(baseUrl).ifEmpty { FALLBACK_OUTLETS }
    }
    PosOutletFab(
        outlets = outlets,
        onOutletSelected = { outlet -> onAction(DashboardAction.OpenPosOutlet(outlet.url, outlet.name)) },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 24.dp, bottom = 24.dp),
    )

    } // end outer Box

    if (uiState.showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = { viewModel.confirmLogout(sharedGeckoView?.session) },
            onDismiss = { viewModel.dismissLogoutDialog() },
        )
    }

    if (uiState.isLoggingOut) {
        LogoutProgressOverlay()
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(onLogoClick: () -> Unit, userName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // ── Left: logo + title ─────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = NightBlack,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
                modifier = Modifier
                    .size(72.dp)
                    .clickable(onClick = onLogoClick),
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

        // ── Right: username + digital clock ────────────────────────────────
        Column(horizontalAlignment = Alignment.End) {
            if (userName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(modifier = Modifier.size(4.dp))
            }
            DigitalClock()
        }
    }
}

// SimpleDateFormat is used (not java.time) so this works on API 21+ (Sunmi D2s = API 25).
private val clockFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

@Composable
private fun DigitalClock() {
    var time by remember { mutableStateOf(clockFormatter.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            time = clockFormatter.format(Date())
        }
    }
    Text(
        text = time,
        style = MaterialTheme.typography.titleLarge,
        color = NeonCyan,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
    )
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
