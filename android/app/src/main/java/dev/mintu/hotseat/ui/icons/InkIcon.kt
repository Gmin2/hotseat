package dev.mintu.hotseat.ui.icons

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Immutable
data class Part(
    val d: String,
    val stroke: Float,
    val fill: Boolean,
    val accent: Boolean = false,
    val square: Boolean = false,
    val evenOdd: Boolean = false,
)

@Immutable
data class InkIcon(val viewport: Float, val parts: List<Part>)

// what the icon does when poked
enum class InkMotion { Pop, Bob, Draw, Spin, Wiggle, Nudge }

private val ExpoOut = CubicBezierEasing(0.19f, 1f, 0.22f, 1f)

@Composable
fun InkIcon(
    icon: InkIcon,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    motion: InkMotion = InkMotion.Pop,
    pulse: Int = 0,
) {
    val paths = remember(icon) {
        icon.parts.map { part ->
            PathParser().parsePathString(part.d).toPath().apply {
                fillType = if (part.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero
            }
        }
    }

    val press = remember { Animatable(1f) }
    val lift = remember { Animatable(0f) }
    val shift = remember { Animatable(0f) }
    val turn = remember { Animatable(0f) }
    val accentTurn = remember { Animatable(0f) }
    val draw = remember { Animatable(1f) }

    LaunchedEffect(pulse) {
        if (pulse == 0) return@LaunchedEffect
        val bouncy = spring<Float>(dampingRatio = 0.4f, stiffness = 420f)
        coroutineScope {
            launch {
                press.snapTo(0.8f)
                press.animateTo(1f, bouncy)
            }
            when (motion) {
                InkMotion.Pop -> Unit
                InkMotion.Bob -> launch {
                    lift.snapTo(-3f)
                    lift.animateTo(0f, bouncy)
                }
                InkMotion.Draw -> launch {
                    draw.snapTo(0f)
                    draw.animateTo(1f, tween(700, easing = ExpoOut))
                }
                InkMotion.Spin -> launch {
                    accentTurn.snapTo(-360f)
                    accentTurn.animateTo(0f, tween(750, easing = ExpoOut))
                }
                InkMotion.Wiggle -> launch {
                    turn.animateTo(0f, keyframes {
                        durationMillis = 420
                        -16f at 80
                        10f at 170
                        -4f at 260
                        0f at 420
                    })
                }
                InkMotion.Nudge -> launch {
                    shift.animateTo(3f, tween(110))
                    shift.animateTo(0f, bouncy)
                }
            }
        }
    }

    Canvas(modifier.then(Modifier.size(size))) {
        val unit = this.size.minDimension / icon.viewport
        val mid = icon.viewport / 2f
        scale(press.value) {
            rotate(turn.value) {
                scale(unit, unit, Offset.Zero) {
                    translate(shift.value, lift.value) {
                        icon.parts.forEachIndexed { i, part ->
                            val path = paths[i]
                            val spin = if (part.accent) accentTurn.value else 0f
                            rotate(spin, Offset(mid, mid)) {
                                if (part.fill) drawPath(path, tint)
                                if (part.stroke > 0f) {
                                    val trimmed = if (draw.value < 1f && (part.accent || icon.parts.none { it.accent })) trim(path, draw.value) else path
                                    drawPath(
                                        trimmed,
                                        tint,
                                        style = Stroke(
                                            width = part.stroke,
                                            cap = if (part.square) StrokeCap.Square else StrokeCap.Round,
                                            join = StrokeJoin.Round,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun trim(path: Path, amount: Float): Path {
    val measure = PathMeasure().apply { setPath(path, false) }
    return Path().also { measure.getSegment(0f, measure.length * amount, it, true) }
}
