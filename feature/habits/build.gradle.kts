plugins {
    alias(libs.plugins.prumo.android.feature)
}

android {
    namespace = "com.henriquesebastiao.prumo.feature.habits"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    // The widget of the home screen (D27).
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    testImplementation(testFixtures(projects.core.network))
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.glance.appwidget.testing)
    debugImplementation(libs.compose.ui.test.manifest)
}
