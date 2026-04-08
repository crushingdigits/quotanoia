package dev.qwertyfinger.quotanoia

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureHilt() {
    when {
        pluginManager.hasPlugin(libs.findPlugin("android.application").id) -> {
            apply(plugin = libs.findPlugin("hilt.android").id)
            dependencies {
                "implementation"(libs.findBundle("hilt.app").get())
                "ksp"(libs.findBundle("hilt.android.ksp").get())
            }
        }

        pluginManager.hasPlugin(libs.findPlugin("android.library").id) -> {
            apply(plugin = libs.findPlugin("hilt.android").id)
            dependencies {
                "implementation"(libs.findBundle("hilt.library").get())
                "ksp"(libs.findBundle("hilt.android.ksp").get())
            }
        }

        else -> {
            dependencies {
                "implementation"(libs.findBundle("hilt.core").get())
                "ksp"(libs.findBundle("hilt.core.ksp").get())
            }
        }
    }
}
