plugins {
    alias(libs.plugins.prumo.android.feature)
}

android {
    namespace = "com.henriquesebastiao.prumo.feature.reading"
}

dependencies {
    testImplementation(testFixtures(projects.core.network))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
}
