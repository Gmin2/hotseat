package dev.mintu.hotseat.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme
import kotlin.math.abs

/**
 * Play button, the dock style tick row and the label pill riding the playhead.
 * [level] makes the ticks around the playhead breathe with the voice.
 */
@Composable
fun Scrubber(
    progress: Float,
    label: String,
    play: PlayState,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    level: Float = 0f,
    onSeek: ((Float) -> Unit)? = null,
) {
    val p = HotseatTheme.palette
    val density = LocalDensity.current
    val smooth by animateFloatAsState(progress, spring(dampingRatio = 1f, stiffness = 600f), label = "playhead")
    val voice by animateFloatAsState(level, spring(dampingRatio = 0.5f, stiffness = 260f), label = "tick voice")
    val position = smooth * (Dimens.tickCount - 1)
    val tickRow = Dimens.tickPitch * Dimens.tickCount

    Box(modifier.padding(start = Dimens.gutter)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayButton(play, onPlay)
            Spacer(Modifier.width(Dimens.playToTicks))
            val seek = onSeek
            Canvas(
                Modifier
                    .size(tickRow, Dimens.tickPeak + 8.dp)
                    .then(
                        if (seek == null) Modifier
                        else Modifier
                            .pointerInput(Unit) { detectTapGestures { seek((it.x / size.width).coerceIn(0f, 1f)) } }
                            .pointerInput(Unit) { detectDragGestures { change, _ -> seek((change.position.x / size.width).coerceIn(0f, 1f)) } },
                    ),
            ) {
                val pitch = Dimens.tickPitch.toPx()
                val rest = Dimens.tickRest.toPx()
                val peak = Dimens.tickPeak.toPx() * (1f + 0.25f * voice)
                val head = position.toInt()
                for (i in 0 until Dimens.tickCount) {
                    val d = abs(i - position)
                    val swell = (1f - d / (Dimens.tickSwell + 1f)).coerceAtLeast(0f)
                    val h = rest + (peak - rest) * swell
                    val color = when {
                        i == head -> p.playhead
                        d <= Dimens.tickSwell -> p.tickActive
                        else -> p.tick
                    }
                    val x = i * pitch
                    drawLine(color, Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), Dimens.tickWidth.toPx())
                }
            }
        }

        val pillWidth = remember { mutableIntStateOf(0) }
        val headX = Dimens.playSize + Dimens.playToTicks + Dimens.tickPitch * position.toInt()
        Pill(
            label,
            Modifier
                .offset(x = headX - with(density) { (pillWidth.intValue / 2).toDp() }, y = -Dimens.pillHeight)
                .onSizeChanged { pillWidth.intValue = it.width },
        )
    }
}
