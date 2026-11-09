package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/** A section's title (titleSmall in the accent color) with an optional text at the end ("3 pendentes"). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A centered message for an empty list, with an optional action. [hero] is for a screen that is
 * nothing but this message: a big icon in primary and a headline.
 */
@Composable
fun EmptyState(
    icon: Int,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    hero: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = if (hero) 48.dp else Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = if (hero) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(if (hero) 88.dp else 48.dp),
        )
        Text(
            title,
            style = if (hero) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
            )
        }
        if (action != null) {
            Spacer(Modifier.height(Spacing.sm))
            action()
        }
    }
}

/** What a [StatusChip] tells. */
enum class StatusTone { Success, Warning, Error, Neutral }

/**
 * A status in a pill ("Servidor encontrado", "Em breve"): always color, icon and label, in its
 * tone's container colors. 32 dp for a state; [small] (24 dp) for an attribute of a list item.
 */
@Composable
fun StatusChip(label: String, tone: StatusTone, modifier: Modifier = Modifier, small: Boolean = false) {
    val extended = PrumoTheme.extendedColors
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        StatusTone.Success -> extended.successContainer to extended.onSuccessContainer
        StatusTone.Warning -> extended.warningContainer to extended.onWarningContainer
        StatusTone.Error -> scheme.errorContainer to scheme.onErrorContainer
        StatusTone.Neutral -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
    }
    val icon = when (tone) {
        StatusTone.Success -> PrumoIcons.CheckCircleFilled
        StatusTone.Warning -> PrumoIcons.WarningFilled
        StatusTone.Error -> PrumoIcons.ErrorFilled
        StatusTone.Neutral -> PrumoIcons.DoNotDisturbOnFilled
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(if (small) 6.dp else 8.dp),
        modifier = modifier,
    ) {
        Row(
            Modifier
                .height(if (small) 24.dp else 32.dp)
                .padding(start = if (small) 6.dp else Spacing.sm, end = if (small) Spacing.sm else Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (small) Spacing.xs else 6.dp),
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(if (small) 14.dp else 18.dp),
            )
            Text(
                label,
                style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

/**
 * A label in a pill: the server's address, the time of cached data. Not a filter. With [onClick],
 * a default that changes with a tap (the date and km of a fueling).
 */
@Composable
fun Tag(
    label: String,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    small: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            Modifier
                .height(if (small) 32.dp else 40.dp)
                .padding(PaddingValues(start = if (icon != null) Spacing.md else Spacing.lg, end = Spacing.lg)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (icon != null) Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                label,
                style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    val color = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.onSurface
    if (onClick != null) {
        Surface(onClick, modifier, shape = CircleShape, color = color, contentColor = contentColor, content = content)
    } else {
        Surface(modifier, shape = CircleShape, color = color, contentColor = contentColor, content = content)
    }
}

/**
 * Something failed: the [title] says what, the [body] what to check, and [retryLabel] tries again.
 * In the error container.
 */
@Composable
fun ErrorCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null,
    icon: Int = PrumoIcons.Error,
) {
    MessageCard(
        icon,
        title,
        body,
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
        modifier,
        retryLabel,
        onRetry,
    )
}

/** The device has no connection: what still works and what waits. */
@Composable
fun NoConnectionCard(title: String, body: String, modifier: Modifier = Modifier) {
    MessageCard(
        PrumoIcons.CloudOff,
        title,
        body,
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.onSurface,
        modifier,
        null,
        null,
    )
}

@Composable
private fun MessageCard(
    icon: Int,
    title: String,
    body: String,
    container: Color,
    content: Color,
    modifier: Modifier,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(icon), contentDescription = null)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            }
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = BODY_ALPHA),
            )
            if (actionLabel != null && onAction != null) {
                FilledTonalButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                    Icon(
                        painterResource(PrumoIcons.Refresh),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(Spacing.sm))
                    Text(actionLabel)
                }
            }
        }
    }
}

/**
 * One of a few options as a connected button group (the theme: Sistema, Claro, Escuro): the
 * selected button filled, the ends rounded, all as wide. [icons] go before the labels.
 */
@Composable
fun <T> ChoiceGroup(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: ((T) -> Int)? = null,
) {
    Row(
        modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val checked = option == selected
            ToggleButton(
                checked = checked,
                onCheckedChange = { onSelect(option) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(),
                contentPadding = PaddingValues(horizontal = Spacing.md),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .semantics {
                        role = Role.RadioButton
                        this.selected = checked
                    },
            ) {
                if (icon != null) {
                    Icon(painterResource(icon(option)), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(Spacing.sm))
                }
                Text(
                    label(option),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 16.sp, stepSize = 1.sp),
                )
            }
        }
    }
}

private const val BODY_ALPHA = 0.86f

@PhonePreviews
@Composable
private fun ComponentsPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                SectionHeader("Aguardando sincronização", trailing = "3 pendentes")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    StatusChip("Servidor encontrado", StatusTone.Success)
                    StatusChip("Em breve", StatusTone.Warning, small = true)
                    Tag("prumo.minhacasa.net", icon = PrumoIcons.Dns, small = true)
                }
                ChoiceGroup(listOf("Sistema", "Claro", "Escuro"), "Sistema", { it }, {})
                ErrorCard(
                    "Não foi possível conectar",
                    "O nome prumo.minhacasa.nte não existe no DNS.",
                    retryLabel = "Testar de novo",
                    onRetry = {},
                )
                NoConnectionCard("Sem conexão", "Continue registrando normalmente.")
                EmptyState(PrumoIcons.Widgets, "Nenhum módulo ativo", body = "Ative um módulo em Configurações.")
                TextButton(onClick = {}) { Text("Organizar módulos") }
            }
        }
    }
}
