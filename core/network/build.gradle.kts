plugins {
    alias(libs.plugins.prumo.jvm.library)
    alias(libs.plugins.prumo.hilt)
    alias(libs.plugins.kotlin.serialization)
    `java-test-fixtures`
}

dependencies {
    api(projects.core.model)
    api(libs.okhttp)
    api(libs.retrofit)
    api(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.core)

    // The server's OpenAPI and real responses, for this module's tests and the features' (contract,
    // Home summaries): testImplementation(testFixtures(projects.core.network)).
    testFixturesApi(libs.kotlinx.serialization.json)
    testFixturesApi(libs.junit)

    testImplementation(libs.okhttp.mockwebserver)
}
