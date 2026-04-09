package dev.crushingdigits.quotanoia

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal fun Project.configureAndroidTests(
    commonExtension: CommonExtension,
) {
    commonExtension.testOptions.animationsDisabled = true

    dependencies {
        "androidTestImplementation"(libs.findBundle("android.test").get())
        constraints {
            "implementation"(libs.findLibrary("androidx-concurrent-futures").get())
            "implementation"(libs.findLibrary("androidx-concurrent-futures-ktx").get())
        }
    }
}

internal fun Project.configureComposeUiTests() {
    dependencies {
        "testImplementation"(libs.findBundle("compose.uiTesting").get())
    }
}

internal fun Project.configureUnitTests(
    commonExtension: CommonExtension? = null,
) {
    commonExtension?.testOptions?.unitTests?.apply {
        isIncludeAndroidResources = true
        isReturnDefaultValues = true
        all {
            it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
        }
    }

    dependencies {
        "testImplementation"(libs.findBundle("coroutine.testing").get())
        "testImplementation"(libs.findBundle("unit.testing").get())
    }

    tasks.withType<Test> {
        failOnNoDiscoveredTests.set(false)
    }
}

internal fun LibraryAndroidComponentsExtension.disableUnnecessaryAndroidTests(
    project: Project,
) = beforeVariants { variant ->
    val androidTestDir = project.projectDir.resolve("src/androidTest")
    val hasAndroidTestSourceFiles = androidTestDir.exists() &&
            androidTestDir.walk().any { it.isFile }

    variant.androidTest.enable = variant.androidTest.enable && hasAndroidTestSourceFiles
}
