import dev.qwertyfinger.quotanoia.configureDetekt
import dev.qwertyfinger.quotanoia.configureHilt
import dev.qwertyfinger.quotanoia.configureKotlinJvm
import dev.qwertyfinger.quotanoia.configureLint
import dev.qwertyfinger.quotanoia.configureUnitTests
import dev.qwertyfinger.quotanoia.id
import dev.qwertyfinger.quotanoia.libs
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
