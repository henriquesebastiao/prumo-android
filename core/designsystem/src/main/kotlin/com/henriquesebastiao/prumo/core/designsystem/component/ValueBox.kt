package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/**
 * The main value of a form as the design draws it ("Campo de valor", docs/design-v1.md): the label
 * in labelMedium over the value in a large face with tabular figures, on a 16 dp box. Focused, the
 * box is the lightest surface with a 2 dp primary border and the label in primary; at rest, a
 * filled tonal box. [focusRequester] lets a sheet give it the focus as it opens.
 */
@Composable
internal fun ValueBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    transformation: VisualTransformation,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    leadingIcon: Int? = null,
    suffix: String? = null,
    supportingText: String? = null,
    error: String? = null,
    focusRequester: FocusRequester? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    // A tap anywhere on the box (the label too) goes to the field.
    val requester = focusRequester ?: remember { FocusRequester() }
    val focused by interaction.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.fastEffectsSpec<Color>()
    val borderColor by animateColorAsState(
        when {
            error != null -> scheme.error
            focused -> scheme.primary
            else -> scheme.surfaceContainerHighest
        },
        effects,
        label = "border",
    )
    val container by animateColorAsState(
        if (focused || error != null) scheme.surfaceContainerLowest else scheme.surfaceContainerHighest,
        effects,
        label = "container",
    )
    val borderWidth by animateDpAsState(if (focused || error != null) 2.dp else 0.dp, label = "borderWidth")
    val labelColor = when {
        error != null -> scheme.error
        focused -> scheme.primary
        else -> scheme.onSurfaceVariant
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Surface(
            color = container,
            contentColor = scheme.onSurface,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .border(borderWidth, borderColor, MaterialTheme.shapes.large)
                .clickable(interactionSource = null, indication = null) { requester.requestFocus() },
        ) {
            Row(
                Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (leadingIcon != null) {
                    Icon(painterResource(leadingIcon), contentDescription = null, tint = scheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor)
                    // Typing and deleting happen at the end, as on a calculator: the cursor stays
                    // there even when the field gets the focus with a value in it (a partial
                    // payment opens with what is left).
                    BasicTextField(
                        value = TextFieldValue(value, TextRange(value.length)),
                        onValueChange = { onValueChange(it.text) },
                        textStyle = PrumoType.dataHeadline.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(if (error != null) scheme.error else scheme.primary),
                        // The unit ("L", "km", "de 425") follows the number, drawn after what was
                        // typed, smaller and muted.
                        visualTransformation = if (suffix != null) {
                            val unit = MaterialTheme.typography.titleLarge
                            SuffixTransformation(
                                transformation,
                                suffix,
                                SpanStyle(
                                    color = scheme.onSurfaceVariant,
                                    fontSize = unit.fontSize,
                                    fontWeight = FontWeight.Normal,
                                ),
                            )
                        } else {
                            transformation
                        },
                        keyboardOptions = keyboardOptions,
                        singleLine = true,
                        interactionSource = interaction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(requester)
                            .semantics {
                                contentDescription = label
                                if (error != null) error(error)
                            },
                    )
                }
            }
        }
        val support = error ?: supportingText
        if (support != null) {
            Text(
                support,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) scheme.error else scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }
    }
}

/**
 * A [FocusRequester] that takes the focus once, when the composable enters (a sheet or form
 * opening on its "Campo de valor", keyboard up). With [enabled] false (editing), it does nothing.
 */
@Composable
fun rememberOpeningFocus(enabled: Boolean = true): FocusRequester {
    val requester = remember { FocusRequester() }
    if (enabled) LaunchedEffect(Unit) { requester.requestFocus() }
    return requester
}

/** [base], then " [suffix]" in [style] when something was typed. The cursor never goes into the suffix. */
private class SuffixTransformation(
    private val base: VisualTransformation,
    private val suffix: String,
    private val style: SpanStyle,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val inner = base.filter(text)
        if (text.isEmpty()) return inner
        val shown = inner.text.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = inner.offsetMapping.originalToTransformed(offset)

            override fun transformedToOriginal(offset: Int): Int =
                inner.offsetMapping.transformedToOriginal(offset.coerceAtMost(shown))
        }
        return TransformedText(inner.text + AnnotatedString(" $suffix", style), mapping)
    }

    override fun equals(other: Any?): Boolean =
        other is SuffixTransformation && other.base == base && other.suffix == suffix && other.style == style

    override fun hashCode(): Int = 31 * (31 * base.hashCode() + suffix.hashCode()) + style.hashCode()
}
