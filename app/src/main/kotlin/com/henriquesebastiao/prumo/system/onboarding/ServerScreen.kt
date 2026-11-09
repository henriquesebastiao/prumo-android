package com.henriquesebastiao.prumo.system.onboarding

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henriquesebastiao.prumo.R
import com.henriquesebastiao.prumo.core.designsystem.component.ErrorCard
import com.henriquesebastiao.prumo.core.designsystem.component.IconAvatar
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.OnboardingFooter
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
import com.henriquesebastiao.prumo.core.designsystem.component.StatusTone
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.ServerAddress
import com.henriquesebastiao.prumo.core.model.ServerCheck
import com.henriquesebastiao.prumo.core.model.UnreachableCause
import com.henriquesebastiao.prumo.core.ui.R as UiR
import com.henriquesebastiao.prumo.core.ui.unreachableShort
import com.henriquesebastiao.prumo.core.ui.unreachableText

@Composable
fun ServerRoute(
    onContinue: (baseUrl: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var localNetworkDenied by rememberSaveable { mutableStateOf(false) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        localNetworkDenied = !granted
        viewModel.test()
    }
    // Android 17+ keeps apps off the home network without ACCESS_LOCAL_NETWORK: ask before the
    // first test (a typed 192.168.x.x or a name the home router resolves both need it).
    val test = {
        val needed = Build.VERSION.SDK_INT >= LOCAL_NETWORK_SDK &&
            ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
        if (needed && !asked) {
            asked = true
            permission.launch(ACCESS_LOCAL_NETWORK)
        } else {
            viewModel.test()
        }
    }
    ServerScreen(
        address = viewModel.address,
        state = state,
        localNetworkDenied = localNetworkDenied,
        onAddressChange = viewModel::onAddressChange,
        onTest = test,
        onContinue = { state.found?.let { onContinue(it.baseUrl) } },
        modifier = modifier,
    )
}

/** First run, step 2: where the user's Prumo is. "Continuar" opens with a Prumo server found. */
@Composable
fun ServerScreen(
    address: String,
    state: ServerUiState,
    localNetworkDenied: Boolean,
    onAddressChange: (String) -> Unit,
    onTest: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Testing hides the keyboard, so the result and "Continuar" show (the test also runs when the
    // field loses focus).
    val keyboard = LocalSoftwareKeyboardController.current
    val testAndHide = {
        keyboard?.hide()
        onTest()
    }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                IconAvatar(
                    PrumoIcons.Dns,
                    size = 64.dp,
                    iconSize = 32.dp,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                )
                Text(
                    stringResource(R.string.server_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.server_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AddressField(address, state, onAddressChange, testAndHide)
                if (state.check == null && !state.checking) {
                    TextButton(onClick = testAndHide, enabled = address.isNotBlank()) {
                        Icon(painterResource(PrumoIcons.NetworkCheck), contentDescription = null)
                        Text(stringResource(R.string.server_test), Modifier.padding(start = Spacing.sm))
                    }
                }
                ServerResult(state, testAndHide)
                if (localNetworkDenied) {
                    Text(
                        stringResource(R.string.server_local_network_denied),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            OnboardingFooter(
                step = 2,
                label = stringResource(R.string.onboarding_step, 2, STEPS),
                nextLabel = stringResource(R.string.server_continue),
                onNext = onContinue,
                enabled = state.found != null,
                busy = state.checking,
            )
        }
    }
}

@Composable
private fun AddressField(address: String, state: ServerUiState, onAddressChange: (String) -> Unit, onTest: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val check = state.check
    val untested = address.isNotBlank() && check == null && !state.checking
    val error = when (check) {
        is ServerCheck.Unreachable -> unreachableShort(check.cause)
        is ServerCheck.NotPrumo -> stringResource(R.string.server_not_prumo_title)
        ServerCheck.InvalidAddress -> stringResource(R.string.server_invalid_address)
        else -> null
    }
    OutlinedTextField(
        value = address,
        onValueChange = onAddressChange,
        label = { Text(stringResource(R.string.server_field)) },
        leadingIcon = { Icon(painterResource(PrumoIcons.Link), contentDescription = null) },
        trailingIcon = if (address.isNotEmpty()) {
            {
                IconButton(onClick = { onAddressChange("") }) {
                    Icon(painterResource(PrumoIcons.Close), contentDescription = stringResource(R.string.server_clear))
                }
            }
        } else {
            null
        },
        textStyle = PrumoType.dataMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
        supportingText = { Text(error ?: stringResource(R.string.server_field_hint)) },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onGo = { onTest() }),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                // The test runs when the field loses focus with something typed.
                if (focused && !it.isFocused && untested) onTest()
                focused = it.isFocused
            },
    )
}

@Composable
private fun ServerResult(state: ServerUiState, onTest: () -> Unit) {
    val retry = stringResource(R.string.server_test_again)
    when (val check = state.check) {
        null -> if (state.checking) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                LoadingIndicator(Modifier.padding(Spacing.xs))
                Text(stringResource(R.string.server_testing), style = MaterialTheme.typography.bodyMedium)
            }
        }

        is ServerCheck.Found -> Found(check, onTest)

        is ServerCheck.Unreachable -> ErrorCard(
            stringResource(UiR.string.ui_unreachable_title),
            unreachableText(check.cause, hostOf(check.baseUrl)),
            retryLabel = retry,
            onRetry = onTest,
        )

        is ServerCheck.NotPrumo -> ErrorCard(
            stringResource(R.string.server_not_prumo_title),
            stringResource(R.string.server_not_prumo_body, ServerAddress.display(check.baseUrl)),
            retryLabel = retry,
            onRetry = onTest,
        )

        is ServerCheck.DatabaseDown -> ErrorCard(
            stringResource(R.string.server_database_down_title),
            stringResource(R.string.server_database_down_body, check.version),
            retryLabel = retry,
            onRetry = onTest,
        )

        ServerCheck.InvalidAddress -> Unit
    }
}

@Composable
private fun Found(check: ServerCheck.Found, onTest: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(stringResource(R.string.server_found), StatusTone.Success, Modifier.weight(1f, fill = false))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onTest) {
                    Icon(painterResource(PrumoIcons.Refresh), contentDescription = null)
                    Text(stringResource(R.string.server_test), Modifier.padding(start = Spacing.xs))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            KpiTile(
                stringResource(R.string.server_version_value, check.version),
                stringResource(R.string.server_version),
                Modifier.weight(1f),
            )
            KpiTile(
                stringResource(R.string.server_latency_value, check.latencyMillis),
                stringResource(R.string.server_latency),
                Modifier.weight(1f),
            )
        }
        if (ServerAddress.isInsecurePublic(check.baseUrl)) {
            Text(
                stringResource(R.string.server_insecure),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** The host of [baseUrl] as typed, for the "no answer" texts. */
internal fun hostOf(baseUrl: String): String = ServerAddress.display(baseUrl).substringBefore('/')

private const val LOCAL_NETWORK_SDK = 37
private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

@PhonePreviews
@Composable
private fun ServerFoundPreview() {
    PrumoTheme(dynamicColor = false) {
        ServerScreen(
            address = "https://prumo.minhacasa.net",
            state = ServerUiState(check = ServerCheck.Found("https://prumo.minhacasa.net", "1.0.2", 84)),
            localNetworkDenied = false,
            onAddressChange = {},
            onTest = {},
            onContinue = {},
        )
    }
}

@PhonePreviews
@Composable
private fun ServerErrorPreview() {
    PrumoTheme(dynamicColor = false) {
        ServerScreen(
            address = "https://prumo.minhacasa.nte",
            state = ServerUiState(
                check = ServerCheck.Unreachable(
                    "https://prumo.minhacasa.nte",
                    UnreachableCause.UnknownHost,
                ),
            ),
            localNetworkDenied = false,
            onAddressChange = {},
            onTest = {},
            onContinue = {},
        )
    }
}
