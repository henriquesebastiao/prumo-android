package com.henriquesebastiao.prumo.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// The system's destinations (docs/arquitetura.md, "Navegação"). Each module brings its own keys
// through its PrumoFeature.

/** First run, step 1. */
@Serializable
data object Welcome : NavKey

/** First run, step 2: the server's address. */
@Serializable
data object ServerSetup : NavKey

/** First run, step 3: username and password on [baseUrl]. */
@Serializable
data class Login(val baseUrl: String) : NavKey

/** The root of the signed-in app. */
@Serializable
data object Home : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data object AccountSettings : NavKey

@Serializable
data object AppearanceSettings : NavKey

@Serializable
data object ModulesSettings : NavKey

@Serializable
data object SessionsSettings : NavKey

@Serializable
data object ServerSettings : NavKey
