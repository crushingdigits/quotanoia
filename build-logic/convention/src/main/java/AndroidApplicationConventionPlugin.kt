import com.android.build.api.dsl.ApplicationExtension
import dev.crushingdigits.quotanoia.configureAndroid
import dev.crushingdigits.quotanoia.configureAndroidTests
import dev.crushingdigits.quotanoia.configureCompose
import dev.crushingdigits.quotanoia.configureComposeUiTests
import dev.crushingdigits.quotanoia.configureDetekt
import dev.crushingdigits.quotanoia.configureHilt
import dev.crushingdigits.quotanoia.configureKotlinAndroid
import dev.crushingdigits.quotanoia.configureLint
import dev.crushingdigits.quotanoia.configureScreenshotTests
import dev.crushingdigits.quotanoia.configureTimber
import dev.crushingdigits.quotanoia.configureUnitTests
import dev.crushingdigits.quotanoia.id
import dev.crushingdigits.quotanoia.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.findPlugin("android.application").id)
            apply(plugin = libs.findPlugin("kotlin.android").id)
            apply(plugin = libs.findPlugin("kotlin.serialization").id)
            apply(plugin = libs.findPlugin("ksp").id)

            configureHilt()
            configureDetekt()
            configureLint()
            configureTimber()
            configureScreenshotTests()
            configureComposeUiTests()

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                configureAndroidTests(this)
                configureCompose(this)
                configureKotlinAndroid(this)
                configureUnitTests(this)

                buildFeatures {
                    buildConfig = true
                    viewBinding = true
                }

                lint.baseline = file("lint-baseline.xml")
            }
        }
    }
}
