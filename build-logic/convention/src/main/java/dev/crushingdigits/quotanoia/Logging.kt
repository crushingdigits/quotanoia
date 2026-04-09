package dev.crushingdigits.quotanoia

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureTimber() {
    dependencies {
        "implementation"(libs.findLibrary("timber").get())
    }
}
