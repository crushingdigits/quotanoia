import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import dev.qwertyfinger.quotanoia.configureAndroid
import dev.qwertyfinger.quotanoia.configureAndroidTests
import dev.qwertyfinger.quotanoia.configureDetekt
import dev.qwertyfinger.quotanoia.configureHilt
import dev.qwertyfinger.quotanoia.configureKotlinAndroid
import dev.qwertyfinger.quotanoia.configureLint
import dev.qwertyfinger.quotanoia.configureTimber
import dev.qwertyfinger.quotanoia.configureUnitTests
import dev.qwertyfinger.quotanoia.disableUnnecessaryAndroidTests
import dev.qwertyfinger.quotanoia.id
import dev.qwertyfinger.quotanoia.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.findPlugin("android.library").id)
            apply(plugin = libs.findPlugin("kotlin.android").id)
            apply(plugin = libs.findPlugin("ksp").id)

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
                configureAndroidTests(this)
                configureDetekt()
                configureHilt()
                configureKotlinAndroid(this)
                configureLint()
                configureTimber()
                configureUnitTests(this)

                buildTypes {
                    getByName("debug") {
                        manifestPlaceholders["appAuthRedirectScheme"] = ""
                    }
                }
            }

            extensions.configure<LibraryAndroidComponentsExtension> {
                disableUnnecessaryAndroidTests(project)
            }
        }
    }
}
