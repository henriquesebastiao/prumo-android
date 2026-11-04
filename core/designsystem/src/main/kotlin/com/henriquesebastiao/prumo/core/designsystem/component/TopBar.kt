package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.R

/**
 * A screen's top app bar with a back button: [large] for the first level (Configurações), small for
 * the levels under it (Conta, Aparência). Pair the large one with an exit-until-collapsed
 * [scrollBehavior]. A [subtitle] goes under the title ("Honda CG 160 Fan · 2023"); a form that
 * discards what was typed shows [close] (✕) instead of the arrow.
 */
@Composable
fun PrumoTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    subtitle: String? = null,
    close: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val navigation: @Composable () -> Unit = { if (onBack != null) BackButton(onBack, close = close) }
    val subtitleText: (@Composable () -> Unit)? = subtitle?.let { { Text(it) } }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
    if (large) {
        LargeFlexibleTopAppBar(
            title = { Text(title) },
            subtitle = subtitleText,
            modifier = modifier,
            navigationIcon = navigation,
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    } else if (subtitleText != null) {
        TopAppBar(
            title = { Text(title) },
            subtitle = subtitleText,
            modifier = modifier,
            navigationIcon = navigation,
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    } else {
        TopAppBar(
            title = { Text(title) },
            modifier = modifier,
            navigationIcon = navigation,
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    }
}

/** The round back button of every screen under the Home; [close] (✕) on forms. */
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier, close: Boolean = false) {
    FilledTonalIconButton(
        onClick = onBack,
        modifier = modifier,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        if (close) {
            Icon(painterResource(PrumoIcons.Close), contentDescription = stringResource(R.string.designsystem_close))
        } else {
            Icon(painterResource(PrumoIcons.ArrowBack), contentDescription = stringResource(R.string.designsystem_back))
        }
    }
}
