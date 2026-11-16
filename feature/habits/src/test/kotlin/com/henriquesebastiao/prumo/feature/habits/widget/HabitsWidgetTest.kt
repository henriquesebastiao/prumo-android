package com.henriquesebastiao.prumo.feature.habits.widget

import android.app.Application
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.assertHasRunCallbackClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.feature.habits.ui.HabitsPreview
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The widget of the home screen (D27) with the demo server's day (HabitsPreview). */
@Config(application = Application::class)
@RunWith(AndroidJUnit4::class)
class HabitsWidgetTest {
    private fun show(state: WidgetState, test: GlanceAppWidgetUnitTest.() -> Unit) = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 200.dp))
        setContext(ApplicationProvider.getApplicationContext())
        provideComposable { HabitsWidgetTheme { HabitsWidgetContent(state) } }
        test()
    }

    @Test
    fun theOpenHabitsOfTodayAreMarkedRightThere() = show(WidgetState.Loaded(HabitsPreview.day)) {
        onNode(hasText("1 de 5 hoje")).assertExists()
        onNode(hasText("Alongar")).assertExists()
        onNode(hasText("07:00 · passou do horário")).assertExists()
        onNode(hasText("Tarefa do mês")).assertExists()
        onNode(hasText("1.250\u00A0/\u00A02.000\u00A0ml")).assertExists()
        // Done and still open in their period stay out: the app shows them.
        onNode(hasText("Ler a Bíblia")).assertDoesNotExist()
        onNode(hasText("Academia")).assertDoesNotExist()
        onNode(hasContentDescription("Mais Beber água")).assertHasRunCallbackClickAction<MarkHabitAction>(
            actionParametersOf(
                MarkHabitAction.HabitId to "h-water",
                MarkHabitAction.Day to "2026-11-17",
                MarkHabitAction.Value to 1500,
            ),
        )
    }

    @Test
    fun everythingDoneSaysSo() = show(
        WidgetState.Loaded(HabitsPreview.day.copy(timeline = emptyList(), untimed = emptyList())),
    ) {
        onNode(hasText("Tudo feito por hoje.")).assertExists()
    }

    @Test
    fun aFailureSaysWhatToDo() = show(WidgetState.Failed(AppError.SessionExpired)) {
        onNode(hasText("Entre no Prumo para ver os hábitos.")).assertExists()
    }
}
