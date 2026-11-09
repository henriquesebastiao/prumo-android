plugins {
    alias(libs.plugins.prumo.android.feature)
}

android {
    namespace = "com.henriquesebastiao.prumo.feature.reading"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(testFixtures(projects.core.network))
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
