package dev.mintu.hotseat.ui.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w600dp-h400dp-xhdpi")
class GlassIconTest {
    @get:Rule
    val rule = createComposeRule()

    private val all = listOf(
        GlassIcons.Practice to GlassMotion.Bob,
        GlassIcons.Sessions to GlassMotion.Tilt,
        GlassIcons.Progress to GlassMotion.Draw,
        GlassIcons.More to GlassMotion.Spin,
        GlassIcons.Sparkle to GlassMotion.Spin,
        GlassIcons.Next to GlassMotion.Nudge,
    )

    @Test
    fun sheet() {
        rule.setContent {
            Column(Modifier.background(Color.White).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for (tint in listOf(null, Color(0xFF1C1C1C), Color(0xFF0A64E4))) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        all.forEach { (icon, _) -> GlassIcon(icon, 64.dp, tint = tint) }
                    }
                }
            }
        }
        rule.onRoot().captureRoboImage("build/icons/sheet.png")
    }

    @Test
    fun motion() {
        rule.mainClock.autoAdvance = false
        var pulse by mutableIntStateOf(0)
        rule.setContent {
            Row(Modifier.background(Color.White).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                all.forEach { (icon, motion) -> GlassIcon(icon, 64.dp, tint = Color(0xFF0A64E4), motion = motion, pulse = pulse) }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        pulse = 1
        var elapsed = 0L
        for (ms in listOf(16L, 48L, 96L, 160L, 320L, 700L)) {
            rule.mainClock.advanceTimeBy(ms - elapsed)
            elapsed = ms
            rule.onRoot().captureRoboImage("build/icons/motion-$ms.png")
        }
    }
}
