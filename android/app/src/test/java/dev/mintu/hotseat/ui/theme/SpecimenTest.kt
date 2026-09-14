package dev.mintu.hotseat.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi")
class SpecimenTest {

    @Test
    fun day() = captureRoboImage("build/specimen/day.png") {
        HotseatTheme { Specimen(Sky.Day, "Mostly Cloudy", "30°", dots("Bengaluru", "Mostly Cloudy"), dots("H 30°", "L 22°", "60% Humidity", "0% Rain"), "2 PM", 4) }
    }

    @Test
    fun night() = captureRoboImage("build/specimen/night.png") {
        HotseatTheme(dark = true) { Specimen(Sky.Night, "Partly Cloudy", "26°", dots("Bengaluru", "Partly Cloudy"), dots("H 30°", "L 22°", "60% Humidity", "0% Rain"), "8 PM", 16) }
    }
}

private fun dots(vararg parts: String) = parts.joinToString("\u2002·\u2002")

@Composable
private fun Specimen(sky: Sky, headline: String, number: String, meta: String, caption: String, time: String, playhead: Int) {
    val p = HotseatTheme.palette
    val t = HotseatTheme.type
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to sky.top,
                    0.29f to sky.upper,
                    0.53f to sky.mid,
                    0.62f to sky.low,
                    0.8f to sky.floor,
                ),
            ),
    ) {
        Box(
            Modifier
                .offset(Dimens.chipInset, 59.dp)
                .size(Dimens.chipWidth, Dimens.chipHeight)
                .clip(CircleShape)
                .background(p.chip)
                .border(1.dp, p.glassEdge, CircleShape),
        )
        BasicText(headline, Modifier.offset(Dimens.gutter, 136.dp), style = t.headline.copy(color = p.text))

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(start = Dimens.gutter, end = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(number, style = t.display.copy(color = p.display))
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(Dimens.arrowWidth, Dimens.arrowHeight).clip(CircleShape).background(p.glass).border(1.dp, p.glassEdge, CircleShape))
            }
            BasicText(meta, Modifier.padding(start = Dimens.gutter), style = t.meta.copy(color = p.meta))
            Box(Modifier.padding(start = Dimens.gutter, top = Dimens.metaGap)) {
                BasicText(caption, style = t.caption.copy(color = p.caption))
                Box(
                    Modifier
                        .offset(x = 76.dp - Dimens.gutter + Dimens.tickPitch * playhead - 30.dp, y = (-9).dp)
                        .height(Dimens.pillHeight)
                        .clip(CircleShape)
                        .background(p.pill)
                        .padding(horizontal = Dimens.pillPadding),
                    contentAlignment = Alignment.Center,
                ) { BasicText(time, style = t.pill.copy(color = p.pillText)) }
            }
            Row(Modifier.padding(start = Dimens.gutter, top = Dimens.captionToTicks), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(Dimens.playSize).clip(CircleShape).background(p.playButton))
                Spacer(Modifier.width(Dimens.playToTicks))
                Ticks(playhead)
            }
            Spacer(Modifier.height(36.dp))
            Row(
                Modifier
                    .padding(horizontal = Dimens.tabBarInset)
                    .fillMaxWidth()
                    .height(Dimens.tabBarHeight)
                    .clip(RoundedCornerShape(27.dp))
                    .background(p.tabBar)
                    .padding(Dimens.tabActiveInset),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("Practice", "Sessions", "Progress", "More").forEachIndexed { i, label ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(23.dp))
                            .background(if (i == 0) p.tabActive else p.tabBar),
                        contentAlignment = Alignment.BottomCenter,
                    ) { BasicText(label, Modifier.padding(bottom = 6.dp), style = t.tabLabel.copy(color = if (i == 0) p.tabTint else p.tabLabel)) }
                }
            }
            Spacer(Modifier.height(Dimens.tabBarBottom))
        }
    }
}

@Composable
private fun Ticks(playhead: Int) {
    val p = HotseatTheme.palette
    Canvas(Modifier.size(Dimens.tickPitch * Dimens.tickCount, Dimens.tickPeak)) {
        val pitch = Dimens.tickPitch.toPx()
        for (i in 0 until Dimens.tickCount) {
            val d = abs(i - playhead)
            val h = if (d > Dimens.tickSwell) Dimens.tickRest.toPx()
            else Dimens.tickRest.toPx() + (Dimens.tickPeak - Dimens.tickRest).toPx() * (1f - d / (Dimens.tickSwell + 1f))
            val x = i * pitch
            val color = when {
                d == 0 -> p.playhead
                d <= Dimens.tickSwell -> p.tickActive
                else -> p.tick
            }
            drawLine(color, Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), Dimens.tickWidth.toPx())
        }
    }
}
