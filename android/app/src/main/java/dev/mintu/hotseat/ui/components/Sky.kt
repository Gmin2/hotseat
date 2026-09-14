package dev.mintu.hotseat.ui.components

import dev.mintu.hotseat.ui.theme.loop
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import dev.mintu.hotseat.ui.theme.Motion
import dev.mintu.hotseat.ui.theme.Sky
import kotlin.math.PI
import kotlin.math.sin

enum class Mood(val sky: Sky, val ms: Int) {
    Day(Sky.Day, Motion.SKY_TO_DAY_MS),
    Dusk(Sky.Dusk, Motion.SKY_TO_NIGHT_MS),
    Night(Sky.Night, Motion.SKY_TO_NIGHT_MS),
    Overcast(Sky.Overcast, Motion.SKY_TO_OVERCAST_MS),
    Morning(Sky.Morning, Motion.SKY_TO_DAY_MS),
    Mist(Sky.Mist, Motion.SKY_TO_DAY_MS),
}

// soft cloud puffs as fractions of the screen: x, y, radius, drift speed
private val puffs = listOf(
    floatArrayOf(0.12f, 0.10f, 0.20f, 0.6f),
    floatArrayOf(0.30f, 0.14f, 0.16f, 0.9f),
    floatArrayOf(0.78f, 0.08f, 0.22f, 0.5f),
    floatArrayOf(0.58f, 0.22f, 0.18f, 0.7f),
    floatArrayOf(0.05f, 0.36f, 0.24f, 0.4f),
    floatArrayOf(0.40f, 0.44f, 0.20f, 0.8f),
    floatArrayOf(0.88f, 0.40f, 0.22f, 0.55f),
    floatArrayOf(0.22f, 0.56f, 0.26f, 0.35f),
    floatArrayOf(0.70f, 0.58f, 0.24f, 0.65f),
)

private val stars = List(26) { i ->
    val x = ((i * 73) % 100) / 100f
    val y = ((i * 37) % 55) / 100f
    Offset(x, y)
}

/**
 * The animated backdrop. Clouds drift, the orb is the interviewer and swells with [level].
 */
@Composable
fun SkyBackdrop(mood: Mood, level: Float, modifier: Modifier = Modifier, orb: Offset = Offset(0.64f, 0.47f)) {
    val tween = tween<Color>(mood.ms, easing = FastOutSlowInEasing)
    val top by animateColorAsState(mood.sky.top, tween, label = "top")
    val upper by animateColorAsState(mood.sky.upper, tween, label = "upper")
    val mid by animateColorAsState(mood.sky.mid, tween, label = "mid")
    val low by animateColorAsState(mood.sky.low, tween, label = "low")
    val floor by animateColorAsState(mood.sky.floor, tween, label = "floor")
    val night by animateFloatAsState(if (mood == Mood.Night) 1f else 0f, tween(mood.ms), label = "night")
    val voice by animateFloatAsState(level, spring(dampingRatio = 0.6f, stiffness = 180f), label = "voice")
    // the chat skies drop the clouds and the orb for two soft glows, blue for the interviewer and violet for you
    val calm by animateFloatAsState(if (mood == Mood.Morning || mood == Mood.Mist) 1f else 0f, tween(600), label = "calm")
    val you by animateFloatAsState(if (mood == Mood.Mist) 1f else 0f, tween(700), label = "you glow")

    val drift = loop(60_000, "drift")
    val breath = loop(4200, "breath", reverse = true, easing = FastOutSlowInEasing)
    val shimmer = loop(900, "shimmer")

    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(0f to top, 0.29f to upper, 0.53f to mid, 0.62f to low, 0.8f to floor))

        if (night > 0f) {
            stars.forEachIndexed { i, s ->
                val twinkle = 0.4f + 0.6f * ((sin((drift * 40 + i) * PI).toFloat() + 1f) / 2f)
                drawCircle(Color.White.copy(alpha = 0.5f * night * twinkle), 1.2f * density, Offset(s.x * size.width, s.y * size.height))
            }
        }

        val base = lerp(Color.White.copy(alpha = 0.34f), Color(0xFF30364A).copy(alpha = 0.45f), night)
        val cloud = base.copy(alpha = base.alpha * (1f - calm))
        puffs.forEach { p ->
            val travel = ((p[0] + drift * p[3]) % 1.3f) - 0.15f
            val center = Offset(travel * size.width, p[1] * size.height)
            puff(center, p[2] * size.width, cloud)
        }

        val wobble = if (voice > 0.01f) sin(shimmer * 2 * PI).toFloat() * 0.03f * voice else 0f
        val radius = size.width * (0.2f + 0.02f * breath + 0.12f * voice + wobble)
        if (calm > 0f) {
            val blue = Offset(size.width * 0.92f, size.height * 0.08f)
            val blueR = size.width * (0.75f + 0.12f * voice + 0.04f * breath)
            drawCircle(Brush.radialGradient(0f to Color(0xFF8FB3F5).copy(alpha = 0.42f * calm * (1f - 0.5f * you)), 1f to Color.Transparent, center = blue, radius = blueR), blueR, blue)
            val violet = Offset(size.width * 0.05f, size.height * 0.62f)
            val violetR = size.width * (0.7f + 0.05f * breath)
            drawCircle(Brush.radialGradient(0f to Color(0xFFB9A8F3).copy(alpha = 0.34f * calm * (0.35f + 0.65f * you)), 1f to Color.Transparent, center = violet, radius = violetR), violetR, violet)
        }

        val c = Offset(orb.x * size.width, orb.y * size.height)
        val glow = lerp(Color.White, Color(0xFFF3EAC8), night)
        val fade = 1f - calm
        drawCircle(Brush.radialGradient(0f to glow.copy(alpha = 0.55f * fade), 0.45f to glow.copy(alpha = 0.18f * fade), 1f to Color.Transparent, center = c, radius = radius * 2.2f), radius * 2.2f, c)
        drawCircle(Brush.radialGradient(0f to glow.copy(alpha = 0.95f * fade), 0.6f to glow.copy(alpha = 0.7f * fade), 1f to glow.copy(alpha = 0f * fade), center = c, radius = radius * 0.55f), radius * 0.55f, c)
    }
}

private fun DrawScope.puff(center: Offset, radius: Float, color: Color) {
    drawCircle(Brush.radialGradient(0f to color, 0.6f to color.copy(alpha = color.alpha * 0.45f), 1f to Color.Transparent, center = center, radius = radius), radius, center)
    val side = Offset(center.x + radius * 0.6f, center.y + radius * 0.12f)
    drawCircle(Brush.radialGradient(0f to color, 1f to Color.Transparent, center = side, radius = radius * 0.7f), radius * 0.7f, side)
}

