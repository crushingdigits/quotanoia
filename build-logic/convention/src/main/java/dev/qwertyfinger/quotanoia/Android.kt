package dev.qwertyfinger.quotanoia

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

internal fun Project.configureAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildToolsVersion = libs.findVersion("buildToolsVersion").get().toString()
        compileSdk = libs.findVersion("compileSdk").get().toString().toInt()

        defaultConfig {
            if (commonExtension is ApplicationExtension) {
                commonExtension.defaultConfig.targetSdk =
                    libs.findVersion("targetSdk").get().toString().toInt()
            }

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
}
