package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.henriquesebastiao.prumo.core.designsystem.R
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme

/**
 * Asks before something that ends or removes: sign out, revoke a session, delete, archive. The
 * [body] says what goes and what stays. A [destructive] action is in the error color; archiving is
 * not destructive and stays in primary. [dismissLabel] defaults to "Cancelar".
 */
@Composable
fun ConfirmDialog(
    icon: Int,
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    dismissLabel: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(icon), contentDescription = null) },
        title = { Text(title, textAlign = TextAlign.Center) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel ?: stringResource(R.string.designsystem_cancel)) }
        },
        modifier = modifier,
    )
}

@PhonePreviews
@Composable
private fun ConfirmDialogPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            ConfirmDialog(
                icon = PrumoIcons.Logout,
                title = "Sair do Prumo?",
                body = "Você vai precisar entrar de novo neste aparelho. Nada é apagado do servidor.",
                confirmLabel = "Sair",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
