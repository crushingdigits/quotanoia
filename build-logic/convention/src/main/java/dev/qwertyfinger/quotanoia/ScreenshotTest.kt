package dev.qwertyfinger.quotanoia

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureScreenshotTests() {
    apply(plugin = libs.findPlugin("roborazzi").id)

    val androidComponents = extensions.findByType(AndroidComponentsExtension::class.java)

    extensions.configure<RoborazziExtension> {
        outputDir.set(file("src/test/screenshots"))

        generateComposePreviewRobolectricTests {
            enable.set(true)
            includePrivatePreviews.set(false)
            packages.set(listOf("dev.qwertyfinger"))
            androidComponents?.finalizeDsl {
                if (it !is CommonExtension) return@finalizeDsl
                it.namespace?.let { namespace ->
                    packages.set(listOf("$namespace."))
                }
            }
            robolectricConfig.set(
                mapOf(
                    "sdk" to "[34]",
                    "qualifiers" to "com.github.takahirom.roborazzi.RobolectricDeviceQualifiers.Pixel8a",
                    "application" to "android.app.Application::class",
                ),
            )
        }
    }

    dependencies {
        "testImplementation"(libs.findBundle("screenshot.testing").get())
    }
}
