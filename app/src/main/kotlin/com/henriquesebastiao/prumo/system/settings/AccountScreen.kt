package com.henriquesebastiao.prumo.system.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.R
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.GroupNote
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.getOrNull
import com.henriquesebastiao.prumo.core.ui.errorText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the account screen shows besides the fields; [saved] is the message of the last save. */
data class AccountUiState(
    val username: String = "",
    val saving: Boolean = false,
    val nameError: AppError? = null,
    val passwordError: AppError? = null,
    val saved: AccountSaved? = null,
)

enum class AccountSaved { Name, Password }

@HiltViewModel
class AccountViewModel @Inject constructor(private val account: AccountRepository) : ViewModel() {
    var name by mutableStateOf(account.user.value?.displayName.orEmpty())
        private set
    var currentPassword by mutableStateOf("")
        private set
    var newPassword by mutableStateOf("")
        private set
    var confirmPassword by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(AccountUiState(username = account.user.value?.username.orEmpty()))
    val state: StateFlow<AccountUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            account.refreshUser().getOrNull()?.let { user ->
                if (name.isEmpty()) name = user.displayName
                mutableState.update { it.copy(username = user.username) }
            }
        }
    }

    fun onNameChange(value: String) {
        name = value
        mutableState.update { it.copy(nameError = null, saved = null) }
    }

    fun onCurrentPasswordChange(value: String) {
        currentPassword = value
        mutableState.update { it.copy(passwordError = null, saved = null) }
    }

    fun onNewPasswordChange(value: String) {
        newPassword = value
        mutableState.update { it.copy(passwordError = null, saved = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        confirmPassword = value
        mutableState.update { it.copy(passwordError = null, saved = null) }
    }

    /** Whether the two new passwords differ (checked before sending; the rules are the server's). */
    val passwordsDiffer: Boolean get() = confirmPassword.isNotEmpty() && confirmPassword != newPassword

    fun saveName() {
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val outcome = account.updateName(name)) {
                is Outcome.Ok -> mutableState.update { it.copy(saving = false, saved = AccountSaved.Name) }
                is Outcome.Failed -> mutableState.update { it.copy(saving = false, nameError = outcome.error) }
            }
        }
    }

    fun savePassword() {
        if (passwordsDiffer || currentPassword.isEmpty() || newPassword.isEmpty()) return
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val outcome = account.changePassword(currentPassword, newPassword)) {
                is Outcome.Ok -> {
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                    mutableState.update { it.copy(saving = false, saved = AccountSaved.Password) }
                }

                is Outcome.Failed -> mutableState.update { it.copy(saving = false, passwordError = outcome.error) }
            }
        }
    }
}

@Composable
fun AccountRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: AccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountScreen(
        state = state,
        fields = AccountFields(
            viewModel.name,
            viewModel.currentPassword,
            viewModel.newPassword,
            viewModel.confirmPassword,
            viewModel.passwordsDiffer,
        ),
        onNameChange = viewModel::onNameChange,
        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSaveName = viewModel::saveName,
        onSavePassword = viewModel::savePassword,
        onBack = onBack,
        modifier = modifier,
    )
}

/** The typed texts of the account screen. */
data class AccountFields(
    val name: String,
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String,
    val passwordsDiffer: Boolean,
)

/** Settings › Conta: the display name and a new password. The username is the server's. */
@Composable
fun AccountScreen(
    state: AccountUiState,
    fields: AccountFields,
    onNameChange: (String) -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onSavePassword: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val nameSaved = stringResource(R.string.account_name_saved)
    val passwordSaved = stringResource(R.string.account_password_saved)
    LaunchedEffect(state.saved) {
        when (state.saved) {
            AccountSaved.Name -> snackbar.showSnackbar(nameSaved)
            AccountSaved.Password -> snackbar.showSnackbar(passwordSaved)
            null -> Unit
        }
    }
    Scaffold(
        modifier,
        topBar = { PrumoTopBar(stringResource(R.string.account_title), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionHeader(stringResource(R.string.account_profile))
            OutlinedTextField(
                fields.name,
                onNameChange,
                label = { Text(stringResource(R.string.account_name)) },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(errorText(it)) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                state.username,
                {},
                label = { Text(stringResource(R.string.account_username)) },
                enabled = false,
                supportingText = { Text(stringResource(R.string.account_username_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSaveName,
                enabled = fields.name.isNotBlank() && !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.account_save_name))
            }
            SectionHeader(stringResource(R.string.account_password))
            PasswordField(
                fields.currentPassword,
                onCurrentPasswordChange,
                stringResource(R.string.account_current_password),
            )
            PasswordField(
                fields.newPassword,
                onNewPasswordChange,
                stringResource(R.string.account_new_password),
                supporting =
                    state.passwordError?.let { errorText(it) } ?: stringResource(R.string.account_new_password_hint),
                isError = state.passwordError != null,
            )
            PasswordField(
                fields.confirmPassword,
                onConfirmPasswordChange,
                stringResource(R.string.account_confirm_password),
                supporting = if (fields.passwordsDiffer) stringResource(R.string.account_passwords_differ) else null,
                isError = fields.passwordsDiffer,
            )
            Button(
                onClick = onSavePassword,
                enabled = !state.saving && !fields.passwordsDiffer && fields.currentPassword.isNotEmpty() &&
                    fields.newPassword.isNotEmpty() && fields.confirmPassword.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.account_save_password))
            }
            GroupNote(stringResource(R.string.account_password_note))
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supporting: String? = null,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value,
        onValueChange,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        supportingText = supporting?.let { { Text(it) } },
        isError = isError,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@PhonePreviews
@Composable
private fun AccountPreview() {
    PrumoTheme(dynamicColor = false) {
        AccountScreen(
            state = AccountUiState(username = "marina"),
            fields = AccountFields("Marina Costa", "", "", "", passwordsDiffer = false),
            onNameChange = {},
            onCurrentPasswordChange = {},
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onSaveName = {},
            onSavePassword = {},
            onBack = {},
        )
    }
}
