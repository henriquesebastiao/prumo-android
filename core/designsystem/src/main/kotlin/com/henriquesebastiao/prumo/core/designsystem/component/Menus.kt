package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.R

/** One option of a ⋮ menu. */
data class MenuOption(val label: String, val icon: Int, val onClick: () -> Unit)

/** The ⋮ button of a top bar or a row, and its menu. */
@Composable
fun OverflowMenu(options: List<MenuOption>, modifier: Modifier = Modifier, label: String? = null) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { open = true }) {
            Icon(
                painterResource(PrumoIcons.MoreVert),
                contentDescription = label ?: stringResource(R.string.designsystem_more),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        open = false
                        option.onClick()
                    },
                    leadingIcon = { Icon(painterResource(option.icon), contentDescription = null) },
                )
            }
        }
    }
}
