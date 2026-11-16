package com.henriquesebastiao.prumo

import com.henriquesebastiao.prumo.core.feature.QuickAction
import com.henriquesebastiao.prumo.core.testing.FakeAccountRepository
import com.henriquesebastiao.prumo.core.testing.FakeDashboardRepository
import com.henriquesebastiao.prumo.core.testing.FakeDevicePreferences
import com.henriquesebastiao.prumo.core.testing.FakeSessionRepository
import com.henriquesebastiao.prumo.core.testing.MainDispatcherRule
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.habits.HabitsFeature
import com.henriquesebastiao.prumo.system.LauncherShortcuts
import com.henriquesebastiao.prumo.system.ModuleRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    /** What the app published as shortcuts of its icon, last first. */
    private val published = mutableListOf<List<String>>()

    private val shortcuts = object : LauncherShortcuts {
        override fun publish(actions: List<QuickAction>) {
            published.add(0, actions.filter { it.launcherShortcut }.map { it.id })
        }

        override fun clear() = Unit
    }

    private val registry = ModuleRegistry(setOf(FinanceFeature(), HabitsFeature()))

    private fun viewModel(account: FakeAccountRepository) = AppViewModel(
        FakeSessionRepository(),
        account,
        FakeDashboardRepository(),
        FakeDevicePreferences(),
        shortcuts,
        registry,
    )

    @Test
    fun theShortcutsOfTheAppIconFollowTheEnabledModules() = runTest {
        val account = FakeAccountRepository()
        account.setEnabledModules(listOf("finance", "habits"))
        viewModel(account)
        assertEquals(listOf("habits.check_in"), published.first())

        account.setEnabledModules(listOf("finance"))
        assertEquals(emptyList<String>(), published.first())
    }

    @Test
    fun aShortcutWaitsForTheHomeToOpenIt() = runTest {
        val vm = viewModel(FakeAccountRepository())
        vm.onLaunchAction(null)
        assertNull(vm.launchAction.value)
        vm.onLaunchAction("habits.check_in")
        assertEquals("habits.check_in", vm.launchAction.value)
        vm.onLaunchActionOpened()
        assertNull(vm.launchAction.value)
    }
}
