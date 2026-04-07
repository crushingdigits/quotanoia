package dev.qwertyfinger.quotanoia

import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

/**
 * Automatically captures screenshots of all @Preview composables using Roborazzi
 * and ComposablePreviewScanner. Requires Robolectric native graphics (x86_64 only).
 *
 * Run `./gradlew :app:recordRoborazziDebug` to generate baseline images.
 * Run `./gradlew :app:verifyRoborazziDebug` to compare against baselines.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [34], qualifiers = "w400dp-h800dp-480dpi")
class ScreenshotTests {

    companion object {
        @JvmStatic
        @BeforeClass
        fun checkPlatform() {
            val arch = System.getProperty("os.arch").orEmpty()
            Assume.assumeTrue(
                "Robolectric native not supported on $arch",
                arch in listOf("amd64", "x86_64"),
            )
        }
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun captureAllPreviews() {
        val previews: List<ComposablePreview<AndroidPreviewInfo>> =
            AndroidComposablePreviewScanner()
                .scanPackageTrees("dev.qwertyfinger.quotanoia")
                .getPreviews()

        previews.forEach { preview ->
            val name = preview.methodName
                ?: preview.declaringClass?.replace('.', '_')
                ?: "unknown"

            preview.captureRoboImage("src/test/screenshots/$name.png")
        }
    }
}
