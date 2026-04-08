package dev.qwertyfinger.quotanoia

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceTask
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal fun Project.configureDetekt() {

    apply(plugin = libs.findPlugin("detekt").id)

    dependencies {
        "detektPlugins"(libs.findBundle("detekt").get())
    }

    extensions.configure<DetektExtension> {
        val buildLogicDir = gradle.includedBuild("build-logic").projectDir
        val customConfig = buildLogicDir.resolve("../config/detekt/detekt-overrides.yml").normalize()
        require(customConfig.isFile) {
            "Custom Detekt config not found at ${customConfig.absolutePath}. " +
                "Expected ../config/detekt/detekt-overrides.yml next to the included build-logic."
        }
        buildUponDefaultConfig.set(true)
        basePath.set(rootProject.layout.projectDirectory.asFile)
        config.setFrom(customConfig)
        parallel.set(true)
        ignoredBuildTypes.set(listOf("release"))
    }

    tasks.withType<Detekt>().configureEach {
        excludeGeneratedCode()

        reports {
            html.required.set(true)
            markdown.required.set(false)
            sarif.required.set(false)
            checkstyle.required.set(false)
        }
    }
    tasks.withType<DetektCreateBaselineTask>().configureEach {
        excludeGeneratedCode()
    }
}

private fun SourceTask.excludeGeneratedCode() {
    exclude { fileTreeElement ->
        val normalizedPath = fileTreeElement.file.path.replace('\\', '/')
        normalizedPath.startsWith("build/") || normalizedPath.contains("/build/")
    }
}
