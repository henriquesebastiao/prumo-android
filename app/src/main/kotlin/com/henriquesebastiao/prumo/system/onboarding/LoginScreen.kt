package com.henriquesebastiao.prumo.system.onboarding

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henriquesebastiao.prumo.R
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.OnboardingFooter
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.ErrorCodes
import com.henriquesebastiao.prumo.core.model.ServerAddress
import com.henriquesebastiao.prumo.core.ui.errorText

@Composable
fun LoginRoute(
    baseUrl: String,
    onChangeServer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(
        server = ServerAddress.display(baseUrl),
        username = viewModel.username,
        password = viewModel.password,
        state = state,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = { viewModel.signIn(baseUrl) },
        onChangeServer = onChangeServer,
        modifier = modifier,
    )
}

/** First run, step 3: the instance's username and password. A credential error shows on the password. */
@Composable
fun LoginScreen(
    server: String,
    username: String,
    password: String,
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onChangeServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
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
                FitText(
                    stringResource(R.string.login_title),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Tag(server, Modifier.weight(1f, fill = false), icon = PrumoIcons.Dns, small = true)
                    TextButton(onClick = onChangeServer) { Text(stringResource(R.string.login_change_server)) }
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.login_username)) },
                    leadingIcon = { Icon(painterResource(PrumoIcons.Person), contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
                )
                val error = state.error
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.login_password)) },
                    leadingIcon = { Icon(painterResource(PrumoIcons.Lock), contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                painterResource(if (visible) PrumoIcons.VisibilityOff else PrumoIcons.Visibility),
                                contentDescription = stringResource(
                                    if (visible) R.string.login_hide_password else R.string.login_show_password,
                                ),
                            )
                        }
                    },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = error != null,
                    supportingText = error?.let { { Text(errorText(it, server)) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
                )
            }
            OnboardingFooter(
                step = 3,
                label = stringResource(R.string.onboarding_step, 3, STEPS),
                nextLabel = stringResource(R.string.login_submit),
                onNext = onSubmit,
                icon = PrumoIcons.Login,
                enabled = username.isNotBlank() && password.isNotEmpty(),
                busy = state.submitting,
            )
        }
    }
}

@PhonePreviews
@Composable
private fun LoginPreview() {
    PrumoTheme(dynamicColor = false) {
        LoginScreen(
            server = "prumo.minhacasa.net",
            username = "marina",
            password = "",
            state = LoginUiState(error = AppError.Api(401, ErrorCodes.AUTH_INVALID_CREDENTIALS)),
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onChangeServer = {},
        )
    }
}
