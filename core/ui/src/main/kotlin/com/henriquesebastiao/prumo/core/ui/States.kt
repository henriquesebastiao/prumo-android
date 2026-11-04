package com.henriquesebastiao.prumo.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.ErrorCard
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.UnreachableCause

/** A short wait: Material's contained loading indicator, centered, with what is loading under it. */
@Composable
fun LoadingState(label: String, modifier: Modifier = Modifier) {
    val loading = stringResource(R.string.ui_loading)
    Box(modifier.fillMaxSize().semantics { contentDescription = loading }, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            ContainedLoadingIndicator(Modifier.size(64.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * A screen's content failed to load: what happened and what to check, with "Tentar novamente".
 * A server that didn't answer gets the "Servidor inacessível" card instead.
 */
@Composable
fun ErrorState(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    server: String? = null,
    title: String? = null,
) {
    Box(modifier.fillMaxSize().padding(Spacing.screen), contentAlignment = Alignment.TopCenter) {
        if (error is AppError.Unreachable) {
            ServerUnreachableCard(server, onRetry)
        } else {
            ErrorCard(
                title = title ?: errorTitle(error),
                body = errorText(error, server),
                retryLabel = stringResource(R.string.ui_retry),
                onRetry = onRetry,
            )
        }
    }
}

/** The device is online but the server didn't answer: different from "no connection". */
@Composable
fun ServerUnreachableCard(server: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    ErrorCard(
        title = stringResource(R.string.ui_server_unreachable_title),
        body = stringResource(R.string.ui_server_unreachable_body, server ?: stringResource(R.string.ui_this_server)),
        retryLabel = stringResource(R.string.ui_retry),
        onRetry = onRetry,
        icon = PrumoIcons.CloudAlert,
        modifier = modifier,
    )
}

/**
 * The server refused this device's token (revoked from the web, expired): sign in again, on the
 * same server. "Agora não" leaves the app signed out on the login screen's way.
 */
@Composable
fun SessionExpiredDialog(onSignIn: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    ConfirmDialog(
        icon = PrumoIcons.LockClock,
        title = stringResource(R.string.ui_session_expired_title),
        body = stringResource(R.string.ui_session_expired_body),
        confirmLabel = stringResource(R.string.ui_session_expired_sign_in),
        onConfirm = onSignIn,
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.ui_session_expired_later),
        modifier = modifier,
    )
}

@PhonePreviews
@Composable
private fun StatesPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                ErrorState(AppError.Unexpected(500), onRetry = {
                }, modifier = Modifier.size(width = 412.dp, height = 220.dp))
                ErrorState(
                    AppError.Unreachable(UnreachableCause.Timeout),
                    onRetry = {},
                    server = "prumo.minhacasa.net",
                    modifier = Modifier.size(width = 412.dp, height = 220.dp),
                )
                LoadingState("Carregando o Início", Modifier.size(width = 412.dp, height = 200.dp))
            }
        }
    }
}
