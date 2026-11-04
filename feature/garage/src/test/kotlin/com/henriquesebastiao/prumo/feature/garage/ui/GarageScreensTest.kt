package com.henriquesebastiao.prumo.feature.garage.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The stateless screens of Veículos with the design boards' data (GaragePreview). */
@Config(application = Application::class, qualifiers = "w412dp-h915dp")
@RunWith(AndroidJUnit4::class)
class GarageScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        PrumoTheme(dynamicColor = false) { ModuleTheme(GarageFeature.SEED, content) }
    }

    @Test
    fun theListShowsEachVehicleWithItsOilStatus() {
        var opened = ""
        show {
            VehicleListScreen(
                state = VehicleListUiState(loading = false, vehicles = GaragePreview.vehicles),
                archived = false,
                onBack = {},
                onAdd = {},
                onOpen = { opened = it },
                onNavigate = {},
                actions = GarageActions({}, {}, {}, {}),
                onRefresh = {},
            )
        }
        compose.onNodeWithText("Honda CG 160 Fan · 2023 · 18.420 km").assertIsDisplayed()
        compose.onNodeWithText("Em dia").assertIsDisplayed()
        compose.onNodeWithText("Em breve").assertIsDisplayed()
        compose.onNodeWithContentDescription("Registrar abastecimento").assertIsDisplayed()
        compose.onNodeWithText("Fiat Strada Freedom").performClick()
        assertEquals(GaragePreview.strada.id, opened)
    }

    @Test
    fun withoutVehiclesTheListInvitesToAddOne() {
        var added = 0
        show {
            VehicleListScreen(
                state = VehicleListUiState(loading = false),
                archived = false,
                onBack = {},
                onAdd = { added++ },
                onOpen = {},
                onNavigate = {},
                actions = GarageActions({}, {}, {}, {}),
                onRefresh = {},
            )
        }
        compose.onNodeWithText("Nenhum veículo ainda").assertIsDisplayed()
        compose.onNodeWithText("Adicionar veículo").performClick()
        assertEquals(1, added)
    }

    @Test
    fun theVehicleShowsTheServersOilChangeAndStats() {
        show {
            VehicleDetailScreen(
                state = VehicleDetailUiState(loading = false, vehicle = GaragePreview.cg),
                onBack = {},
                actions = GarageActions({}, {}, {}, {}),
                events = VehicleDetailEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                onActionErrorShown = {},
            )
        }
        compose.onNodeWithText("Faltam 220 km").assertIsDisplayed()
        compose.onNodeWithText("Vence primeiro por km").assertIsDisplayed()
        compose.onNodeWithText("Por km · faltam 220 km").assertIsDisplayed()
        compose.onNodeWithText("Por data · faltam 65 dias").assertIsDisplayed()
        compose.onNodeWithText("18.420 km").assertIsDisplayed()
    }

    @Test
    fun theKmSheetShowsTheDifferenceAndTheConflictingReading() {
        show {
            OdometerSheetContent(
                state = OdometerUiState(
                    loading = false,
                    vehicles = GaragePreview.vehicles,
                    vehicleId = GaragePreview.cg.id,
                    saveError = AppError.Api(
                        422,
                        "garage.odometer_inconsistent",
                        details = mapOf(
                            "conflict" to "previous",
                            "km" to "18420",
                            "recorded_at" to "2026-11-17T10:42:00Z",
                        ),
                    ),
                ),
                km = "18512",
                onSelect = {},
                onKmChange = {},
                onSave = {},
                onRetry = {},
            )
        }
        compose.onNodeWithText("Atual: 18.420 km · hoje, 07:42").assertIsDisplayed()
        compose.onNodeWithText("Menor que o registro de 17/11/2026 (18.420 km). Confira o valor.").assertIsDisplayed()
    }

    @Test
    fun theFuelingSheetPreviewsThePricePerLitre() {
        show {
            FuelingSheetContent(
                state = FuelingUiState(
                    loading = false,
                    vehicles = GaragePreview.vehicles,
                    vehicleId = GaragePreview.cg.id,
                ),
                fields = FuelingFields(total = "4636", litres = "7,3", km = "18512"),
                canSave = true,
                callbacks = FuelingCallbacks({}, {}, {}, {}, {}, {}, {}, {}),
                onSave = {},
                onDelete = {},
                onRetry = {},
            )
        }
        compose.onNodeWithText("R$ 6,351 por litro · calculado").assertIsDisplayed()
        compose.onNodeWithText("Salvar abastecimento").performScrollTo().assertIsEnabled()
    }

    @Test
    fun theMaintenanceFormPreviewsTheTotal() {
        show {
            MaintenanceEditorScreen(
                state = MaintenanceEditorUiState(
                    loading = false,
                    vehicles = GaragePreview.vehicles,
                    vehicleId = GaragePreview.cg.id,
                ),
                fields = MaintenanceFields(
                    km = "18420",
                    labor = "2000",
                    parts = listOf(PartText(0, "Pastilha de freio", "2", "4500")),
                ),
                canSave = false,
                events = MaintenanceEditorEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                onRetry = {},
                onClose = {},
            )
        }
        compose.onNodeWithText("R$ 110,00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Peças R$ 90,00 + mão de obra R$ 20,00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Salvar manutenção").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun theMaintenanceShowsItsValues() {
        show {
            MaintenanceDetailScreen(
                state = MaintenanceDetailUiState(loading = false, maintenance = GaragePreview.maintenance),
                onBack = {},
                onEdit = {},
                onDelete = {},
                onRetry = {},
                onDeleteErrorShown = {},
            )
        }
        compose.onNodeWithText("Pastilha de freio dianteira × 2").assertIsDisplayed()
        compose.onNodeWithText("R$ 130,00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Moto Peças Rocha").assertIsDisplayed()
    }
}
