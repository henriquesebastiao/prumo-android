plugins {
    alias(libs.plugins.prumo.android.library)
    alias(libs.plugins.prumo.hilt)
}

android {
    namespace = "com.henriquesebastiao.prumo.core.data"
}

dependencies {
    api(projects.core.model)
    api(projects.core.network)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(testFixtures(projects.core.network))
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
}
