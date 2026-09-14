package dev.mintu.hotseat.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser

@Immutable
data class SvgShape(val d: String, val fill: Long?, val stroke: Long?, val width: Float, val round: Boolean)

@Immutable
data class SvgArt(val x: Float, val y: Float, val width: Float, val height: Float, val shapes: List<SvgShape>)

/**
 * Draws a flattened svg. [reveal] builds it up back to front, each shape rising a little as it lands,
 * [lift] floats the whole drawing in view box units.
 */
@Composable
fun SvgArtwork(art: SvgArt, modifier: Modifier = Modifier, reveal: Float = 1f, lift: Float = 0f) {
    val paths = remember(art) { art.shapes.map { PathParser().parsePathString(it.d).toPath() } }
    Canvas(modifier) {
        val fit = minOf(size.width / art.width, size.height / art.height)
        val left = (size.width - art.width * fit) / 2
        val top = (size.height - art.height * fit) / 2
        val count = art.shapes.size
        // a window of shapes fades in at once, so the build reads as a wave rather than a flicker
        val front = reveal * (count + WINDOW)
        translate(left, top) {
            scale(fit, fit, Offset.Zero) {
                translate(-art.x, -art.y - lift) {
                    art.shapes.forEachIndexed { i, shape ->
                        val t = ((front - i) / WINDOW).coerceIn(0f, 1f)
                        if (t <= 0f) return@forEachIndexed
                        translate(0f, (1f - t) * 14f) {
                            shape.fill?.let { drawPath(paths[i], Color(it), alpha = t) }
                            shape.stroke?.let {
                                val cap = if (shape.round) StrokeCap.Round else StrokeCap.Butt
                                val join = if (shape.round) StrokeJoin.Round else StrokeJoin.Miter
                                drawPath(paths[i], Color(it), alpha = t, style = Stroke(shape.width, cap = cap, join = join))
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val WINDOW = 6f
