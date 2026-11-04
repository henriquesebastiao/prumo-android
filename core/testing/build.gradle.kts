plugins {
    alias(libs.plugins.prumo.android.library)
}

android {
    namespace = "com.henriquesebastiao.prumo.core.testing"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
