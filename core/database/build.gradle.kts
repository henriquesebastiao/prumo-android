plugins {
    alias(libs.plugins.prumo.android.library)
    alias(libs.plugins.prumo.hilt)
}

android {
    namespace = "com.henriquesebastiao.prumo.core.database"
}

ksp {
    // The schema of each version, for the migrations (and their tests) once the queue of pending records arrives.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
}
