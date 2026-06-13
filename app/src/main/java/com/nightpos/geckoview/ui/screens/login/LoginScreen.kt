package com.nightpos.geckoview.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightpos.geckoview.R
import com.nightpos.geckoview.ui.components.FALLBACK_OUTLETS
import com.nightpos.geckoview.ui.components.LanguageSwitcherButton
import com.nightpos.geckoview.ui.components.PosOutletFab
import com.nightpos.geckoview.ui.components.toOutlets
import com.nightpos.geckoview.ui.theme.ErrorRed
import com.nightpos.geckoview.ui.theme.NeonPink
import com.nightpos.geckoview.ui.theme.NeonPurple
import com.nightpos.geckoview.ui.theme.NightBlack
import com.nightpos.geckoview.ui.theme.TextSecondary

/**
 * First screen shown when the app launches and no session is stored. Authenticates against
 * Odoo via [LoginViewModel] (PIN or Normal/password), and also exposes the Settings/language
 * shortcuts and the "Open POS" outlet FAB so staff can jump straight into a till without
 * signing in to the back office.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    baseUrl: String,
    onLoginSuccess: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPosOutlet: (url: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            viewModel.consumeLoginSuccess()
            onLoginSuccess()
        }
    }

    val outlets = remember(uiState.posConfigs, baseUrl) {
        uiState.posConfigs.toOutlets(baseUrl).ifEmpty { FALLBACK_OUTLETS }
    }

    Surface(modifier = modifier.fillMaxSize(), color = NightBlack) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(alpha = 0.18f), NightBlack),
                        radius = 900f,
                    ),
                ),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                LanguageSwitcherButton()
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.menu_settings),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonPink,
                )
                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(18.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.mode == LoginMode.PIN,
                        onClick = { viewModel.setMode(LoginMode.PIN) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text(stringResource(R.string.login_tab_pin))
                    }
                    SegmentedButton(
                        selected = uiState.mode == LoginMode.NORMAL,
                        onClick = { viewModel.setMode(LoginMode.NORMAL) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text(stringResource(R.string.login_tab_normal))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (uiState.mode) {
                    LoginMode.NORMAL -> NormalLoginForm(
                        uiState = uiState,
                        viewModel = viewModel,
                        focusManager = focusManager,
                        onSubmit = { viewModel.submit(baseUrl) },
                    )
                    LoginMode.PIN -> PinLoginForm(uiState = uiState, viewModel = viewModel)
                }

                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = loginErrorMessage(error),
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.submit(baseUrl)
                    },
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.login_button_signin), fontWeight = FontWeight.Bold)
                    }
                }
            }

            PosOutletFab(
                outlets = outlets,
                onOutletSelected = { outlet -> onOpenPosOutlet(outlet.url, outlet.name) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun NormalLoginForm(
    uiState: LoginUiState,
    viewModel: LoginViewModel,
    focusManager: FocusManager,
    onSubmit: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = uiState.login,
            onValueChange = viewModel::setLogin,
            label = { Text(stringResource(R.string.login_field_username)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::setPassword,
            label = { Text(stringResource(R.string.login_field_password)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onSubmit()
            }),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(R.string.content_desc_password_toggle),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val PinKeySize = 48.dp

@Composable
private fun PinLoginForm(uiState: LoginUiState, viewModel: LoginViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.login_signed_in_as, uiState.login.ifBlank { "-" }),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { viewModel.switchUser() }) {
                Text(stringResource(R.string.login_switch_user), color = NeonPurple)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (uiState.pin.isEmpty()) " " else "•".repeat(uiState.pin.length),
            style = MaterialTheme.typography.headlineMedium,
            letterSpacing = 8.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        val rows = listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9'),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { digit ->
                        PinKey(onClick = { viewModel.appendPinDigit(digit) }) {
                            Text(digit.toString(), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(modifier = Modifier.size(PinKeySize))
                PinKey(onClick = { viewModel.appendPinDigit('0') }) {
                    Text("0", style = MaterialTheme.typography.titleLarge)
                }
                PinKey(onClick = { viewModel.backspacePin() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.content_desc_pin_backspace),
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKey(onClick: () -> Unit, content: @Composable () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(PinKeySize),
    ) {
        content()
    }
}

@Composable
private fun loginErrorMessage(error: LoginError): String = when (error) {
    LoginError.EMPTY_FIELDS -> stringResource(R.string.login_error_empty)
    LoginError.INVALID_CREDENTIALS -> stringResource(R.string.login_error_invalid_credentials)
    LoginError.NETWORK -> stringResource(R.string.login_error_network)
}
