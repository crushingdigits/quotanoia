import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import dev.crushingdigits.quotanoia.configureAndroid
import dev.crushingdigits.quotanoia.configureAndroidTests
import dev.crushingdigits.quotanoia.configureDetekt
import dev.crushingdigits.quotanoia.configureHilt
import dev.crushingdigits.quotanoia.configureKotlinAndroid
import dev.crushingdigits.quotanoia.configureLint
import dev.crushingdigits.quotanoia.configureTimber
import dev.crushingdigits.quotanoia.configureUnitTests
import dev.crushingdigits.quotanoia.disableUnnecessaryAndroidTests
import dev.crushingdigits.quotanoia.id
import dev.crushingdigits.quotanoia.libs
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

            configureHilt()
            configureDetekt()
            configureLint()
            configureTimber()

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
                configureAndroidTests(this)
                configureKotlinAndroid(this)
                configureUnitTests(this)
            }

            extensions.configure<LibraryAndroidComponentsExtension> {
                disableUnnecessaryAndroidTests(project)
            }
        }
    }
}
