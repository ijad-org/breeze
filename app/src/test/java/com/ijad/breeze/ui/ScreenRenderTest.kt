package com.ijad.breeze.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ijad.breeze.data.AcDevice
import com.ijad.breeze.data.AppRepository
import com.ijad.breeze.data.Brands
import com.ijad.breeze.ir.IrTransmitter
import com.ijad.breeze.ui.pairing.PairingScreen
import com.ijad.breeze.ui.remote.RemoteScreen
import com.ijad.breeze.ui.splash.SplashScreen
import com.ijad.breeze.ui.theme.BreezeTheme
import com.ijad.breeze.ui.theme.surfaceColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders screens to PNGs on the JVM (Robolectric native graphics), so UI can be compared with
 * docs/design/\*.png without an emulator. Same size as those PNGs: 394×853 dp at 1×.
 *
 * Opt-in: `./gradlew testDebugUnitTest -Pbreeze.screenshots=true --tests '*ScreenRenderTest*'`.
 * PNGs land in app/build/screenshots.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w394dp-h853dp-mdpi")
class ScreenRenderTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File(System.getProperty("breeze.screenshotDir") ?: "build/screenshots")

    @Before
    fun onlyWhenAsked() {
        assumeTrue(!System.getProperty("breeze.screenshots").isNullOrEmpty())
        outDir.mkdirs()
    }

    private fun render(dark: Boolean, reduceMotion: Boolean = false, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            BreezeTheme(dark = dark, reduceMotion = reduceMotion) {
                Box(Modifier.fillMaxSize().background(surfaceColor())) { content() }
            }
        }
    }

    private fun save(name: String): Bitmap {
        compose.waitForIdle()
        // Draw the window directly: captureToImage waits for a redraw, which never comes while the clock is paused.
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    @Composable
    private fun Pairing(brandId: String) {
        val context = LocalContext.current
        PairingScreen(
            brand = Brands.byId(brandId)!!,
            existingDevices = emptyList(),
            repository = AppRepository(context),
            irTransmitter = IrTransmitter(context),
            onBack = {},
            onPaired = {}
        )
    }

    /** A Daikin AC saved before code lists were versioned, so the Remote asks to find its code again. */
    @Composable
    private fun RemoteWithOldPairing() {
        val device = AcDevice("a", "Living Room", "daikin", "Daikin", 7, codeListVersion = 0)
        RemoteScreen(
            devices = listOf(device),
            activeDevice = device,
            timer = null,
            tempAlerts = true,
            ecoTips = false,
            irTransmitter = IrTransmitter(LocalContext.current),
            onSelectDevice = {},
            onStateChange = { _, _ -> },
            onOpenSettings = {},
            onOpenTimer = {},
            onCancelTimer = {},
            onAddAc = {},
            onFindCode = {},
            onCodeWorks = {}
        )
    }

    @Test
    fun remote_newCodeBanner_light() {
        render(dark = false) { RemoteWithOldPairing() }
        compose.mainClock.advanceTimeBy(1_000)
        save("remote-new-code-light")
    }

    @Test
    fun remote_newCodeBanner_dark() {
        render(dark = true) { RemoteWithOldPairing() }
        compose.mainClock.advanceTimeBy(1_000)
        save("remote-new-code-dark")
    }

    /** From API 36, large screens ignore the portrait lock: content stays phone-width, centred. */
    @Test
    @Config(qualifiers = "w1280dp-h800dp-mdpi")
    fun remote_tabletLandscape() {
        render(dark = true) { RemoteWithOldPairing() }
        compose.mainClock.advanceTimeBy(1_000)
        save("remote-tablet-landscape-dark")
    }

    @Test
    @Config(qualifiers = "w841dp-h701dp-mdpi")
    fun welcome_foldableUnfolded() {
        render(dark = false) { SplashScreen(onGetStarted = {}) }
        compose.mainClock.advanceTimeBy(1_500)
        save("welcome-foldable-light")
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-mdpi")
    fun findCode_tabletLandscape() {
        render(dark = false) { Pairing("daikin") }
        compose.mainClock.advanceTimeBy(1_000)
        save("find-code-tablet-landscape-light")
    }

    @Test
    fun findCode_light() {
        render(dark = false) { Pairing("bluestar") }
        compose.mainClock.advanceTimeBy(1_000)
        save("find-code-bluestar-light")
    }

    @Test
    fun findCode_dark() {
        render(dark = true) { Pairing("other") }
        compose.mainClock.advanceTimeBy(1_000)
        save("find-code-other-dark")
    }

    @Test
    fun findCode_carrier() {
        render(dark = false) { Pairing("carrier") }
        compose.mainClock.advanceTimeBy(1_000)
        save("find-code-carrier-light")
    }

    /** The pulse ring is driven by rememberFrameMillis: frames differ over time unless motion is reduced. */
    @Test
    fun findCode_pulseAnimates() {
        render(dark = false) { Pairing("samsung") }
        compose.mainClock.advanceTimeBy(300)
        val a = save("find-code-pulse-a")
        compose.mainClock.advanceTimeBy(900)
        val b = save("find-code-pulse-b")
        assertFalse("pulse should move", a.sameAs(b))
    }

    @Test
    fun findCode_reducedMotionIsFrozen() {
        render(dark = false, reduceMotion = true) { Pairing("samsung") }
        compose.mainClock.advanceTimeBy(300)
        val a = save("find-code-reduced-a")
        compose.mainClock.advanceTimeBy(900)
        val b = save("find-code-reduced-b")
        assertEquals(true, a.sameAs(b))
    }

    @Test
    fun welcome_light() {
        render(dark = false) { SplashScreen(onGetStarted = {}) }
        compose.mainClock.advanceTimeBy(1_500)
        save("welcome-light")
    }

    @Test
    fun welcome_ringsAnimate() {
        render(dark = true) { SplashScreen(onGetStarted = {}) }
        compose.mainClock.advanceTimeBy(500)
        val a = save("welcome-dark-a")
        compose.mainClock.advanceTimeBy(1_000)
        val b = save("welcome-dark-b")
        assertFalse("rings should move", a.sameAs(b))
    }

    @Test
    fun welcome_reducedMotionIsFrozen() {
        render(dark = false, reduceMotion = true) { SplashScreen(onGetStarted = {}) }
        compose.mainClock.advanceTimeBy(500)
        val a = save("welcome-reduced-a")
        compose.mainClock.advanceTimeBy(1_000)
        val b = save("welcome-reduced-b")
        assertEquals(true, a.sameAs(b))
    }
}
