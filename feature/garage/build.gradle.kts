plugins {
    alias(libs.plugins.prumo.android.feature)
}

android {
    namespace = "com.henriquesebastiao.prumo.feature.garage"
}

dependencies {
    testImplementation(testFixtures(projects.core.network))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
}
