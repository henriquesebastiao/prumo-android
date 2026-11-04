plugins {
    alias(libs.plugins.prumo.jvm.library)
}

dependencies {
    // JsonObject and JsonPrimitive only: the Home summaries reach each module's PrumoFeature as JSON.
    api(libs.kotlinx.serialization.json)
}
