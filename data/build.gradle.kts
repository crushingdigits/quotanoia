plugins {
    alias(libs.plugins.quotanoia.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.crushingdigits.quotanoia.data"
}

dependencies {
    implementation(projects.core.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.security.crypto)
    implementation(libs.play.services.wearable)
}
