plugins {
    alias(libs.plugins.quotanoia.android.application)
}

android {
    namespace = "dev.qwertyfinger.quotanoia.wear"

    defaultConfig {
        applicationId = "dev.qwertyfinger.quotanoia"
        minSdk = 30
        versionCode = 1
        versionName = "0.1.0-poc"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    // Wear-specific dependencies only — common deps come from convention plugin
    implementation(libs.core.ktx)
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material)
    implementation(libs.wear.protolayout.expression)
    implementation(libs.play.services.wearable)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.google.guava:guava:33.3.1-android")
}
