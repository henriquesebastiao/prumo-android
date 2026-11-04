package com.henriquesebastiao.prumo.system

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.ErrorCodes
import com.henriquesebastiao.prumo.core.model.ServerCheck
import com.henriquesebastiao.prumo.system.home.HomeScreen
import com.henriquesebastiao.prumo.system.onboarding.LoginScreen
import com.henriquesebastiao.prumo.system.onboarding.LoginUiState
import com.henriquesebastiao.prumo.system.onboarding.ServerScreen
import com.henriquesebastiao.prumo.system.onboarding.ServerUiState
import com.henriquesebastiao.prumo.ui.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The stateless system screens, with the design boards' data (PreviewData). */
@Config(application = Application::class, qualifiers = "w412dp-h915dp")
@RunWith(AndroidJUnit4::class)
class SystemScreensTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theHomeShowsEachModulesCardAndOpensSettings() {
        var settings = 0
        var opened = ""
        compose.setContent {
            PrumoTheme(dynamicColor = false) {
                HomeScreen(
                    state = PreviewData.home,
                    onRefresh = {},
                    onOpenModule = { opened = it.id },
                    onAction = {},
                    onTogglePinned = {},
                    onSettings = { settings++ },
                    onOrganizeModules = {},
                    onSignIn = {},
                )
            }
        }
        compose.onNodeWithText("Boa tarde").assertIsDisplayed()
        compose.onNodeWithText("Segunda, 2 de novembro").assertIsDisplayed()
        compose.onNodeWithText("Saldo total").assertIsDisplayed()
        compose.onNodeWithText("Código Limpo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Configurações").performClick()
        assertEquals(1, settings)
        compose.onNodeWithContentDescription("Abrir Finanças").performClick()
        assertEquals("finance", opened)
    }

    @Test
    fun continueWaitsForAPrumoServer() {
        compose.setContent {
            PrumoTheme(dynamicColor = false) {
                ServerScreen(
                    address = "prumo.minhacasa.net",
                    state = ServerUiState(check = ServerCheck.NotPrumo("https://prumo.minhacasa.net")),
                    localNetworkDenied = false,
                    onAddressChange = {},
                    onTest = {},
                    onContinue = {},
                )
            }
        }
        // On the field and in the card.
        compose.onAllNodesWithText("Este endereço não é um servidor Prumo").assertCountEquals(2)
        compose.onNodeWithContentDescription("Continuar").assertIsNotEnabled()
    }

    @Test
    fun aFoundServerShowsItsVersionAndLetsContinue() {
        compose.setContent {
            PrumoTheme(dynamicColor = false) {
                ServerScreen(
                    address = "prumo.minhacasa.net",
                    state = ServerUiState(check = ServerCheck.Found("https://prumo.minhacasa.net", "1.0.2", 84)),
                    localNetworkDenied = false,
                    onAddressChange = {},
                    onTest = {},
                    onContinue = {},
                )
            }
        }
        compose.onNodeWithText("Servidor encontrado").assertIsDisplayed()
        compose.onNodeWithText("Prumo 1.0.2").assertIsDisplayed()
        compose.onNodeWithContentDescription("Continuar").assertIsEnabled()
    }

    @Test
    fun aWrongPasswordShowsOnTheField() {
        compose.setContent {
            PrumoTheme(dynamicColor = false) {
                LoginScreen(
                    server = "prumo.minhacasa.net",
                    username = "marina",
                    password = "",
                    state = LoginUiState(error = AppError.Api(401, ErrorCodes.AUTH_INVALID_CREDENTIALS)),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onChangeServer = {},
                )
            }
        }
        compose.onNodeWithText("Usuário ou senha incorretos").assertIsDisplayed()
        compose.onNodeWithText("prumo.minhacasa.net").assertIsDisplayed()
        compose.onNodeWithContentDescription("Entrar").assertIsNotEnabled()
    }
}
