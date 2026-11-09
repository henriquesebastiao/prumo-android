package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoShape
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import kotlin.math.roundToInt

/**
 * The bottom of the first run: where it's at ([label], "Etapa 1 de 3") and the 96 dp button that
 * moves it on ([nextLabel] is what it does). The button changes shape at every [step] (circle,
 * squircle, leaf) and squares up while pressed, on the spatial spring. [busy] puts a loading
 * indicator in it; disabled, it turns grey. [emphasized] (the first step) shows the label larger, in
 * primary. Pads itself above the navigation bar.
 */
@Composable
fun OnboardingFooter(
    step: Int,
    label: String,
    nextLabel: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Int = PrumoIcons.ArrowForward,
    enabled: Boolean = true,
    busy: Boolean = false,
    emphasized: Boolean = false,
) {
    Surface(
        modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = PrumoShape.OnboardingFooter,
    ) {
        Box(Modifier.navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
            Row(
                Modifier
                    .widthIn(max = CONTENT_WIDTH)
                    .fillMaxWidth()
                    .padding(start = Spacing.xxl, end = Spacing.lg, top = Spacing.lg, bottom = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                val typography = MaterialTheme.typography
                val scheme = MaterialTheme.colorScheme
                Text(
                    label,
                    style = if (emphasized) typography.headlineMedium else typography.titleLarge,
                    color = if (emphasized) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                NextButton(step, nextLabel, icon, onNext, enabled, busy)
            }
        }
    }
}

@Composable
private fun NextButton(step: Int, label: String, icon: Int, onClick: () -> Unit, enabled: Boolean, busy: Boolean) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corners = if (pressed) PressedCorners else StepCorners[(step - 1).mod(StepCorners.size)]
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val topStart by animateFloatAsState(corners[0], spec, label = "topStart")
    val topEnd by animateFloatAsState(corners[1], spec, label = "topEnd")
    val bottomEnd by animateFloatAsState(corners[2], spec, label = "bottomEnd")
    val bottomStart by animateFloatAsState(corners[3], spec, label = "bottomStart")

    // Percent of the side (CornerSize(Float) would read pixels). The spring overshoots; a corner past
    // half the side would fold the shape onto itself.
    fun corner(percent: Float) = CornerSize(percent.coerceIn(0f, MAX_CORNER).roundToInt())
    val shape = RoundedCornerShape(corner(topStart), corner(topEnd), corner(bottomEnd), corner(bottomStart))
    val scheme = MaterialTheme.colorScheme
    FilledIconButton(
        onClick = onClick,
        enabled = enabled && !busy,
        shape = shape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
            disabledContainerColor = if (busy) scheme.primaryContainer else scheme.surfaceContainerHighest,
            disabledContentColor = if (busy) {
                scheme.onPrimaryContainer
            } else {
                scheme.onSurface.copy(
                    alpha = DISABLED_ALPHA,
                )
            },
        ),
        interactionSource = interaction,
        modifier = Modifier.size(NEXT_SIZE).semantics { contentDescription = label },
    ) {
        if (busy) {
            LoadingIndicator(color = scheme.onPrimaryContainer, modifier = Modifier.size(56.dp))
        } else {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

// Corners in percent of the side, topStart, topEnd, bottomEnd, bottomStart: circle, squircle, leaf.
private val StepCorners = listOf(
    listOf(50f, 50f, 50f, 50f),
    listOf(30f, 30f, 30f, 30f),
    listOf(50f, 50f, 50f, 22f),
)
private val PressedCorners = listOf(24f, 24f, 24f, 24f)
private const val MAX_CORNER = 50f
private const val DISABLED_ALPHA = 0.38f
private val NEXT_SIZE = 96.dp
private val CONTENT_WIDTH = 560.dp

@PhonePreviews
@Composable
private fun OnboardingPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                OnboardingFooter(step = 1, label = "Etapa 1 de 3", nextLabel = "Começar", onNext = {
                }, emphasized = true)
                OnboardingFooter(step = 2, label = "Etapa 2 de 3", nextLabel = "Continuar", onNext = {
                }, enabled = false)
                OnboardingFooter(step = 3, label = "Etapa 3 de 3", nextLabel = "Entrar", onNext = {}, busy = true)
            }
        }
    }
}
