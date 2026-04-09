package dev.crushingdigits.quotanoia

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureCompose(
    commonExtension: CommonExtension,
) {
    apply(plugin = libs.findPlugin("kotlin.compose").id)

    commonExtension.apply {
        buildFeatures.compose = true
    }

    dependencies {
        val bom = libs.findLibrary("compose-bom").get()
        "implementation"(platform(bom))
        "implementation"(libs.findLibrary("compose-ui").get())
        "implementation"(libs.findLibrary("compose-ui-graphics").get())
        "implementation"(libs.findLibrary("compose-ui-tooling-preview").get())
        "implementation"(libs.findLibrary("compose-material3").get())
        "implementation"(libs.findLibrary("activity-compose").get())
        "implementation"(libs.findLibrary("navigation-compose").get())
        "debugImplementation"(libs.findBundle("compose.debug").get())
    }
}
