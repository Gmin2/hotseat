package dev.mintu.hotseat.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import dev.mintu.hotseat.ui.theme.loop
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.ui.brand.IdleMascot
import dev.mintu.hotseat.ui.icons.InkIcon
import dev.mintu.hotseat.ui.icons.InkIcons
import dev.mintu.hotseat.ui.icons.InkMotion
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme

enum class ChipState { Ready, Connecting, Live, Done }

@Composable
fun StatusChip(state: ChipState, modifier: Modifier = Modifier) {
    val p = HotseatTheme.palette
    var pulse by remember { mutableIntStateOf(0) }
    val glow = 1f + 0.6f * loop(700, "chip glow", reverse = true, easing = FastOutSlowInEasing)
    val dot by animateColorAsState(
        when (state) {
            ChipState.Ready -> Color(0xFF34C759)
            ChipState.Connecting -> Color(0xFFE08A1E)
            ChipState.Live -> p.playhead
            ChipState.Done -> p.tabTint
        },
        tween(300),
        label = "dot",
    )
    Row(
        modifier
            .size(Dimens.chipWidth, Dimens.chipHeight)
            .clip(CircleShape)
            .background(p.chip)
            .border(1.dp, p.glassEdge, CircleShape)
            .clickable(remember { MutableInteractionSource() }, indication = null) { pulse++ },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // tapping makes the bird hop, while live its mic glows like a recording light
        IdleMascot(34.dp, mic = if (state == ChipState.Live) (glow - 1f) / 0.6f else 0f, pulse = pulse)
        Spacer(Modifier.width(8.dp))
        Box(contentAlignment = Alignment.Center) {
            if (state == ChipState.Live || state == ChipState.Connecting) {
                Box(Modifier.size(8.dp).scale(glow).clip(CircleShape).background(dot.copy(alpha = 0.3f)))
            }
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        }
    }
}

@Composable
fun RoundButton(onClick: () -> Unit, modifier: Modifier = Modifier, icon: InkIcon = InkIcons.Next) {
    val p = HotseatTheme.palette
    var pulse by remember { mutableIntStateOf(0) }
    Box(
        modifier
            .size(Dimens.arrowWidth, Dimens.arrowHeight)
            .shadow(12.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(CircleShape)
            .background(p.glass)
            .border(1.dp, p.glassEdge, CircleShape)
            .clickable {
                pulse++
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        InkIcon(icon, 20.dp, p.glassGlyph, motion = InkMotion.Nudge, pulse = pulse)
    }
}

enum class PlayState { Play, Pause, Replay, Mic, MicOff }

@Composable
fun PlayButton(state: PlayState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = HotseatTheme.palette
    var pulse by remember { mutableIntStateOf(0) }
    Box(
        modifier
            .size(Dimens.playSize)
            .clip(CircleShape)
            .background(p.playButton)
            .clickable {
                pulse++
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            PlayState.Replay -> InkIcon(InkIcons.Clock, 16.dp, p.playGlyph, motion = InkMotion.Spin, pulse = pulse)
            PlayState.Mic -> InkIcon(InkIcons.Mic, 16.dp, p.playGlyph, motion = InkMotion.Pop, pulse = pulse)
            PlayState.MicOff -> InkIcon(InkIcons.MicOff, 16.dp, p.playGlyph, motion = InkMotion.Wiggle, pulse = pulse)
            PlayState.Pause -> Canvas(Modifier.size(11.dp)) {
                val w = size.width * 0.3f
                drawRect(p.playGlyph, Offset(size.width * 0.08f, 0f), Size(w, size.height))
                drawRect(p.playGlyph, Offset(size.width * 0.62f, 0f), Size(w, size.height))
            }
            PlayState.Play -> Canvas(Modifier.size(11.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.18f, 0f)
                    lineTo(size.width, size.height / 2)
                    lineTo(size.width * 0.18f, size.height)
                    close()
                }
                drawPath(path, p.playGlyph)
            }
        }
    }
}

/** The red stop pill that ends a running interview. [label] says what tapping it does right now. */
@Composable
fun EndButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "end press")
    val fill by animateColorAsState(if (enabled) Color(0xFFE03143) else Color(0xFFB9C0CC), tween(200), label = "end fill")
    Row(
        modifier
            .height(Dimens.arrowHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (enabled) 12.dp else 0.dp, CircleShape, ambientColor = Color(0x55E03143), spotColor = Color(0x55E03143))
            .clip(CircleShape)
            .background(fill)
            .clickable(press, indication = null, enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
        Spacer(Modifier.width(8.dp))
        BasicText(label, style = HotseatTheme.type.pill.copy(color = Color.White))
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, fill: Color = HotseatTheme.palette.pill, ink: Color = HotseatTheme.palette.pillText) {
    Box(
        modifier
            .height(Dimens.pillHeight)
            .clip(CircleShape)
            .background(fill)
            .padding(horizontal = Dimens.pillPadding),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = HotseatTheme.type.pill.copy(color = ink))
    }
}

/** A pill segmented control, the selection slides between options. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = HotseatTheme.palette
    val t = HotseatTheme.type
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(CircleShape)
            .background(p.tabActive)
            .padding(3.dp),
    ) {
        val cell = maxWidth / options.size
        val x by animateDpAsState(cell * selected, spring(dampingRatio = 0.75f, stiffness = 420f), label = "segment")
        Box(
            Modifier
                .offset(x = x)
                .width(cell)
                .fillMaxHeight()
                .shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.1f))
                .clip(CircleShape)
                .background(p.tabBar),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(label, style = t.meta.copy(color = if (i == selected) p.text else p.caption))
                }
            }
        }
    }
}
