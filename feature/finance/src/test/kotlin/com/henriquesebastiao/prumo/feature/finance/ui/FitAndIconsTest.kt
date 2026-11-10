package com.henriquesebastiao.prumo.feature.finance.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.ServerIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Values never wrap nor lose a digit on a 360 dp screen with the Android font at 130 %, and the
 * category icons: every one drawn, chosen by group or by a search (D23).
 */
@Config(application = Application::class, qualifiers = "w360dp-h800dp")
@RunWith(AndroidJUnit4::class)
class FitAndIconsTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun largeFont() {
        RuntimeEnvironment.setFontScale(LARGE_FONT)
    }

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        PrumoTheme(dynamicColor = false) { ModuleTheme(FinanceFeature.SEED, content) }
    }

    /** The text as laid out: one line, nothing cut. */
    private fun assertOneWholeLine(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(
            text,
            useUnmergedTree = true,
        ).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        val layout = layouts.single()
        assertEquals(text, 1, layout.lineCount)
        val detail = "$text: ${layout.layoutInput.style.fontSize} in ${layout.layoutInput.constraints}, " +
            "size ${layout.size}, text width ${layout.multiParagraph.width}, lines ${layout.lineCount}"
        assertFalse(detail, layout.hasVisualOverflow)
    }

    @Test
    fun aBigBalanceStaysOnOneLineWithEveryDigit() {
        val rich = FinancePreview.nubank.copy(name = "Conta conjunta da família", balanceCents = 9_876_543_210)
        show {
            AccountsTab(
                state = AccountsUiState(
                    loading = false,
                    accounts = listOf(rich, FinancePreview.inter),
                    totalBalanceCents = 9_876_758_900,
                ),
                events = AccountsEvents({}, {}, {}, {}),
            )
        }
        assertOneWholeLine(Formats.money(9_876_543_210))
        assertOneWholeLine(Formats.money(9_876_758_900))
    }

    @Test
    fun everyCategoryIconIsDrawnAndNamedOnce() {
        assertEquals(CategoryIcons.size, CategoryIcons.toSet().size)
        CategoryIcons.forEach { assertNotEquals(it, PrumoIcons.Category, ServerIcons.of(it)) }
        assertTrue(QuickCategoryIcons.all { it in CategoryIcons })
        assertTrue(CategoryIcons.size >= MANY)
    }

    @Test
    fun moreIconsOpensTheGroupsAndASearchWithoutAccents() {
        var icon = ""
        show {
            CategorySheetContent(
                state = CategorySheetUiState(icon = "tag"),
                name = "Farmácia",
                events = CategorySheetEvents({}, {}, {}, { icon = it }, {}, {}, {}),
            )
        }
        compose.onNodeWithContentDescription("Mais ícones").performClick()
        compose.onNodeWithText("Escolher ícone").assertIsDisplayed()
        compose.onNodeWithText("Transporte").assertIsDisplayed()
        compose.onNodeWithText("Buscar ícone").performTextInput("farmacia")
        compose.onNodeWithContentDescription("Farmácia").performScrollTo().performClick()
        assertEquals("pill", icon)
        // Back on the form, the chosen icon joins the quick picks.
        compose.onNodeWithContentDescription("Mais ícones").assertIsDisplayed()
    }

    private companion object {
        const val LARGE_FONT = 1.3f
        const val MANY = 80
    }
}
