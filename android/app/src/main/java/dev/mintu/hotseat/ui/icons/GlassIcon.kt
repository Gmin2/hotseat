package dev.mintu.hotseat.ui.icons

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Immutable
data class Grad(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val stops: List<Pair<Float, Long>>)

@Immutable
data class Layer(val d: String, val grad: Grad, val evenOdd: Boolean = false)

@Immutable
data class GlassIcon(val back: Layer, val front: Layer, val rims: List<Layer>, val blur: Float)

// how the icon reacts when it is poked
enum class GlassMotion { Pop, Bob, Tilt, Draw, Spin, Nudge }

private val ExpoOut = CubicBezierEasing(0.19f, 1f, 0.22f, 1f)

@Composable
fun GlassIcon(
    icon: GlassIcon,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    motion: GlassMotion = GlassMotion.Pop,
    pulse: Int = 0,
) {
    val paths = remember(icon) {
        fun parse(layer: Layer) = PathParser().parsePathString(layer.d).toPath().apply {
            fillType = if (layer.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero
        }
        Triple(parse(icon.back), parse(icon.front), icon.rims.map(::parse))
    }
    val (back, front, rims) = paths

    val press = remember { Animatable(1f) }
    val backShift = remember { Animatable(0f) }
    val frontTurn = remember { Animatable(0f) }
    val iconTurn = remember { Animatable(0f) }
    val frontLift = remember { Animatable(0f) }
    val draw = remember { Animatable(1f) }

    LaunchedEffect(pulse) {
        if (pulse == 0) return@LaunchedEffect
        val bouncy = spring<Float>(dampingRatio = 0.42f, stiffness = 380f)
        val settle = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 300f)
        coroutineScope {
            launch {
                press.snapTo(0.82f)
                press.animateTo(1f, bouncy)
            }
            launch {
                backShift.snapTo(1f)
                backShift.animateTo(0f, settle)
            }
            when (motion) {
                GlassMotion.Pop -> Unit
                GlassMotion.Bob -> launch {
                    frontLift.snapTo(-3f)
                    frontLift.animateTo(0f, bouncy)
                }
                // the whole icon tilts, cutting holes stay lined up
                GlassMotion.Tilt -> launch {
                    iconTurn.snapTo(-14f)
                    iconTurn.animateTo(0f, bouncy)
                }
                GlassMotion.Spin -> launch {
                    frontTurn.snapTo(0f)
                    frontTurn.animateTo(90f, spring(dampingRatio = 0.55f, stiffness = 260f))
                    frontTurn.snapTo(0f)
                }
                GlassMotion.Draw -> {
                    launch {
                        draw.snapTo(0f)
                        draw.animateTo(1f, tween(650, easing = ExpoOut))
                    }
                    launch {
                        frontLift.snapTo(5f)
                        frontLift.animateTo(0f, bouncy)
                    }
                }
                GlassMotion.Nudge -> launch {
                    frontLift.snapTo(0f)
                    frontLift.animateTo(3f, tween(120))
                    frontLift.animateTo(0f, bouncy)
                }
            }
        }
    }

    Canvas(modifier.then(Modifier.size(size))) {
        val unit = this.size.minDimension / 24f
        val pivot = Offset(12f * unit, 12f * unit)
        scale(press.value, pivot) {
            rotate(iconTurn.value, pivot) {
            scale(unit, unit, Offset.Zero) {
                val lift = frontLift.value
                val dx = if (motion == GlassMotion.Nudge) lift else 0f
                val dy = if (motion == GlassMotion.Nudge) 0f else lift
                val bx = -backShift.value * 0.8f
                val by = backShift.value * 1.4f

                // back shape, cut out where the frosted front currently sits
                val hole = moved(front, frontTurn.value, dx - bx, dy - by)
                translate(bx, by) {
                    clipPath(hole, ClipOp.Difference) {
                        drawPath(back, brush(icon.back.grad, tint))
                    }
                }

                withFront(frontTurn.value, dx, dy) {
                    // the blurred copy of the back that shows through the glass
                    clipPath(front) {
                        rotate(-frontTurn.value, Offset(12f, 12f)) {
                            translate(bx - dx, by - dy) {
                                blurred(back, brush(icon.back.grad, tint), icon.blur)
                            }
                        }
                    }
                    drawPath(front, brush(icon.front.grad))
                    // the highlight edge sweeps in from the left on Draw
                    clipRect(right = 24f * draw.value) {
                        rims.forEachIndexed { i, rim -> drawPath(rim, brush(icon.rims[i].grad)) }
                    }
                }
            }
            }
        }
    }
}

private fun DrawScope.withFront(turn: Float, dx: Float, dy: Float, block: DrawScope.() -> Unit) {
    translate(dx, dy) {
        rotate(turn, Offset(12f, 12f)) { block() }
    }
}

private fun moved(path: Path, turn: Float, dx: Float, dy: Float): Path {
    if (turn == 0f && dx == 0f && dy == 0f) return path
    val m = Matrix().apply {
        translate(12f + dx, 12f + dy)
        rotateZ(turn)
        translate(-12f, -12f)
    }
    return Path().apply {
        addPath(path)
        transform(m)
    }
}

private fun DrawScope.blurred(path: Path, brush: Brush, radius: Float) {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        shader = (brush as ShaderBrush).createShader(size)
        maskFilter = BlurMaskFilter(radius * 1.7f, BlurMaskFilter.Blur.NORMAL)
    }
    drawIntoCanvas { it.nativeCanvas.drawPath(path.asAndroidPath(), paint) }
}

private fun brush(grad: Grad, tint: Color? = null): Brush {
    val stops = grad.stops.map { (offset, argb) ->
        val base = Color(argb)
        offset to if (tint == null) base else lerp(tint.lighten(0.25f), tint.darken(0.35f), offset).copy(alpha = base.alpha)
    }.toTypedArray()
    return Brush.linearGradient(*stops, start = Offset(grad.x1, grad.y1), end = Offset(grad.x2, grad.y2))
}

private fun Color.lighten(by: Float) = lerp(this, Color.White, by)

private fun Color.darken(by: Float) = lerp(this, Color.Black, by)
