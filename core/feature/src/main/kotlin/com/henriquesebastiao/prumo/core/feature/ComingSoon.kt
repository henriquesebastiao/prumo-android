package com.henriquesebastiao.prumo.core.feature

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme

/**
 * A module's place while its screens aren't built (the foundation): its name, in its colors, and
 * that it comes next. Each `PrumoFeature` replaces it with its screens in its phase.
 */
@Composable
fun ComingSoonScreen(name: String, icon: Int, seed: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ModuleTheme(seed) {
        Scaffold(modifier, topBar = { PrumoTopBar(title = name, onBack = onBack, large = true) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = icon,
                    title = stringResource(R.string.feature_coming_soon_title, name),
                    body = stringResource(R.string.feature_coming_soon_body),
                    hero = true,
                )
            }
        }
    }
}

@PhonePreviews
@Composable
private fun ComingSoonPreview() {
    PrumoTheme(dynamicColor = false) {
        ComingSoonScreen("Veículos", PrumoIcons.TwoWheeler, 0xFF1F5F8B.toInt(), onBack = {})
    }
}
