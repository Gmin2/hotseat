package dev.mintu.hotseat.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

val BrandRed = Color(0xFFE03143)

private val ExpoOut = CubicBezierEasing(0.19f, 1f, 0.22f, 1f)

/**
 * The hotseat mark. [draw] is how much of the chair is drawn, [drop] how far the dot has landed (0 up high, 1 in place),
 * [live] makes the dot breathe like a recording light.
 */
@Composable
fun BrandMark(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    draw: Float = 1f,
    drop: Float = 1f,
    live: Boolean = false,
) {
    val chair = remember { PathParser().parsePathString(Brand.CHAIR).toPath() }
    val beat = rememberInfiniteTransition(label = "mark")
    val glow by beat.animateFloat(1f, 1.7f, infiniteRepeatable(tween(750), RepeatMode.Reverse), label = "glow")

    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        scale(unit, unit, Offset.Zero) {
            val drawn = if (draw >= 1f) chair else trim(chair, draw)
            drawPath(drawn, color, style = Stroke(Brand.STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (drop > 0f) {
                val y = Brand.DOT_Y - (1f - drop) * 10f
                val center = Offset(Brand.DOT_X, y)
                if (live) drawCircle(BrandRed.copy(alpha = 0.28f), Brand.DOT_R * glow, center)
                drawCircle(BrandRed, Brand.DOT_R * drop.coerceIn(0.6f, 1f), center)
            }
        }
    }
}

/** Plays the mark in: the chair draws itself, then the dot drops onto the seat with a bounce. */
@Composable
fun AnimatedBrandMark(size: Dp, color: Color, modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    val draw = remember { Animatable(0f) }
    val drop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        draw.animateTo(1f, tween(650, easing = ExpoOut))
        drop.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 260f))
        onDone()
    }
    BrandMark(size, color, modifier, draw = draw.value, drop = drop.value)
}

/** Starts from the finished mark, the one on the splash, and hops the dot up and back onto the seat. */
@Composable
fun HoppingBrandMark(size: Dp, color: Color, modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    val drop = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        drop.animateTo(0.45f, tween(260, easing = ExpoOut))
        drop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 320f))
        onDone()
    }
    BrandMark(size, color, modifier, drop = drop.value)
}

private fun trim(path: Path, amount: Float): Path {
    val measure = PathMeasure().apply { setPath(path, false) }
    return Path().also { measure.getSegment(0f, measure.length * amount, it, true) }
}
