package dev.mintu.hotseat.ui.art

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w510dp-h380dp-xxhdpi")
class IsoArtTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun hotseat() {
        rule.setContent { IsoArt(IsoScenes.Hotseat, Modifier.fillMaxSize().background(Color(0xFFA5CAFA))) }
        rule.onRoot().captureRoboImage("build/art/hotseat.png")
    }

    @Test
    fun tabs() {
        rule.setContent {
            Row(Modifier.fillMaxSize().background(Color(0xFFA5CAFA))) {
                listOf(IsoScenes.Sessions, IsoScenes.Progress, IsoScenes.You).forEach {
                    IsoArt(it, Modifier.weight(1f).fillMaxSize())
                }
            }
        }
        rule.onRoot().captureRoboImage("build/art/tabs.png")
    }
}
