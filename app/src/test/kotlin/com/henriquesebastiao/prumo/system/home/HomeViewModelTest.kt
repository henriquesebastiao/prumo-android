package com.henriquesebastiao.prumo.system.home

import com.henriquesebastiao.prumo.core.data.repository.DataChanges
import com.henriquesebastiao.prumo.core.model.ActivityItem
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Dashboard
import com.henriquesebastiao.prumo.core.model.DayPart
import com.henriquesebastiao.prumo.core.model.ModuleSummary
import com.henriquesebastiao.prumo.core.model.UnreachableCause
import com.henriquesebastiao.prumo.core.model.UpcomingItem
import com.henriquesebastiao.prumo.core.testing.FakeAccountRepository
import com.henriquesebastiao.prumo.core.testing.FakeDashboardRepository
import com.henriquesebastiao.prumo.core.testing.FakeDevicePreferences
import com.henriquesebastiao.prumo.core.testing.FakeSessionRepository
import com.henriquesebastiao.prumo.core.testing.MainDispatcherRule
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.reading.ReadingFeature
import com.henriquesebastiao.prumo.system.ModuleRegistry
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val changes = DataChanges()

    private val registry = ModuleRegistry(setOf(FinanceFeature(), GarageFeature(), ReadingFeature()))

    // 17/11/2026 at 14:30 in São Paulo (UTC-3), as the design boards; 17:30 in UTC.
    private val clock = Clock.fixed(Instant.parse("2026-11-17T17:30:00Z"), ZoneOffset.UTC)

    private fun summary(module: String) = ModuleSummary(module, JsonObject(emptyMap()))

    private fun viewModel(
        account: FakeAccountRepository = FakeAccountRepository(),
        dashboard: FakeDashboardRepository = FakeDashboardRepository(
            Dashboard(listOf(summary("finance"), summary("garage"), summary("reading")), emptyList(), emptyList()),
        ),
        devicePreferences: FakeDevicePreferences = FakeDevicePreferences(),
    ) = HomeViewModel(account, dashboard, devicePreferences, registry, FakeSessionRepository(), clock, changes)

    @Test
    fun theCardsFollowTheUsersOrderAndSkipWhatTheAppDoesNotKnow() = runTest {
        val account = FakeAccountRepository()
        account.setEnabledModules(listOf("reading", "health", "finance"))
        val vm = viewModel(account)
        backgroundScope.launch { vm.state.collect {} }
        vm.refresh()

        val state = vm.state.first { !it.loading }
        assertEquals(listOf("reading", "finance"), state.cards.map { it.feature.id })
        assertEquals(listOf("reading", "finance"), state.groups.map { it.feature.id })
    }

    @Test
    fun theGreetingAndTheDateAreInTheUsersZone() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.state.collect {} }
        vm.refresh()

        val state = vm.state.first { !it.loading }
        assertEquals(DayPart.Afternoon, state.greeting)
        assertEquals(LocalDate.of(2026, 11, 17), state.date)
        assertEquals("BRL", state.context.currency)
    }

    @Test
    fun thePinnedActionsAreTheDesignsUntilTheUserChooses() = runTest {
        val device = FakeDevicePreferences()
        val vm = viewModel(devicePreferences = device)
        backgroundScope.launch { vm.state.collect {} }
        vm.refresh()

        assertEquals(
            listOf("finance.expense", "garage.fueling", "garage.odometer", "reading.progress"),
            vm.state.first { !it.loading }.pinned.map { it.id },
        )
        vm.togglePinned("garage.odometer")
        assertEquals(
            listOf("finance.expense", "garage.fueling", "reading.progress"),
            device.preferences.value.pinnedActions,
        )
    }

    @Test
    fun aServerThatStopsAnsweringKeepsTheLastData() = runTest {
        val dashboard = FakeDashboardRepository(Dashboard(listOf(summary("finance")), emptyList(), emptyList()))
        val vm = viewModel(dashboard = dashboard)
        backgroundScope.launch { vm.state.collect {} }
        vm.refresh()
        vm.state.first { !it.loading }

        dashboard.failure = AppError.Unreachable(UnreachableCause.Timeout)
        vm.refresh()

        val state = vm.state.first { !it.refreshing }
        assertNull(state.error)
        assertEquals(SyncStatus.Unreachable, state.sync)
        assertEquals(listOf("finance", "garage", "reading"), state.cards.map { it.feature.id })
    }

    @Test
    fun aFirstLoadThatFailsShowsTheError() = runTest {
        val dashboard = FakeDashboardRepository().apply { failure = AppError.Unexpected(500) }
        val vm = viewModel(dashboard = dashboard)
        backgroundScope.launch { vm.state.collect {} }
        vm.refresh()

        assertEquals(AppError.Unexpected(500), vm.state.first { !it.loading }.error)
    }

    @Test
    fun aRecordSavedInAModuleReloadsTheSummaries() = runTest {
        val dashboard = FakeDashboardRepository(Dashboard(listOf(summary("finance")), emptyList(), emptyList()))
        val vm = viewModel(dashboard = dashboard)
        backgroundScope.launch { vm.state.collect {} }
        vm.refresh()
        vm.state.first { !it.loading }

        dashboard.dashboard = Dashboard(listOf(summary("finance"), summary("garage")), emptyList(), emptyList())
        changes.changed("garage")

        val garage = vm.state.first { state -> state.cards.any { it.feature.id == "garage" && it.summary != null } }
        assertEquals("garage", garage.cards[1].feature.id)
    }

    @Test
    fun theListsKeepTheItemsOfTheEnabledModulesTheAppKnows() = runTest {
        val account = FakeAccountRepository()
        account.setEnabledModules(listOf("finance", "reading", "health"))
        fun upcoming(module: String) =
            UpcomingItem(module, "$module.x", LocalDate.of(2026, 11, 20), "s", null, emptyMap())
        fun activity(module: String) =
            ActivityItem(module, "$module.x", Instant.parse("2026-11-17T10:00:00Z"), "s", null, emptyMap())
        val dashboard = FakeDashboardRepository(
            Dashboard(
                listOf(summary("finance")),
                listOf(upcoming("garage"), upcoming("finance"), upcoming("health")),
                listOf(activity("reading"), activity("garage"), activity("finance")),
            ),
        )
        val vm = viewModel(account, dashboard)
        backgroundScope.launch { vm.state.collect {} }
        vm.refresh()

        val state = vm.state.first { !it.loading }
        assertEquals(listOf("finance"), state.upcoming.map { it.feature.id })
        assertEquals(listOf("reading", "finance"), state.activity.map { it.feature.id })
        assertEquals(ZoneId.of("America/Sao_Paulo"), state.context.zone)
    }
}
