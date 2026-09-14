package dev.mintu.hotseat.ui.hero

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import dev.mintu.hotseat.ui.theme.HotseatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// nothing phone (4a) is 408x907dp at 480dpi
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w408dp-h907dp-xxhdpi")
class HeroScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun hero() {
        // the chair drifts forever, so drive the clock by hand or the test never goes idle
        rule.mainClock.autoAdvance = false
        rule.setContent { HotseatTheme { HeroScreen(HeroState()) } }
        rule.mainClock.advanceTimeBy(500)
        rule.onRoot().captureRoboImage("build/hero/hero.png")
    }
}
