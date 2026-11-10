package com.henriquesebastiao.prumo.system.settings

import android.app.Application
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.data.repository.ExportResult
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.UnreachableCause
import com.henriquesebastiao.prumo.core.testing.FakeAccountRepository
import com.henriquesebastiao.prumo.core.testing.FakeDataExportRepository
import com.henriquesebastiao.prumo.core.testing.FakeDevicePreferences
import com.henriquesebastiao.prumo.core.testing.FakeServerRepository
import com.henriquesebastiao.prumo.core.testing.FakeSessionRepository
import com.henriquesebastiao.prumo.core.testing.MainDispatcherRule
import com.henriquesebastiao.prumo.system.ModuleRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(application = Application::class)
@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    // 17/11/2026 at 14:30 in São Paulo (UTC-3).
    private val clock = Clock.fixed(Instant.parse("2026-11-17T17:30:00Z"), ZoneOffset.UTC)

    private fun viewModel(export: FakeDataExportRepository, account: FakeAccountRepository = FakeAccountRepository()) =
        SettingsViewModel(
            account,
            FakeSessionRepository(),
            FakeServerRepository(),
            ModuleRegistry(emptySet()),
            export,
            clock,
            FakeDevicePreferences(),
        )

    @Test
    fun theSuggestedFileIsNamedLikeTheServersInTheUsersZone() = runTest {
        val account = FakeAccountRepository()
        account.refreshPreferences()
        assertEquals("prumo-marina-20261117-1430.zip", viewModel(FakeDataExportRepository(), account).exportFileName())
    }

    @Test
    fun anExportEndsWithItsResultUntilItIsShown() = runTest {
        val export = FakeDataExportRepository(ExportResult.Failed(AppError.Unreachable(UnreachableCause.Timeout)))
        val vm = viewModel(export)
        backgroundScope.launch { vm.state.collect {} }
        val file = Uri.parse("content://downloads/prumo.zip")

        vm.export(file)

        val ended = vm.state.first { it.exportResult != null }
        assertEquals(ExportResult.Failed(AppError.Unreachable(UnreachableCause.Timeout)), ended.exportResult)
        assertEquals(false, ended.exporting)
        assertEquals(listOf(file), export.exported)
        vm.exportResultShown()
        assertNull(vm.state.first { it.exportResult == null }.exportResult)
    }
}
