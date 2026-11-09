package com.henriquesebastiao.prumo.system.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.R
import com.henriquesebastiao.prumo.core.designsystem.color.Seeds
import com.henriquesebastiao.prumo.core.designsystem.component.HeroHeader
import com.henriquesebastiao.prumo.core.designsystem.component.IconAvatar
import com.henriquesebastiao.prumo.core.designsystem.component.OnboardingFooter
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/** First run, step 1: the wide title and Material shapes in the colors of the three modules. */
@Composable
fun WelcomeScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().statusBarsPadding().padding(horizontal = Spacing.sm),
                // 40 dp between the shapes and the title: 16 here plus the header's own 24.
                verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ModuleShapes()
                HeroHeader(
                    stringResource(R.string.app_name),
                    subtitle = stringResource(R.string.welcome_body),
                    centered = true,
                )
            }
            OnboardingFooter(
                step = 1,
                label = stringResource(R.string.onboarding_step, 1, STEPS),
                nextLabel = stringResource(R.string.welcome_start),
                onNext = onStart,
                emphasized = true,
            )
        }
    }
}

/** Three avatars, one per module, each in its module's colors and a Material shape. */
@Composable
private fun ModuleShapes(modifier: Modifier = Modifier) {
    Box(modifier.width(300.dp).height(250.dp)) {
        ModuleTheme(Seeds.FINANCE) {
            IconAvatar(
                PrumoIcons.AccountBalanceWallet,
                Modifier.offset(x = 8.dp, y = 0.dp),
                size = 128.dp,
                iconSize = 52.dp,
                shape = MaterialShapes.Cookie9Sided.toShape(),
            )
        }
        ModuleTheme(Seeds.GARAGE) {
            IconAvatar(
                PrumoIcons.TwoWheeler,
                Modifier.align(Alignment.TopEnd).offset(y = 48.dp),
                size = 112.dp,
                iconSize = 44.dp,
                shape = MaterialShapes.Sunny.toShape(),
            )
        }
        ModuleTheme(Seeds.READING) {
            IconAvatar(
                PrumoIcons.MenuBook,
                Modifier.align(Alignment.BottomStart).offset(x = 92.dp),
                size = 104.dp,
                iconSize = 40.dp,
                shape = MaterialShapes.Flower.toShape(),
            )
        }
    }
}

/** The steps of the first run: welcome, server, login. */
internal const val STEPS = 3

@PhonePreviews
@Composable
private fun WelcomePreview() {
    PrumoTheme(dynamicColor = false) { WelcomeScreen(onStart = {}) }
}
