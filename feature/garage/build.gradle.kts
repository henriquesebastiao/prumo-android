plugins {
    alias(libs.plugins.prumo.android.feature)
}

android {
    namespace = "com.henriquesebastiao.prumo.feature.garage"
}

dependencies {
    testImplementation(testFixtures(projects.core.network))
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
