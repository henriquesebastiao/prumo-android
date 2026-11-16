package com.henriquesebastiao.prumo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.model.ThemeMode
import com.henriquesebastiao.prumo.navigation.PrumoApp
import com.henriquesebastiao.prumo.system.LauncherShortcuts
import com.henriquesebastiao.prumo.system.ModuleRegistry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    @Inject
    lateinit var registry: ModuleRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The splash stays while the encrypted session is read.
        splash.setKeepOnScreenCondition { viewModel.state.value.session == SessionState.Loading }
        if (savedInstanceState == null) viewModel.onLaunchAction(LauncherShortcuts.actionOf(intent))
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val launchAction by viewModel.launchAction.collectAsStateWithLifecycle()
            val dark = when (state.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // The bars' icons follow the app's theme, which may differ from the system's (the
            // theme is the account's preference): light icons on the dark theme and vice versa.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            PrumoTheme(darkTheme = dark, dynamicColor = state.device.dynamicColor, pureBlack = state.device.pureBlack) {
                PrumoApp(
                    state.session,
                    registry,
                    launchAction = launchAction,
                    onLaunchActionOpened = viewModel::onLaunchActionOpened,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.onLaunchAction(LauncherShortcuts.actionOf(intent))
    }
}
