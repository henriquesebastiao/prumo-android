plugins {
    alias(libs.plugins.prumo.android.library)
    alias(libs.plugins.prumo.android.compose)
}

android {
    namespace = "com.henriquesebastiao.prumo.core.feature"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    api(libs.androidx.navigation3.runtime)
    api(libs.kotlinx.serialization.json)
}
