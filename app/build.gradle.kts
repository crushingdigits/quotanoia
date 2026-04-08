plugins {
    alias(libs.plugins.quotanoia.android.application)
}

android {
    namespace = "dev.qwertyfinger.quotanoia"

    defaultConfig {
        applicationId = "dev.qwertyfinger.quotanoia"
        minSdk = 28
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
    // App-specific dependencies only — common deps come from convention plugin
    implementation(projects.core.coroutines)
    implementation(libs.compose.material.icons)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.workmanager)
    implementation(libs.security.crypto)
    implementation(libs.play.services.wearable)

    testImplementation("org.json:json:20231013")
}
