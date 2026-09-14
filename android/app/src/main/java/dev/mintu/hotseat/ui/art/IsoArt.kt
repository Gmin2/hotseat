package dev.mintu.hotseat.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser

@Immutable
data class IsoShape(val d: String, val fill: Long?, val stroke: Float, val dashed: Boolean = false)

@Immutable
data class IsoPart(val name: String, val ax: Float, val ay: Float, val shapes: List<IsoShape>)

@Immutable
data class IsoScene(val x: Float, val y: Float, val width: Float, val height: Float, val parts: List<IsoPart>)

/** How one part is moved this frame. Vertical scale around the anchor is exact in isometric, it only stretches z. */
data class PartMotion(val dy: Float = 0f, val scaleY: Float = 1f, val scaleX: Float = 1f)

private val Ink = Color(0xFF1C1C1C)
private val Red = Color(0xFFE03143)

@Composable
fun IsoArt(scene: IsoScene, modifier: Modifier = Modifier, motion: (String) -> PartMotion = { PartMotion() }) {
    val paths = remember(scene) { scene.parts.map { p -> p.shapes.map { PathParser().parsePathString(it.d).toPath() } } }
    Canvas(modifier) {
        val fit = minOf(size.width / scene.width, size.height / scene.height)
        val left = (size.width - scene.width * fit) / 2
        val top = (size.height - scene.height * fit) / 2
        translate(left, top) {
            scale(fit, fit, Offset.Zero) {
                translate(-scene.x, -scene.y) {
                    scene.parts.forEachIndexed { i, part ->
                        val m = motion(part.name)
                        translate(0f, m.dy) {
                            scale(m.scaleX, m.scaleY, Offset(part.ax, part.ay)) {
                                part.shapes.forEachIndexed { j, shape -> drawShape(shape, paths[i][j]) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawShape(shape: IsoShape, path: Path) {
    shape.fill?.let { drawPath(path, Color(it)) }
    if (shape.stroke > 0f) {
        drawPath(
            path,
            if (shape.dashed) Red else Ink,
            style = Stroke(
                width = shape.stroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = if (shape.dashed) PathEffect.dashPathEffect(floatArrayOf(4f, 4f)) else null,
            ),
        )
    }
}
