package dev.crushingdigits.quotanoia

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

internal fun Project.configureLint() {
    when {
        pluginManager.hasPlugin(libs.findPlugin("android.application").id) ->
            configure<ApplicationExtension> { lint(Lint::configure) }

        pluginManager.hasPlugin(libs.findPlugin("android.library").id) ->
            configure<LibraryExtension> { lint(Lint::configure) }

        else -> {
            apply(plugin = libs.findPlugin("android.lint").id)
            configure<Lint>(configuration = Lint::configure)
        }
    }
}

private fun Lint.configure() {
    checkDependencies = true
    htmlReport = true
    xmlReport = false
    textReport = false
}
