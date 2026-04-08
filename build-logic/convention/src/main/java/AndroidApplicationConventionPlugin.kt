import com.android.build.api.dsl.ApplicationExtension
import dev.qwertyfinger.quotanoia.configureAndroid
import dev.qwertyfinger.quotanoia.configureAndroidTests
import dev.qwertyfinger.quotanoia.configureCompose
import dev.qwertyfinger.quotanoia.configureComposeUiTests
import dev.qwertyfinger.quotanoia.configureDetekt
import dev.qwertyfinger.quotanoia.configureHilt
import dev.qwertyfinger.quotanoia.configureKotlinAndroid
import dev.qwertyfinger.quotanoia.configureLint
import dev.qwertyfinger.quotanoia.configureScreenshotTests
import dev.qwertyfinger.quotanoia.configureTimber
import dev.qwertyfinger.quotanoia.configureUnitTests
import dev.qwertyfinger.quotanoia.id
import dev.qwertyfinger.quotanoia.libs
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
