plugins {
    alias(libs.plugins.prumo.android.library)
    alias(libs.plugins.prumo.android.compose)
}

android {
    namespace = "com.henriquesebastiao.prumo.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
}
