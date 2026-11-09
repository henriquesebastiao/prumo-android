package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/**
 * The wide display title of the welcome ("Prumo") and of the Home's greeting ([small]: "Boa tarde"),
 * with an optional subtitle under it. One per screen. A long word steps the title down. [centered]
 * centers both (the welcome).
 */
@Composable
fun HeroHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    small: Boolean = false,
    centered: Boolean = false,
) {
    val align = if (centered) TextAlign.Center else TextAlign.Start
    Column(
        modifier.padding(horizontal = Spacing.xl, vertical = if (small) Spacing.md else Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(if (small) Spacing.xs else Spacing.lg),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        val style = if (small) PrumoType.heroSmall else PrumoType.hero
        Text(
            title,
            style = style,
            maxLines = 2,
            textAlign = align,
            autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = style.fontSize, stepSize = 2.sp),
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = if (small) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = align,
            )
        }
    }
}

@PhonePreviews
@Composable
private fun HeroPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column {
                HeroHeader("Prumo", subtitle = "Finanças, veículos e livros em módulos.")
                HeroHeader("Boa tarde", subtitle = "Segunda, 2 de novembro", small = true)
            }
        }
    }
}
