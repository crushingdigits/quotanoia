plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.roborazzi) apply false
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    parallel = true
    source.setFrom(
        "app/src/main/kotlin",
        "wear/src/main/kotlin",
    )
}
