plugins {
    alias(libs.plugins.prumo.android.feature)
}

android {
    namespace = "com.henriquesebastiao.prumo.feature.finance"
}

dependencies {
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.paging.compose)

    testImplementation(testFixtures(projects.core.network))
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
