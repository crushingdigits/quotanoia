import dev.crushingdigits.quotanoia.configureDetekt
import dev.crushingdigits.quotanoia.configureHilt
import dev.crushingdigits.quotanoia.configureKotlinJvm
import dev.crushingdigits.quotanoia.configureLint
import dev.crushingdigits.quotanoia.configureUnitTests
import dev.crushingdigits.quotanoia.id
import dev.crushingdigits.quotanoia.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.findPlugin("kotlin.jvm").id)
            apply(plugin = libs.findPlugin("ksp").id)

            configureDetekt()
            configureHilt()
            configureKotlinJvm()
            configureLint()
            configureUnitTests()
        }
    }
}
