package dev.mintu.hotseat.ui.hero

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.R
import dev.mintu.hotseat.ui.icons.GlassIcon
import dev.mintu.hotseat.ui.icons.GlassIcons
import dev.mintu.hotseat.ui.icons.GlassMotion
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme
import dev.mintu.hotseat.ui.theme.Sky
import kotlin.math.abs
import kotlin.math.roundToInt

data class HeroState(
    val sky: Sky = Sky.Day,
    val headline: String = "Tell me about a project you are proud of",
    val number: String = "04:12",
    val meta: List<String> = listOf("Android engineer", "Behavioral"),
    val caption: List<String> = listOf("Q3 of 8", "142 wpm", "4 fillers"),
    val pill: String = "Q3",
    val progress: Float = 0.3f,
    val playing: Boolean = false,
    val ended: Boolean = false,
)

val tabs = listOf(
    Tab("Practice", GlassIcons.Practice, GlassMotion.Bob),
    Tab("Sessions", GlassIcons.Sessions, GlassMotion.Tilt),
    Tab("Progress", GlassIcons.Progress, GlassMotion.Draw),
    Tab("More", GlassIcons.More, GlassMotion.Spin),
)

data class Tab(val label: String, val icon: GlassIcon, val motion: GlassMotion)

private fun dots(parts: List<String>) = parts.joinToString(" · ")

@Composable
fun HeroScreen(
    state: HeroState,
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    onTab: (Int) -> Unit = {},
    onPlay: () -> Unit = {},
    onArrow: () -> Unit = {},
) {
    val p = HotseatTheme.palette
    val t = HotseatTheme.type
    val sky = state.sky

    Box(
        modifier
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
        Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
            StatusChip(Modifier.padding(start = Dimens.chipInset))
            BasicText(
                state.headline,
                Modifier.padding(start = Dimens.gutter, end = Dimens.gutter, top = 30.dp),
                style = t.headline.copy(color = p.text),
            )
        }

        FloatingChair(Modifier.align(Alignment.Center).offset(y = (-36).dp))

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = Dimens.gutter, end = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    state.number,
                    Modifier.offset(y = -Dimens.displayNudge),
                    style = t.display.copy(color = p.display),
                )
                Spacer(Modifier.weight(1f))
                GlassButton(onArrow)
            }
            BasicText(dots(state.meta), Modifier.padding(start = Dimens.gutter), style = t.meta.copy(color = p.meta))
            BasicText(
                dots(state.caption),
                Modifier.padding(start = Dimens.gutter, top = Dimens.metaGap),
                style = t.caption.copy(color = p.caption),
            )
            Scrubber(state, onPlay, Modifier.padding(top = Dimens.captionToTicks))
            Spacer(Modifier.height(36.dp))
            TabBar(selectedTab, onTab)
            Spacer(Modifier.height(Dimens.tabBarBottom))
        }
    }
}

@Composable
private fun FloatingChair(modifier: Modifier = Modifier) {
    val drift = rememberInfiniteTransition(label = "chair drift")
    val bob by drift.animateFloat(-6f, 6f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val sway by drift.animateFloat(-2.5f, 2.5f, infiniteRepeatable(tween(4700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway")

    var pulse by remember { mutableIntStateOf(0) }
    val squash = remember { Animatable(1f) }
    LaunchedEffect(pulse) {
        if (pulse == 0) return@LaunchedEffect
        squash.snapTo(0.9f)
        squash.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))
    }

    Image(
        painterResource(R.drawable.illo_chair),
        contentDescription = null,
        modifier = modifier
            .size(Dimens.heroIllustration)
            .graphicsLayer {
                translationY = bob.dp.toPx()
                rotationZ = sway
                scaleX = squash.value
                scaleY = squash.value
            }
            .clickable(remember { MutableInteractionSource() }, indication = null) { pulse++ },
    )
}

@Composable
private fun StatusChip(modifier: Modifier = Modifier) {
    val p = HotseatTheme.palette
    Row(
        modifier
            .size(Dimens.chipWidth, Dimens.chipHeight)
            .clip(CircleShape)
            .background(p.chip)
            .border(1.dp, p.glassEdge, CircleShape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var pulse by remember { mutableIntStateOf(0) }
        GlassIcon(
            GlassIcons.Sparkle,
            20.dp,
            Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { pulse++ },
            tint = p.tabTint,
            motion = GlassMotion.Spin,
            pulse = pulse,
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(8.dp).clip(CircleShape).background(p.playhead))
    }
}

@Composable
private fun GlassButton(onClick: () -> Unit) {
    val p = HotseatTheme.palette
    var pulse by remember { mutableIntStateOf(0) }
    Box(
        Modifier
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
        GlassIcon(GlassIcons.Next, 22.dp, tint = p.tabTint, motion = GlassMotion.Nudge, pulse = pulse)
    }
}

@Composable
private fun Scrubber(state: HeroState, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val p = HotseatTheme.palette
    val t = HotseatTheme.type
    val playhead = (state.progress * (Dimens.tickCount - 1)).roundToInt()
    val density = LocalDensity.current

    Box(modifier.padding(start = Dimens.gutter)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayButton(state.playing, state.ended, onPlay)
            Spacer(Modifier.width(Dimens.playToTicks))
            Ticks(playhead)
        }

        val pillWidth = remember { mutableIntStateOf(0) }
        val tickX = Dimens.playSize + Dimens.playToTicks + Dimens.tickPitch * playhead
        val pillX = tickX - with(density) { (pillWidth.intValue / 2).toDp() }
        Box(
            Modifier
                .offset(x = pillX, y = -Dimens.pillHeight + 4.dp)
                .onSizeChanged { pillWidth.intValue = it.width }
                .height(Dimens.pillHeight)
                .clip(CircleShape)
                .background(p.pill)
                .padding(horizontal = Dimens.pillPadding),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(state.pill, style = t.pill.copy(color = p.pillText))
        }
    }
}

@Composable
private fun PlayButton(playing: Boolean, ended: Boolean, onClick: () -> Unit) {
    val p = HotseatTheme.palette
    Box(
        Modifier
            .size(Dimens.playSize)
            .clip(CircleShape)
            .background(p.playButton)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            ended -> Icon(R.drawable.ic_replay, p.playGlyph, 16.dp)
            playing -> Canvas(Modifier.size(12.dp)) {
                val w = size.width * 0.3f
                drawRect(p.playGlyph, Offset(size.width * 0.08f, 0f), Size(w, size.height))
                drawRect(p.playGlyph, Offset(size.width * 0.62f, 0f), Size(w, size.height))
            }
            else -> Canvas(Modifier.size(12.dp)) {
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

@Composable
private fun Ticks(playhead: Int) {
    val p = HotseatTheme.palette
    Canvas(Modifier.size(Dimens.tickPitch * Dimens.tickCount, Dimens.tickPeak)) {
        val pitch = Dimens.tickPitch.toPx()
        val rest = Dimens.tickRest.toPx()
        val peak = Dimens.tickPeak.toPx()
        for (i in 0 until Dimens.tickCount) {
            val d = abs(i - playhead)
            val h = if (d > Dimens.tickSwell) rest else rest + (peak - rest) * (1f - d / (Dimens.tickSwell + 1f))
            val color = when {
                d == 0 -> p.playhead
                d <= Dimens.tickSwell -> p.tickActive
                else -> p.tick
            }
            val x = i * pitch
            drawLine(color, Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), Dimens.tickWidth.toPx())
        }
    }
}

@Composable
private fun TabBar(selected: Int, onTab: (Int) -> Unit) {
    val p = HotseatTheme.palette
    val t = HotseatTheme.type
    Row(
        Modifier
            .padding(horizontal = Dimens.tabBarInset)
            .fillMaxWidth()
            .height(Dimens.tabBarHeight)
            .shadow(24.dp, RoundedCornerShape(Dimens.tabBarHeight / 2), ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(Dimens.tabBarHeight / 2))
            .background(p.tabBar)
            .padding(Dimens.tabActiveInset),
    ) {
        val pulses = remember { mutableStateListOf(*Array(tabs.size) { 0 }) }
        tabs.forEachIndexed { i, tab ->
            val active = i == selected
            val tint by animateColorAsState(if (active) p.tabTint else p.tabLabel, tween(220), label = "tab tint")
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape((Dimens.tabBarHeight - Dimens.tabActiveInset * 2) / 2))
                    .background(if (active) p.tabActive else Color.Transparent)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {
                        pulses[i]++
                        onTab(i)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                GlassIcon(tab.icon, 24.dp, tint = tint, motion = tab.motion, pulse = pulses[i])
                BasicText(tab.label, Modifier.padding(top = 3.dp), style = t.tabLabel.copy(color = tint))
            }
        }
    }
}

@Composable
private fun Icon(@DrawableRes id: Int, tint: Color, size: Dp) {
    Image(painterResource(id), null, Modifier.size(size), colorFilter = ColorFilter.tint(tint))
}
