package com.henriquesebastiao.prumo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.data.session.SignOutReason
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.ui.SessionExpiredDialog
import com.henriquesebastiao.prumo.system.ModuleRegistry
import com.henriquesebastiao.prumo.system.home.HomeRoute
import com.henriquesebastiao.prumo.system.onboarding.LoginRoute
import com.henriquesebastiao.prumo.system.onboarding.ServerRoute
import com.henriquesebastiao.prumo.system.onboarding.WelcomeScreen
import com.henriquesebastiao.prumo.system.settings.AccountRoute
import com.henriquesebastiao.prumo.system.settings.AppearanceRoute
import com.henriquesebastiao.prumo.system.settings.ModulesRoute
import com.henriquesebastiao.prumo.system.settings.ServerSettingsRoute
import com.henriquesebastiao.prumo.system.settings.SessionsRoute
import com.henriquesebastiao.prumo.system.settings.SettingsActions
import com.henriquesebastiao.prumo.system.settings.SettingsRoute

/**
 * The app by session: the first run while signed out, the Home and everything under it while
 * signed in. A session that expires keeps the screens (and their last data) under the "Sessão
 * expirada" dialog; "Entrar" goes to the login with the server filled in, and after signing in the
 * same screens come back.
 */
@Composable
fun PrumoApp(session: SessionState, registry: ModuleRegistry, modifier: Modifier = Modifier) {
    val main = rememberNavBackStack(Home)
    var signInAgain by rememberSaveable { mutableStateOf(false) }
    var dialogDismissed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(session) {
        if (session is SessionState.SignedIn) {
            signInAgain = false
            dialogDismissed = false
        }
        // Signed out by choice (or another server): the next session starts at the Home.
        if (session is SessionState.SignedOut && session.reason != SignOutReason.Expired) main.resetTo(Home)
    }
    when (session) {
        SessionState.Loading -> Unit

        is SessionState.SignedIn -> MainNavigation(main, registry, onSignIn = {}, modifier = modifier)

        is SessionState.SignedOut -> if (session.reason == SignOutReason.Expired && !signInAgain) {
            MainNavigation(main, registry, onSignIn = { signInAgain = true }, modifier = modifier)
            if (!dialogDismissed) {
                SessionExpiredDialog(onSignIn = { signInAgain = true }, onDismiss = { dialogDismissed = true })
            }
        } else {
            FirstRun(session.lastServer, modifier)
        }
    }
}

/** The first run: welcome, server, login; straight to the login when the server is known. */
@Composable
private fun FirstRun(lastServer: String?, modifier: Modifier = Modifier) {
    val stack = if (lastServer !=
        null
    ) {
        rememberNavBackStack(ServerSetup, Login(lastServer))
    } else {
        rememberNavBackStack(Welcome)
    }
    NavDisplay(
        backStack = stack,
        onBack = { stack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        modifier = modifier,
        entryProvider = entryProvider {
            entry<Welcome> { WelcomeScreen(onStart = { stack.add(ServerSetup) }) }
            entry<ServerSetup> { ServerRoute(onContinue = { baseUrl -> stack.add(Login(baseUrl)) }) }
            entry<Login> { key ->
                LoginRoute(
                    baseUrl = key.baseUrl,
                    onChangeServer = {
                        if (stack.size > 1) stack.removeLastOrNull() else stack.resetTo(ServerSetup)
                    },
                )
            }
        },
    )
}

/** The signed-in app: the Home as root, Settings, and each enabled module's destinations. */
@Composable
private fun MainNavigation(
    stack: NavBackStack<NavKey>,
    registry: ModuleRegistry,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigator = remember(stack) {
        object : Navigator {
            override fun navigate(key: NavKey) {
                stack.add(key)
            }

            override fun back() {
                if (stack.size > 1) stack.removeLastOrNull()
            }
        }
    }
    val settings = SettingsActions(
        onBack = navigator::back,
        onAccount = { stack.add(AccountSettings) },
        onAppearance = { stack.add(AppearanceSettings) },
        onModules = { stack.add(ModulesSettings) },
        onSessions = { stack.add(SessionsSettings) },
        onServer = { stack.add(ServerSettings) },
    )
    NavDisplay(
        backStack = stack,
        onBack = navigator::back,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        modifier = modifier,
        entryProvider = entryProvider {
            entry<Home> {
                HomeRoute(
                    onOpenModule = { navigator.navigate(it.entryKey) },
                    onAction = { navigator.navigate(it.target) },
                    onSettings = { stack.add(Settings) },
                    onOrganizeModules = { stack.add(ModulesSettings) },
                    onSignIn = onSignIn,
                )
            }
            entry<Settings> { SettingsRoute(settings) }
            entry<AccountSettings> { AccountRoute(onBack = navigator::back) }
            entry<AppearanceSettings> { AppearanceRoute(onBack = navigator::back) }
            entry<ModulesSettings> { ModulesRoute(onBack = navigator::back) }
            entry<SessionsSettings> { SessionsRoute(onBack = navigator::back) }
            entry<ServerSettings> { ServerSettingsRoute(onBack = navigator::back) }
            registry.all.values.forEach { feature -> with(feature) { entries(navigator) } }
        },
    )
}

/** The stack with only [key]. */
private fun NavBackStack<NavKey>.resetTo(key: NavKey) {
    if (size == 1 && first() == key) return
    clear()
    add(key)
}
