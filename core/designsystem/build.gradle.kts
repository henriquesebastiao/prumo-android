plugins {
    alias(libs.plugins.prumo.android.library)
    alias(libs.plugins.prumo.android.compose)
}

android {
    namespace = "com.henriquesebastiao.prumo.core.designsystem"
}

dependencies {
    api(libs.compose.material3)
    api(libs.compose.ui)
    api(libs.compose.foundation)
    implementation(libs.material.color.utilities)
}
