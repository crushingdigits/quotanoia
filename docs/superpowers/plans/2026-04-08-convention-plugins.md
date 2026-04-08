# Convention Plugins Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a build-logic included build with convention plugins that centralize Android, Kotlin, Compose, Detekt, Lint, Hilt, and test configuration — eliminating duplication between app and wear modules.

**Architecture:** A `build-logic/convention` included build produces 4 Gradle plugins (application, library, feature, jvm-library). Each plugin composes configuration helpers from `dev.qwertyfinger.quotanoia` package. Modules apply a single plugin to get all shared configuration, keeping only module-specific settings.

**Tech Stack:** Gradle convention plugins, `kotlin-dsl`, AGP 9.1.0, Kotlin 2.3.20, Hilt/Dagger 2.58, KSP 2.3.4, Detekt 2.0.0-alpha.2, Roborazzi 1.59.0

**Reference:** Pattern adapted from `/Users/andrii/StudioProjects/common/build-logic/convention/`

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `gradle/libs.versions.toml` | Modify | Add versions, libraries, plugins, bundles for convention plugins |
| `build-logic/settings.gradle.kts` | Create | Included build settings with version catalog |
| `build-logic/convention/build.gradle.kts` | Create | Plugin project with kotlin-dsl and dependencies |
| `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/ProjectExtensions.kt` | Create | Version catalog accessor helpers |
| `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Android.kt` | Create | Android SDK/compile options |
| `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Kotlin.kt` | Create | Kotlin/JVM toolchain, compiler options |
| `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Compose.kt` | Create | Compose plugin, BOM, dependencies |
| `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Logging.kt` | Create | Timber dependency |
| `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Detekt.kt` | Create | Per-module detekt with config and plugins |
| `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Lint.kt` | Create | Lint configuration for app/library/jvm |
| `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Hilt.kt` | Create | Hilt DI for app/library/core variants |
| `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Test.kt` | Create | Unit, Android, Compose UI test setup |
| `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/ScreenshotTest.kt` | Create | Roborazzi screenshot test setup |
| `build-logic/convention/src/main/java/AndroidApplicationConventionPlugin.kt` | Create | Plugin for app modules |
| `build-logic/convention/src/main/java/AndroidLibraryConventionPlugin.kt` | Create | Plugin for library modules |
| `build-logic/convention/src/main/java/AndroidFeatureConventionPlugin.kt` | Create | Plugin for feature modules |
| `build-logic/convention/src/main/java/JvmLibraryConventionPlugin.kt` | Create | Plugin for pure Kotlin/JVM modules |
| `config/detekt/detekt-overrides.yml` | Create (replace detekt.yml) | Expanded detekt overrides with compose/ktlint rules |
| `lint.xml` | Create | Root lint severity overrides |
| `settings.gradle.kts` | Modify | Add `includeBuild("build-logic")` |
| `build.gradle.kts` | Modify | Remove root detekt config |
| `gradle.properties` | Modify | Improved JVM tuning, caching, parallel |
| `app/build.gradle.kts` | Modify | Simplify with convention plugin |
| `wear/build.gradle.kts` | Modify | Simplify with convention plugin |
| `.github/workflows/build.yml` | Modify | Update detekt command |

---

### Task 1: Version catalog updates

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Replace the entire version catalog**

Write the complete `gradle/libs.versions.toml` with all new entries. The file should contain:

```toml
[versions]
agp = "9.1.0"
kotlin = "2.3.20"
compileSdk = "36"
targetSdk = "36"
buildToolsVersion = "36.0.0"

# AndroidX
androidxActivityCompose = "1.9.3"
androidxConcurrentFutures = "1.3.0"
androidxCoreKtx = "1.15.0"
androidxFragmentTesting = "1.8.9"
androidxLifecycle = "2.8.7"
androidxLintGradle = "1.0.0-alpha05"
androidxNavigationCompose = "2.8.5"
androidxTest = "1.7.0"
androidxTestJunit = "1.3.0"

# Compose (BOM-managed, individual libs don't need versions)
composeBom = "2026.03.01"

# Build
androidTools = "32.1.0"
ksp = "2.3.4"

# DI
dagger = "2.58"
hiltAndroid = "1.3.0"

# Quality
detekt = "2.0.0-alpha.2"
detektComposeRules = "0.5.6"

# Networking
okhttp = "4.12.0"

# Testing
espresso = "3.7.0"
junit = "4.13.2"
kotlinxCoroutines = "1.9.0"
mockk = "1.14.9"
roborazzi = "1.59.0"
robolectric = "4.14.1"
truth = "1.4.5"
turbine = "1.2.1"

# UI
glance = "1.1.1"

# Wear
horologist = "0.6.22"
playServicesWearable = "19.0.0"
wearProtolayout = "1.3.0"
wearTiles = "1.4.1"

# Misc
kotlinxSerialization = "1.10.0"
securityCrypto = "1.1.0-alpha06"
timber = "5.0.1"
workmanager = "2.10.0"

[libraries]
# Compose (BOM-managed — no version.ref needed)
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }

# AndroidX
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "androidxActivityCompose" }
androidx-concurrent-futures = { module = "androidx.concurrent:concurrent-futures", version.ref = "androidxConcurrentFutures" }
androidx-concurrent-futures-ktx = { module = "androidx.concurrent:concurrent-futures-ktx", version.ref = "androidxConcurrentFutures" }
androidx-fragment-testing = { module = "androidx.fragment:fragment-testing", version.ref = "androidxFragmentTesting" }
androidx-test-core = { module = "androidx.test:core-ktx", version.ref = "androidxTest" }
androidx-test-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }
androidx-test-junit = { module = "androidx.test.ext:junit-ktx", version.ref = "androidxTestJunit" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidxTest" }
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "androidxCoreKtx" }
lifecycle-runtime = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "androidxLifecycle" }
lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "androidxLifecycle" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "androidxNavigationCompose" }

# DI
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "dagger" }
hilt-android-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "dagger" }
hilt-core = { module = "com.google.dagger:hilt-core", version.ref = "dagger" }
hilt-core-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "dagger" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltAndroid" }

# Coroutines
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }

# Quality
detekt-compose = { module = "io.nlopez.compose.rules:detekt", version.ref = "detektComposeRules" }
detekt-ktlint = { module = "dev.detekt:detekt-rules-ktlint-wrapper", version.ref = "detekt" }

# Networking
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
roborazzi = { module = "io.github.takahirom.roborazzi:roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { module = "io.github.takahirom.roborazzi:roborazzi-compose", version.ref = "roborazzi" }
roborazzi-compose-preview = { module = "io.github.takahirom.roborazzi:roborazzi-compose-preview-scanner-support", version.ref = "roborazzi" }
roborazzi-junit-rule = { module = "io.github.takahirom.roborazzi:roborazzi-junit-rule", version.ref = "roborazzi" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

# UI
glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
glance-material3 = { group = "androidx.glance", name = "glance-material3", version.ref = "glance" }

# Wear
play-services-wearable = { group = "com.google.android.gms", name = "play-services-wearable", version.ref = "playServicesWearable" }
wear-protolayout = { group = "androidx.wear.protolayout", name = "protolayout", version.ref = "wearProtolayout" }
wear-protolayout-expression = { group = "androidx.wear.protolayout", name = "protolayout-expression", version.ref = "wearProtolayout" }
wear-protolayout-material = { group = "androidx.wear.protolayout", name = "protolayout-material", version.ref = "wearProtolayout" }
wear-tiles = { group = "androidx.wear.tiles", name = "tiles", version.ref = "wearTiles" }
wear-tiles-material = { group = "androidx.wear.tiles", name = "tiles-material", version.ref = "wearTiles" }

# Misc
security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
timber = { module = "com.jakewharton.timber:timber", version.ref = "timber" }
workmanager = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workmanager" }

# Dependencies of the included build-logic
android-gradleApiPlugin = { group = "com.android.tools.build", name = "gradle-api", version.ref = "agp" }
android-tools-common = { group = "com.android.tools", name = "common", version.ref = "androidTools" }
androidx-lint-gradle = { group = "androidx.lint", name = "lint-gradle", version.ref = "androidxLintGradle" }
compose-gradlePlugin = { module = "org.jetbrains.kotlin:compose-compiler-gradle-plugin", version.ref = "kotlin" }
detekt-gradlePlugin = { group = "dev.detekt", name = "detekt-gradle-plugin", version.ref = "detekt" }
kotlin-gradlePlugin = { group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version.ref = "kotlin" }
ksp-gradlePlugin = { group = "com.google.devtools.ksp", name = "com.google.devtools.ksp.gradle.plugin", version.ref = "ksp" }
roborazzi-gradlePlugin = { module = "io.github.takahirom.roborazzi:roborazzi-gradle-plugin", version.ref = "roborazzi" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
android-lint = { id = "com.android.lint", version.ref = "agp" }
detekt = { id = "dev.detekt", version.ref = "detekt" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "dagger" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }

# Plugins defined by this project
quotanoia-android-application = { id = "quotanoia.android.application" }
quotanoia-android-feature = { id = "quotanoia.android.feature" }
quotanoia-android-library = { id = "quotanoia.android.library" }
quotanoia-jvm-library = { id = "quotanoia.jvm.library" }

[bundles]
android-test = [
    "androidx-test-espresso-core",
    "androidx-test-junit",
    "androidx-test-core",
    "androidx-test-runner",
]
compose-debug = [
    "compose-ui-test-manifest",
    "compose-ui-tooling",
]
compose-uiTesting = [
    "compose-ui-test-junit4",
    "androidx-test-junit",
    "androidx-test-core",
]
coroutine-testing = [
    "kotlinx-coroutines-test",
    "turbine",
]
detekt = [
    "detekt-compose",
    "detekt-ktlint",
]
hilt-android-ksp = [
    "hilt-android-compiler",
]
hilt-app = [
    "hilt-android",
]
hilt-core = [
    "hilt-core",
]
hilt-core-ksp = [
    "hilt-core-compiler",
]
hilt-library = [
    "hilt-android",
]
screenshot-testing = [
    "androidx-fragment-testing",
    "roborazzi",
    "roborazzi-compose",
    "roborazzi-junit-rule",
    "roborazzi-compose-preview",
]
unit-testing = [
    "junit",
    "kotlin-test",
    "mockk",
    "robolectric",
    "truth",
]
```

- [ ] **Step 2: Validate TOML syntax**

Run: `python3 -c "import tomllib; tomllib.load(open('gradle/libs.versions.toml', 'rb')); print('valid')"`
Expected: `valid`

- [ ] **Step 3: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: update version catalog with convention plugin dependencies"
```

---

### Task 2: Build-logic scaffold

**Files:**
- Create: `build-logic/settings.gradle.kts`
- Create: `build-logic/convention/build.gradle.kts`
- Create: `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/ProjectExtensions.kt`

- [ ] **Step 1: Create `build-logic/settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
```

- [ ] **Step 2: Create `build-logic/convention/build.gradle.kts`**

```kotlin
plugins {
    `kotlin-dsl`
    alias(libs.plugins.android.lint)
}

group = "dev.qwertyfinger.quotanoia.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradleApiPlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    implementation(libs.roborazzi.gradlePlugin)
    lintChecks(libs.androidx.lint.gradle)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.quotanoia.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.quotanoia.android.feature.get().pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.quotanoia.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.quotanoia.jvm.library.get().pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
```

- [ ] **Step 3: Create `ProjectExtensions.kt`**

```kotlin
package dev.qwertyfinger.quotanoia

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency
import java.util.Optional

internal val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal val Optional<Provider<PluginDependency>>.id
    get() = get().get().pluginId
```

- [ ] **Step 4: Commit**

```bash
git add build-logic/
git commit -m "build: scaffold build-logic included build with ProjectExtensions"
```

---

### Task 3: Core configuration helpers

**Files:**
- Create: `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Android.kt`
- Create: `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Kotlin.kt`
- Create: `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Compose.kt`
- Create: `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Logging.kt`

- [ ] **Step 1: Create `Android.kt`**

```kotlin
package dev.qwertyfinger.quotanoia

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

internal fun Project.configureAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildToolsVersion = libs.findVersion("buildToolsVersion").get().toString()
        compileSdk = libs.findVersion("compileSdk").get().toString().toInt()

        defaultConfig {
            if (commonExtension is ApplicationExtension) {
                commonExtension.defaultConfig.targetSdk =
                    libs.findVersion("targetSdk").get().toString().toInt()
            }

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
}
```

Note: if AGP 9.1.0 changed `CommonExtension` type parameter count from 6, adjust the wildcard count to match. A compile error will tell you the exact count needed.

- [ ] **Step 2: Create `Kotlin.kt`**

```kotlin
package dev.qwertyfinger.quotanoia

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        "implementation"(libs.findLibrary("kotlinx-coroutines-android").get())
    }

    configureKotlin<KotlinAndroidProjectExtension>()
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        "implementation"(libs.findLibrary("kotlinx-coroutines-core").get())
    }

    configureKotlin<KotlinJvmProjectExtension>()
}

private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> TODO("Unsupported project extension $this ${T::class}")
    }.apply {
        jvmToolchain(17)
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
```

- [ ] **Step 3: Create `Compose.kt`**

```kotlin
package dev.qwertyfinger.quotanoia

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    apply(plugin = libs.findPlugin("kotlin.compose").id)

    commonExtension.apply {
        buildFeatures {
            compose = true
        }
    }

    dependencies {
        val bom = libs.findLibrary("compose-bom").get()
        "implementation"(platform(bom))
        "implementation"(libs.findLibrary("compose-ui").get())
        "implementation"(libs.findLibrary("compose-ui-graphics").get())
        "implementation"(libs.findLibrary("compose-ui-tooling-preview").get())
        "implementation"(libs.findLibrary("compose-material3").get())
        "implementation"(libs.findLibrary("activity-compose").get())
        "implementation"(libs.findLibrary("navigation-compose").get())
        "debugImplementation"(libs.findBundle("compose.debug").get())
    }
}
```

- [ ] **Step 4: Create `Logging.kt`**

```kotlin
package dev.qwertyfinger.quotanoia

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureTimber() {
    dependencies {
        "implementation"(libs.findLibrary("timber").get())
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Android.kt \
        build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Kotlin.kt \
        build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Compose.kt \
        build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Logging.kt
git commit -m "build: add core configuration helpers (Android, Kotlin, Compose, Logging)"
```

---

### Task 4: Quality configuration helpers

**Files:**
- Create: `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Detekt.kt`
- Create: `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Lint.kt`

- [ ] **Step 1: Create `Detekt.kt`**

```kotlin
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
        buildUponDefaultConfig = true
        basePath = rootProject.layout.projectDirectory.asFile
        config.setFrom(customConfig)
        parallel = true
        ignoredBuildTypes = listOf("release")
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
```

- [ ] **Step 2: Create `Lint.kt`**

```kotlin
package dev.qwertyfinger.quotanoia

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
```

- [ ] **Step 3: Commit**

```bash
git add build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Detekt.kt \
        build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Lint.kt
git commit -m "build: add quality configuration helpers (Detekt, Lint)"
```

---

### Task 5: DI and test configuration helpers

**Files:**
- Create: `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Hilt.kt`
- Create: `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Test.kt`
- Create: `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/ScreenshotTest.kt`

- [ ] **Step 1: Create `Hilt.kt`**

```kotlin
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
```

- [ ] **Step 2: Create `Test.kt`**

```kotlin
package dev.qwertyfinger.quotanoia

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal fun Project.configureAndroidTests(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.testOptions {
        animationsDisabled = true
    }

    dependencies {
        "androidTestImplementation"(libs.findBundle("android.test").get())
        constraints {
            "implementation"(libs.findLibrary("androidx-concurrent-futures").get())
            "implementation"(libs.findLibrary("androidx-concurrent-futures-ktx").get())
        }
    }
}

internal fun Project.configureComposeUiTests() {
    dependencies {
        "testImplementation"(libs.findBundle("compose.uiTesting").get())
    }
}

internal fun Project.configureUnitTests(
    commonExtension: CommonExtension<*, *, *, *, *, *>? = null,
) {
    commonExtension?.testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            }
        }
    }

    dependencies {
        "testImplementation"(libs.findBundle("coroutine.testing").get())
        "testImplementation"(libs.findBundle("unit.testing").get())
    }

    tasks.withType<Test> {
        failOnNoDiscoveredTests = false
    }
}

internal fun LibraryAndroidComponentsExtension.disableUnnecessaryAndroidTests(
    project: Project,
) = beforeVariants { variant ->
    val androidTestDir = project.projectDir.resolve("src/androidTest")
    val hasAndroidTestSourceFiles = androidTestDir.exists() &&
            androidTestDir.walk().any { it.isFile }

    variant.androidTest.enable = variant.androidTest.enable && hasAndroidTestSourceFiles
}
```

- [ ] **Step 3: Create `ScreenshotTest.kt`**

```kotlin
package dev.qwertyfinger.quotanoia

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureScreenshotTests() {
    apply(plugin = libs.findPlugin("roborazzi").id)

    val androidComponents = extensions.findByType(AndroidComponentsExtension::class.java)

    extensions.configure<RoborazziExtension> {
        outputDir.set(file("src/test/screenshots"))

        generateComposePreviewRobolectricTests {
            enable.set(true)
            includePrivatePreviews.set(false)
            packages.set(listOf("dev.qwertyfinger"))
            androidComponents?.finalizeDsl {
                if (it !is CommonExtension<*, *, *, *, *, *>) return@finalizeDsl
                it.namespace?.let { namespace ->
                    packages.set(listOf("$namespace."))
                }
            }
            robolectricConfig.set(
                mapOf(
                    "sdk" to "[34]",
                    "qualifiers" to "com.github.takahirom.roborazzi.RobolectricDeviceQualifiers.Pixel8a",
                    "application" to "android.app.Application::class",
                ),
            )
        }
    }

    dependencies {
        "testImplementation"(libs.findBundle("screenshot.testing").get())
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Hilt.kt \
        build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/Test.kt \
        build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/ScreenshotTest.kt
git commit -m "build: add DI and test configuration helpers (Hilt, Test, ScreenshotTest)"
```

---

### Task 6: Convention plugin classes

**Files:**
- Create: `build-logic/convention/src/main/java/AndroidApplicationConventionPlugin.kt`
- Create: `build-logic/convention/src/main/java/AndroidLibraryConventionPlugin.kt`
- Create: `build-logic/convention/src/main/java/AndroidFeatureConventionPlugin.kt`
- Create: `build-logic/convention/src/main/java/JvmLibraryConventionPlugin.kt`

- [ ] **Step 1: Create `AndroidApplicationConventionPlugin.kt`**

```kotlin
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

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                configureAndroidTests(this)
                configureCompose(this)
                configureComposeUiTests()
                configureDetekt()
                configureHilt()
                configureKotlinAndroid(this)
                configureLint()
                configureTimber()
                configureScreenshotTests()
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
```

- [ ] **Step 2: Create `AndroidLibraryConventionPlugin.kt`**

```kotlin
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
```

- [ ] **Step 3: Create `AndroidFeatureConventionPlugin.kt`**

```kotlin
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
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
import dev.qwertyfinger.quotanoia.disableUnnecessaryAndroidTests
import dev.qwertyfinger.quotanoia.id
import dev.qwertyfinger.quotanoia.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.findPlugin("android.library").id)
            apply(plugin = libs.findPlugin("kotlin.android").id)
            apply(plugin = libs.findPlugin("ksp").id)

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
                configureAndroidTests(this)
                configureCompose(this)
                configureComposeUiTests()
                configureDetekt()
                configureHilt()
                configureKotlinAndroid(this)
                configureLint()
                configureScreenshotTests()
                configureTimber()
                configureUnitTests(this)

                buildFeatures {
                    viewBinding = true
                }

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
```

- [ ] **Step 4: Create `JvmLibraryConventionPlugin.kt`**

```kotlin
import dev.qwertyfinger.quotanoia.configureDetekt
import dev.qwertyfinger.quotanoia.configureHilt
import dev.qwertyfinger.quotanoia.configureKotlinJvm
import dev.qwertyfinger.quotanoia.configureLint
import dev.qwertyfinger.quotanoia.configureUnitTests
import dev.qwertyfinger.quotanoia.id
import dev.qwertyfinger.quotanoia.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.findPlugin("kotlin.jvm").id)
            apply(plugin = libs.findPlugin("ksp").id)

            extensions.configure<JavaPluginExtension> {
                configureDetekt()
                configureHilt()
                configureKotlinJvm()
                configureLint()
                configureUnitTests()
            }
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add build-logic/convention/src/main/java/AndroidApplicationConventionPlugin.kt \
        build-logic/convention/src/main/java/AndroidLibraryConventionPlugin.kt \
        build-logic/convention/src/main/java/AndroidFeatureConventionPlugin.kt \
        build-logic/convention/src/main/java/JvmLibraryConventionPlugin.kt
git commit -m "build: add convention plugin classes (application, library, feature, jvm)"
```

---

### Task 7: Detekt config and root lint.xml

**Files:**
- Delete: `config/detekt/detekt.yml`
- Create: `config/detekt/detekt-overrides.yml`
- Create: `lint.xml`

- [ ] **Step 1: Delete old detekt config and create `detekt-overrides.yml`**

Delete `config/detekt/detekt.yml` and create `config/detekt/detekt-overrides.yml` with this content:

```yaml
complexity:
  LargeClass:
    excludes: ['**/test/**', '**/androidTest/**']
  LongMethod:
    excludes: ['**/test/**', '**/androidTest/**']
  LongParameterList:
    active: false
  TooManyFunctions:
    ignoreAnnotatedFunctions: ['Preview']

coroutines:
  CoroutineLaunchedInTestWithoutRunTest:
    active: true
  GlobalCoroutineUsage:
    active: true
  SuspendFunInFinallySection:
    active: true
  SuspendFunSwallowedCancellation:
    active: true
  SuspendFunWithCoroutineScopeReceiver:
    active: true

exceptions:
  ErrorUsageWithThrowable:
    active: true

naming:
  FunctionNaming:
    ignoreAnnotated: ['Composable']
  MatchingDeclarationName:
    active: false

performance:
  CouldBeSequence:
    active: true
  UnnecessaryInitOnArray:
    active: true
  UnnecessaryPartOfBinaryExpression:
    active: true
  UnnecessaryTypeCasting:
    active: true

potential-bugs:
  CastNullableToNonNullableType:
    excludes: ['**/test/**', '**/androidTest/**']
  CastToNullableType:
    active: true
  CharArrayToStringCall:
    active: true
  DontDowncastCollectionTypes:
    active: true
  ElseCaseInsteadOfExhaustiveWhen:
    active: true
  MissingSuperCall:
    active: true
  NullCheckOnMutableProperty:
    active: true
  NullableToStringCall:
    active: true
  PropertyUsedBeforeDeclaration:
    active: true
  UnconditionalJumpStatementInLoop:
    active: true
  UnnecessaryNotNullCheck:
    active: true

style:
  DestructuringDeclarationWithTooManyEntries:
    active: false
  DoubleNegativeExpression:
    active: true
  ForbiddenComment:
    active: false
  MagicNumber:
    ignorePropertyDeclaration: true
  MaxLineLength:
    excludes: ['**/test/**', '**/androidTest/**', '**/*.kts']
    excludeCommentStatements: true
  RangeUntilInsteadOfRangeTo:
    active: true
  RedundantConstructorKeyword:
    active: true
  ReturnCount:
    excludedFunctions:
      - 'equals'
      - 'compareTo'
  UnnecessaryBracesAroundTrailingLambda:
    active: true
  UnnecessaryInnerClass:
    active: true
  UnnecessaryLet:
    active: true
  UnnecessaryParentheses:
    active: true
    allowForUnclearPrecedence: true
  UnnecessaryReversed:
    active: true
  UnusedImport:
    active: true
    additionalOperatorSet: []
  UnusedPrivateFunction:
    ignoreAnnotated: ['Preview']
  UseEmptyCounterpart:
    active: true
  UseIfEmptyOrIfBlank:
    active: true
  UseLet:
    active: true

Compose:
  Material2:
    active: true
  PreviewPublic:
    active: false
  UnstableCollections:
    active: true

ktlint:
  code_style: 'ktlint_official'
  ChainMethodContinuation:
    active: true
  FunctionSignature:
    forceMultilineWhenParameterCountGreaterOrEqualThan: 2
  MaximumLineLength:
    ignoreBackTickedIdentifier: true
    excludes: ['**/test/**', '**/androidTest/**']
  TrailingCommaOnCallSite:
    useTrailingCommaOnCallSite: true
  TrailingCommaOnDeclarationSite:
    useTrailingCommaOnDeclarationSite: true
```

- [ ] **Step 2: Create root `lint.xml`**

Create `lint.xml` at the project root:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<lint>
    <issue id="AndroidGradlePluginVersion" severity="warning" />
    <issue id="GradleDependency" severity="warning" />
    <issue id="LintBaseline" severity="ignore" />
    <issue id="LintBaselineFixed" severity="informational" />
    <issue id="NewerVersionAvailable" severity="warning" />
    <issue id="SyntheticAccessor" severity="ignore" />
    <issue id="TypographyQuotes" severity="informational" />
    <issue id="UnusedIds" severity="informational" />
</lint>
```

- [ ] **Step 3: Commit**

```bash
git rm config/detekt/detekt.yml
git add config/detekt/detekt-overrides.yml lint.xml
git commit -m "build: expand detekt overrides with compose/ktlint rules, add root lint.xml"
```

---

### Task 8: Project-level configuration updates

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts` (root)
- Modify: `gradle.properties`

- [ ] **Step 1: Update `settings.gradle.kts`**

Replace the entire file with:

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "quotanoia"
include(":app")
include(":wear")
```

- [ ] **Step 2: Update root `build.gradle.kts`**

Replace the entire file with:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
}
```

- [ ] **Step 3: Update `gradle.properties`**

Replace the entire file with:

```properties
org.gradle.jvmargs=-Dfile.encoding=UTF-8 -XX:+UseG1GC -XX:SoftRefLRUPolicyMSPerMB=1 -XX:ReservedCodeCacheSize=256m -XX:+HeapDumpOnOutOfMemoryError -Xmx3g -Xms3g

org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.parallel=true

android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true

roborazzi.record.filePathStrategy=relativePathFromRoborazziContextOutputDirectory
```

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties
git commit -m "build: update project config for convention plugins and improved build settings"
```

---

### Task 9: Module build file simplification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `wear/build.gradle.kts`

- [ ] **Step 1: Replace `app/build.gradle.kts`**

Replace the entire file with:

```kotlin
plugins {
    alias(libs.plugins.quotanoia.android.application)
}

android {
    namespace = "dev.qwertyfinger.quotanoia"

    defaultConfig {
        applicationId = "dev.qwertyfinger.quotanoia"
        minSdk = 28
        versionCode = 1
        versionName = "0.1.0-poc"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    testOptions {
        unitTests {
            all {
                if (project.hasProperty("excludeScreenshots")) {
                    it.exclude("**/ScreenshotTests.class")
                }
            }
        }
    }
}

dependencies {
    // App-specific dependencies only — common deps come from convention plugin
    implementation(libs.compose.material.icons)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.workmanager)
    implementation(libs.security.crypto)
    implementation(libs.play.services.wearable)

    testImplementation("org.json:json:20231013")
}
```

- [ ] **Step 2: Replace `wear/build.gradle.kts`**

Replace the entire file with:

```kotlin
plugins {
    alias(libs.plugins.quotanoia.android.application)
}

android {
    namespace = "dev.qwertyfinger.quotanoia.wear"

    defaultConfig {
        applicationId = "dev.qwertyfinger.quotanoia"
        minSdk = 30
        versionCode = 1
        versionName = "0.1.0-poc"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    // Wear-specific dependencies only — common deps come from convention plugin
    implementation(libs.core.ktx)
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material)
    implementation(libs.wear.protolayout.expression)
    implementation(libs.play.services.wearable)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.google.guava:guava:33.3.1-android")
}
```

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts wear/build.gradle.kts
git commit -m "build: simplify app and wear modules with convention plugin"
```

---

### Task 10: CI workflow update and build verification

**Files:**
- Modify: `.github/workflows/build.yml`

- [ ] **Step 1: Update detekt command in CI**

In `.github/workflows/build.yml`, change the detekt job's run command from:

```yaml
      - run: ./gradlew detekt
```

To:

```yaml
      - run: ./gradlew detektDebug detektDebugUnitTest
```

- [ ] **Step 2: Validate YAML**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml'))"`
Expected: No output (valid YAML)

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: update detekt command for per-module convention plugin setup"
```

- [ ] **Step 4: Run full build to verify everything compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

If this fails, diagnose and fix. Common issues:
- `CommonExtension` type parameter count mismatch with AGP 9.x — adjust wildcard count in Android.kt, Kotlin.kt, Compose.kt, Test.kt, ScreenshotTest.kt
- `android-tools-common` version mismatch — check what version matches AGP 9.1.0
- Missing version catalog entries — verify all `findLibrary`/`findPlugin`/`findBundle` names match catalog keys exactly

- [ ] **Step 5: Run tests to verify nothing broke**

Run: `./gradlew testDebugUnitTest -PexcludeScreenshots`
Expected: BUILD SUCCESSFUL with all tests passing

- [ ] **Step 6: Run detekt with new config**

Run: `./gradlew detektDebug detektDebugUnitTest`
Expected: BUILD SUCCESSFUL (may have new warnings from expanded rules — review and fix or baseline as needed)

- [ ] **Step 7: Run lint**

Run: `./gradlew :app:lintDebug :wear:lintDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Final commit if any fixes were needed**

```bash
git add -A
git commit -m "build: fix convention plugin integration issues"
```
