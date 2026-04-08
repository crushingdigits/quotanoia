# Convention Plugins Design

## Context

Quotanoia currently has 2 modules (`app`, `wear`) with duplicated build configuration — Android SDK versions, Kotlin settings, Compose setup, Lint config, and dependency declarations are repeated across both `build.gradle.kts` files. Detekt is configured at the root level.

As the project grows and modularizes, this duplication becomes harder to maintain. The `common` repository (StudioProjects/common) has a proven convention plugins architecture that centralizes build configuration into reusable Gradle plugins. This design replicates that architecture for quotanoia.

## Reference

Pattern source: `/Users/andrii/StudioProjects/common/build-logic/convention/`

## Changes

### 1. Build-Logic Included Build

Create `build-logic/convention/` as an included Gradle build that produces 4 convention plugins. Uses `kotlin-dsl` and `java-gradle-plugin`.

**Directory structure:**
```
build-logic/
├── settings.gradle.kts
└── convention/
    ├── build.gradle.kts
    └── src/main/java/
        ├── AndroidApplicationConventionPlugin.kt
        ├── AndroidLibraryConventionPlugin.kt
        ├── AndroidFeatureConventionPlugin.kt
        ├── JvmLibraryConventionPlugin.kt
        └── dev/qwertyfinger/quotanoia/
            ├── Android.kt
            ├── Compose.kt
            ├── Detekt.kt
            ├── Hilt.kt
            ├── Kotlin.kt
            ├── Lint.kt
            ├── Logging.kt
            ├── ProjectExtensions.kt
            ├── ScreenshotTest.kt
            └── Test.kt
```

Convention plugin classes live at `src/main/java/` root (no package). Configuration helpers live under `dev.qwertyfinger.quotanoia` package. This matches the nowinandroid/common convention.

**`build-logic/settings.gradle.kts`:**
- Configures `dependencyResolutionManagement` to use the root project's version catalog
- Repositories: Google, Maven Central, Gradle Plugin Portal

**`build-logic/convention/build.gradle.kts`:**
- `kotlin-dsl` plugin
- Java 17 source/target compatibility
- `compileOnly` dependencies on: AGP, Android tools common, Compose Gradle plugin, Detekt Gradle plugin, Kotlin Gradle plugin, KSP Gradle plugin
- `implementation` dependency on Roborazzi Gradle plugin (needs runtime access)
- `validatePlugins` with `enableStricterValidation = true`, `failOnWarning = true`
- `gradlePlugin` block registering all 4 plugins with IDs from version catalog

### 2. Convention Plugins

**`quotanoia.android.application`** — Applied by `app` and `wear`:
- Applies: `com.android.application`, `org.jetbrains.kotlin.android`, `com.google.devtools.ksp`, `org.jetbrains.kotlin.plugin.serialization` (serialization is a plugin-only apply, no dedicated helper needed)
- Calls: `configureAndroid()`, `configureKotlinAndroid()`, `configureCompose()`, `configureDetekt()`, `configureHilt()` (app variant), `configureLint()`, `configureTimber()`, `configureScreenshotTests()`, `configureUnitTests()`, `configureAndroidTests()`, `configureComposeUiTests()`
- Sets: `buildConfig = true`, `viewBinding = true`
- Sets lint baseline: `lint-baseline.xml`

**`quotanoia.android.library`** — For future library modules:
- Applies: `com.android.library`, `org.jetbrains.kotlin.android`, `com.google.devtools.ksp`
- Calls: `configureAndroid()`, `configureKotlinAndroid()`, `configureDetekt()`, `configureHilt()` (library variant), `configureLint()`, `configureTimber()`, `configureUnitTests()`, `configureAndroidTests()`
- Disables unnecessary Android instrumented tests for modules without `src/androidTest` files

**`quotanoia.android.feature`** — For future feature modules:
- Applies: `com.android.library`, `org.jetbrains.kotlin.android`, `com.google.devtools.ksp`
- Calls: Everything in library + `configureCompose()`, `configureComposeUiTests()`, `configureScreenshotTests()`
- Sets: `viewBinding = true`

**`quotanoia.jvm.library`** — For future pure Kotlin modules:
- Applies: `org.jetbrains.kotlin.jvm`, `com.google.devtools.ksp`
- Calls: `configureKotlinJvm()`, `configureDetekt()`, `configureHilt()` (core variant), `configureLint()`, `configureUnitTests()`

### 3. Configuration Helpers

All helpers in `dev.qwertyfinger.quotanoia` package, all `internal fun Project.*()`.

**Android.kt** — `configureAndroid(CommonExtension)`
- SDK versions from version catalog: compileSdk = 36, targetSdk = 36 (ApplicationExtension only)
- buildToolsVersion from catalog
- testInstrumentationRunner = `androidx.test.runner.AndroidJUnitRunner`
- minSdk is NOT set here — each module sets its own

**Kotlin.kt** — `configureKotlinAndroid(CommonExtension)` + `configureKotlinJvm()`
- JVM toolchain: 17
- Java source/target: 17
- Android variant: adds `kotlinx-coroutines-android`
- JVM variant: adds `kotlinx-coroutines-core`
- Compiler options: `@OptIn` for ExperimentalTime, ExperimentalCoroutinesApi; free compiler args for annotation default target, when guards, context parameters

**Compose.kt** — `configureCompose(CommonExtension)`
- Applies `org.jetbrains.kotlin.plugin.compose`
- `compose = true` build feature
- Dependencies: Compose BOM, Material3, UI, UI Graphics, UI Tooling Preview, Activity Compose, Navigation Compose
- Debug dependencies: UI Tooling, UI Test Manifest

**Detekt.kt** — `configureDetekt()`
- Applies detekt plugin per-module (not root-level)
- `buildUponDefaultConfig = true`
- Config: `config/detekt/detekt-overrides.yml` (resolved via `gradle.includedBuild("build-logic")` path)
- `parallel = true`, `ignoredBuildTypes = ["release"]`
- Adds detekt plugins bundle (detekt-compose, detekt-ktlint) via `detektPlugins` configuration
- HTML report enabled; markdown, sarif, checkstyle disabled
- No GitLab custom report (unlike common)
- Excludes generated code from `build/` directories

**Lint.kt** — `configureLint()`
- Conditional: checks for ApplicationExtension, LibraryExtension, or applies standalone `com.android.lint`
- `checkDependencies = true`, `htmlReport = true`, `xmlReport = false`, `textReport = false`

**Hilt.kt** — `configureHilt()`
- App variant: `com.google.dagger.hilt.android` plugin + `hilt.app` bundle + `hilt.android.ksp` dependency
- Library variant: `com.google.dagger.hilt.android` plugin + `hilt.library` bundle + `hilt.android.ksp` dependency
- Core (JVM) variant: no plugin, `hilt.core` bundle + `hilt.core.ksp` dependency

**Logging.kt** — `configureTimber()`
- Adds `timber` dependency

**ScreenshotTest.kt** — `configureScreenshotTests()`
- Applies Roborazzi plugin
- Output dir: `src/test/screenshots`
- `generateComposePreviewRobolectricTests` enabled
- Package filtering: `dev.qwertyfinger` (uses module namespace with trailing dot for exact match)
- Robolectric config: SDK 34, device qualifiers
- Dependencies: `screenshot.testing` bundle
- Note: No custom `testerQualifiedClassName` — use Roborazzi defaults (quotanoia doesn't need the `CustomComposePreviewTester` from common)

**Test.kt** — Three functions:
- `configureUnitTests(CommonExtension?)`: For Android — `isIncludeAndroidResources = true`, `isReturnDefaultValues = true`, Robolectric hardware render mode. For JVM — `failOnNoDiscoveredTests = false`. Both add: `coroutine.testing` bundle, `unit.testing` bundle.
- `configureAndroidTests(CommonExtension)`: `animationsDisabled = true`, adds `android.test` bundle
- `configureComposeUiTests()`: Adds `compose.uiTesting` bundle

**ProjectExtensions.kt** — Helper properties:
- `Project.libs`: Access version catalog with type safety
- `Optional<Provider<PluginDependency>>.id`: Extract plugin ID

### 4. Detekt Configuration Update

Rename `config/detekt/detekt.yml` to `config/detekt/detekt-overrides.yml`.

Expand with rules from common (adapted for quotanoia):

- **Complexity:** LargeClass/LongMethod excluded from test sources, LongParameterList disabled
- **Coroutines:** All rules enabled (GlobalCoroutineUsage, SuspendFunSwallowedCancellation, etc.)
- **Naming:** FunctionNaming allows PascalCase for Composables, MatchingDeclarationName disabled
- **Performance:** CouldBeSequence, UnnecessaryPartOfBinaryExpression, etc.
- **Potential bugs:** CastNullableToNonNullableType (excluded from tests), ElseCaseInsteadOfExhaustiveWhen, MissingSuperCall, NullableToStringCall, etc.
- **Style:** MagicNumber ignores property declarations, MaxLineLength 120 (excluded from tests/kts), ForbiddenComment disabled, DestructuringDeclarationWithTooManyEntries disabled
- **Compose rules:** Material2 detection, UnstableCollections, PreviewPublic disabled
- **KtLint:** `ktlint_official` style, trailing commas on declaration + call sites, function signature multiline at 2+ parameters

### 5. Version Catalog Updates

Add to `gradle/libs.versions.toml`:

**New versions:** hilt, dagger, ksp, detekt-compose-rules, detekt-ktlint

**New libraries:** Hilt Android, Hilt core, Hilt compiler (KSP), Dagger, KSP API, detekt-compose plugin, detekt-ktlint plugin, Kotlin test, MockK, Truth, Robolectric, Turbine, kotlinx-coroutines-test, Espresso, AndroidX Test JUnit, etc.

**New bundles:** `hilt.app`, `hilt.library`, `hilt.core`, `detekt` (plugins), `unit.testing`, `coroutine.testing`, `android.test`, `compose.uiTesting`, `screenshot.testing`

**New plugins:** `quotanoia.android.application`, `quotanoia.android.library`, `quotanoia.android.feature`, `quotanoia.jvm.library`, `hilt`, `ksp`

### 6. Module Build File Simplification

**`app/build.gradle.kts`** — Replace all plugin applies and shared configuration with single `quotanoia.android.application` plugin. Keep only:
- Namespace, applicationId, minSdk, versionCode, versionName
- Signing configs
- Build types (release: R8, ProGuard rules)
- Packaging excludes
- App-specific dependencies (OkHttp, WorkManager, Wearable services, Glance widgets)

**`wear/build.gradle.kts`** — Same simplification. Keep only:
- Namespace, applicationId, minSdk = 30, versionCode, versionName
- Signing configs, build types
- Wear-specific dependencies (Tiles, ProtoLayout, Wear Compose Material)

**`build.gradle.kts` (root)** — Remove detekt plugin application and source configuration. Keep minimal `apply false` declarations for plugins not managed by convention plugins.

### 7. Gradle Properties Updates

Update `gradle.properties` with improved settings from common:

- JVM args: G1GC, SoftRefLRUPolicyMSPerMB=1, ReservedCodeCacheSize=256m, HeapDumpOnOutOfMemoryError, Xmx3g (balanced for 2-module project, not 4g like 30-module common)
- `org.gradle.parallel=true`
- `org.gradle.caching=true`
- `org.gradle.configuration-cache=true`
- `org.gradle.configuration-cache.parallel=true`
- `roborazzi.record.filePathStrategy=relativePathFromRoborazziContextOutputDirectory`

### 8. Settings.gradle.kts Update

Add `includeBuild("build-logic")` to `pluginManagement` block.

### 9. CI Workflow Update

Update `.github/workflows/build.yml` detekt job:
- Change `./gradlew detekt` to `./gradlew detektDebug detektDebugUnitTest`

### 10. Root lint.xml

Add root `lint.xml` with useful severity overrides from common:
- `AndroidGradlePluginVersion`: warning
- `GradleDependency`: warning
- `LintBaseline`: ignored
- `LintBaselineFixed`: informational
- `NewerVersionAvailable`: warning
- `SyntheticAccessor`: ignored
- `TypographyQuotes`: informational
- `UnusedIds`: informational

## Files Affected

| File | Change |
|------|--------|
| `build-logic/settings.gradle.kts` | **New** |
| `build-logic/convention/build.gradle.kts` | **New** |
| `build-logic/convention/src/main/java/*.kt` | **New** — 4 plugin classes |
| `build-logic/convention/src/main/java/dev/qwertyfinger/quotanoia/*.kt` | **New** — 10 helper files |
| `gradle/libs.versions.toml` | Modify — add plugins, bundles, libraries |
| `settings.gradle.kts` | Modify — add `includeBuild("build-logic")` |
| `build.gradle.kts` | Modify — remove detekt config |
| `app/build.gradle.kts` | Modify — simplify with convention plugin |
| `wear/build.gradle.kts` | Modify — simplify with convention plugin |
| `config/detekt/detekt.yml` | Rename to `detekt-overrides.yml` + expand rules |
| `gradle.properties` | Modify — improve JVM/build settings |
| `.github/workflows/build.yml` | Modify — update detekt command |
| `lint.xml` | **New** — root lint severity overrides |

## Non-Goals

- No CycloneDX SBOM generation
- No GitLab-specific detekt reporting (detekt-gitlab plugin or JSON custom report)
- No Firebase Crashlytics/Performance plugin integration
- No Navigation SafeArgs
- No `test-screenshot` module with custom preview tester (use Roborazzi defaults)
- No changes to existing test files or app logic
- Existing ProGuard rules, signing configs, and app-specific dependencies are preserved as-is
