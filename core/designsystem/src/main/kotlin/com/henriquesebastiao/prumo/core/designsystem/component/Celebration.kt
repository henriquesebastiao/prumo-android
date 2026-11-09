package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme

/**
 * The design's one celebratory moment ("Confirmação expressiva": finishing a book, recording an oil
 * change): the hero's avatar morphs from a 9-sided cookie into a sunny shape with an eighth of a
 * turn, on the slow spatial spring. Without [celebrating] it is a plain cookie avatar.
 */
@Composable
fun CelebrationAvatar(
    icon: Int,
    celebrating: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val progress by animateFloatAsState(if (celebrating) 1f else 0f, spec, label = "morph")
    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny) }
    val shape = remember(progress) { MorphShape(morph, progress.coerceIn(0f, 1f)) }
    Box(
        modifier
            .size(size)
            .graphicsLayer { rotationZ = progress * EIGHTH_TURN }
            .background(container, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(iconSize).graphicsLayer { rotationZ = -progress * EIGHTH_TURN },
        )
    }
}

/** A [Morph] between two of Material's unit shapes at [progress], scaled to the size drawn. */
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress).asComposePath()
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

private const val EIGHTH_TURN = 45f

@PhonePreviews
@Composable
private fun CelebrationAvatarPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column {
                CelebrationAvatar(PrumoIcons.AutoStories, celebrating = false)
                CelebrationAvatar(PrumoIcons.AutoStories, celebrating = true)
            }
        }
    }
}
