package dev.mintu.hotseat.ui.art

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlin.math.PI
import kotlin.math.sin

/**
 * The hot seat: a chair on its red ring, voice bars on the table trailing up into a speech bubble.
 * [level] is the interviewer voice, the bars jump with it and rest in a slow breath otherwise.
 */
@Composable
fun HotseatScene(level: Float, modifier: Modifier = Modifier) {
    val scene = IsoScenes.Hotseat
    val clock = rememberInfiniteTransition(label = "scene")
    val t by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "t")
    val float by clock.animateFloat(-1f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val voice by animateFloatAsState(level, spring(dampingRatio = 0.5f, stiffness = 220f), label = "scene voice")

    var taps by remember { mutableIntStateOf(0) }
    val squash = remember { Animatable(1f) }
    LaunchedEffect(taps) {
        if (taps == 0) return@LaunchedEffect
        squash.snapTo(0.88f)
        squash.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = 320f))
    }

    IsoArt(
        scene,
        modifier
            .aspectRatio(scene.width / scene.height)
            .clickable(remember { MutableInteractionSource() }, indication = null) { taps++ },
    ) { name ->
        when {
            name.startsWith("bar") -> {
                val i = name.last().digitToInt()
                val wave = sin((t * 2 * PI * (3 + i)).toFloat() + i * 1.3f)
                val rest = 0.9f + 0.1f * sin((t * 2 * PI).toFloat() + i)
                PartMotion(scaleY = rest + voice * (0.55f + 0.45f * wave))
            }
            name == "bubble" -> PartMotion(dy = float * 5f - voice * 6f, scaleX = 1f + voice * 0.04f, scaleY = 1f + voice * 0.04f)
            name == "trail" -> PartMotion(dy = float * 3f)
            name == "chair" -> PartMotion(scaleY = squash.value)
            else -> PartMotion()
        }
    }
}

/** The smaller scenes that sit between a tab title and its big number. Parts grow in once, then idle. */
@Composable
fun TabScene(scene: IsoScene, modifier: Modifier = Modifier) {
    val clock = rememberInfiniteTransition(label = "tab scene")
    val t by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "t")
    val float by clock.animateFloat(-1f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val grow = remember(scene) { Animatable(0f) }
    LaunchedEffect(scene) { grow.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 120f)) }

    IsoArt(scene, modifier.aspectRatio(scene.width / scene.height)) { name ->
        val index = name.lastOrNull()?.digitToIntOrNull() ?: 0
        // stagger the entrance by part index so steps and cards land one after another
        val g = ((grow.value * 1.6f) - index * 0.12f).coerceIn(0f, 1.15f)
        when {
            name.startsWith("step") -> PartMotion(scaleY = g)
            name.startsWith("wave") -> PartMotion(scaleY = g * (0.75f + 0.35f * sin((t * 2 * PI * 2).toFloat() + index)))
            name.startsWith("card") -> PartMotion(dy = (1f - g.coerceAtMost(1f)) * -30f + if (index == 2) float * 2f else 0f)
            name == "flag" -> PartMotion(dy = (1f - g.coerceAtMost(1f)) * -20f, scaleX = 1f + 0.04f * sin((t * 2 * PI).toFloat()))
            name == "badge" -> PartMotion(dy = float * 3f)
            name == "check" -> PartMotion(dy = float * 3f, scaleX = g, scaleY = g)
            else -> PartMotion()
        }
    }
}
