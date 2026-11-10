package com.henriquesebastiao.prumo.system

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.designsystem.component.HeroHeader
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Titles stay on one line, whole, on a 320 dp screen with the Android font at 130 %. */
@Config(application = Application::class, qualifiers = "w320dp-h700dp")
@RunWith(AndroidJUnit4::class)
class FitTitleTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun largeFont() {
        RuntimeEnvironment.setFontScale(LARGE_FONT)
    }

    /** Every copy of [text] (the large bar draws its title expanded and collapsed): one line, nothing cut. */
    private fun assertOneWholeLine(text: String) {
        val nodes = compose.onAllNodesWithText(text, useUnmergedTree = true)
        val count = nodes.fetchSemanticsNodes().size
        assertTrue(text, count > 0)
        repeat(count) { index ->
            val layouts = mutableListOf<TextLayoutResult>()
            nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(text, 1, layouts.single().lineCount)
            assertFalse(text, layouts.single().hasVisualOverflow)
        }
    }

    @Test
    fun theSettingsTitleStepsDownInsteadOfWrapping() {
        compose.setContent {
            PrumoTheme(dynamicColor = false) { PrumoTopBar("Configurações", onBack = {}, large = true) }
        }
        assertOneWholeLine("Configurações")
    }

    @Test
    fun theHomeGreetingStaysOnOneLine() {
        compose.setContent {
            PrumoTheme(dynamicColor = false) {
                HeroHeader("Boa tarde", subtitle = "Terça, 10 de novembro", small = true)
            }
        }
        assertOneWholeLine("Boa tarde")
    }

    private companion object {
        const val LARGE_FONT = 1.3f
    }
}
