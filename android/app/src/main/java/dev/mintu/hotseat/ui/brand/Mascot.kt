package dev.mintu.hotseat.ui.brand

import dev.mintu.hotseat.ui.theme.LocalReduceMotion
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Immutable
data class MascotShape(val layer: String, val d: String, val fill: Long?, val stroke: Long?, val width: Float)

/**
 * How the bird is posed this frame. [open] 0 is asleep, [look] shifts the pupils in box units,
 * [wing] 0 rests and 1 flaps both wings up, [mic] makes the mic tip glow, [lift] moves the whole bird up.
 */
@Immutable
data class MascotPose(
    val open: Float = 1f,
    val look: Float = 0f,
    val wing: Float = 0f,
    val mic: Float = 0f,
    val lift: Float = 0f,
)

private val paths by lazy { MascotArt.shapes.map { if (it.d.isEmpty()) null else PathParser().parsePathString(it.d).toPath() } }

@Composable
fun Mascot(size: Dp, pose: MascotPose, modifier: Modifier = Modifier, sticker: Boolean = false, tight: Boolean = false) {
    Canvas(modifier.size(size)) {
        // tight crops the empty sticker margin so a small bird fills its slot
        val box = if (tight) TIGHT_BOX else MascotArt.BOX
        val unit = this.size.minDimension / box
        val origin = if (tight) Offset(-TIGHT_X * unit, -TIGHT_Y * unit) else Offset.Zero
        translate(origin.x, origin.y) {
        scale(unit, unit, Offset.Zero) {
            if (sticker) drawSticker()
            translate(0f, -pose.lift) {
                MascotArt.shapes.forEachIndexed { i, shape ->
                    when (shape.layer) {
                        "eyes" -> drawEyes(pose)
                        "wingL" -> rotate(48f * pose.wing, Offset(MascotArt.WING_L_X, MascotArt.WING_L_Y)) { drawShape(shape, paths[i]!!) }
                        "wingR" -> rotate(-48f * pose.wing, Offset(MascotArt.WING_R_X, MascotArt.WING_R_Y)) { drawShape(shape, paths[i]!!) }
                        "mic" -> {
                            if (pose.mic > 0.01f) {
                                val c = Offset(MascotArt.MIC_X, MascotArt.MIC_Y)
                                drawCircle(Color(0xFFE03143).copy(alpha = 0.35f * pose.mic), 16f + 26f * pose.mic, c)
                            }
                            drawShape(shape, paths[i]!!)
                        }
                        else -> drawShape(shape, paths[i]!!)
                    }
                }
            }
        }
        }
    }
}

// the bird with its headset spans about x 90 to 422 and y 96 to 440 in the 512 box, with room for a hop
private const val TIGHT_X = 86f
private const val TIGHT_Y = 96f
private const val TIGHT_BOX = 344f

/** A small bird that blinks now and then on its own, for the chip and chat avatars. Bumping [pulse] makes it hop and flap. */
@Composable
fun IdleMascot(size: Dp, modifier: Modifier = Modifier, mic: Float = 0f, pulse: Int = 0) {
    val open = remember { Animatable(1f) }
    val wing = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    val reduce = LocalReduceMotion.current
    LaunchedEffect(reduce) {
        while (!reduce) {
            delay(2_800L + Random.nextLong(2_600))
            open.animateTo(0f, tween(90))
            open.animateTo(1f, tween(140))
        }
    }
    LaunchedEffect(pulse) {
        if (pulse == 0) return@LaunchedEffect
        coroutineScope {
            launch {
                hop.animateTo(1f, tween(150))
                hop.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 420f))
            }
            launch {
                repeat(2) {
                    wing.animateTo(1f, tween(120))
                    wing.animateTo(0f, tween(130))
                }
            }
        }
    }
    Mascot(size, MascotPose(open = open.value, mic = mic, wing = wing.value, lift = hop.value * 36f), modifier, tight = true)
}

private fun DrawScope.drawShape(shape: MascotShape, path: Path) {
    shape.fill?.let { drawPath(path, Color(it)) }
    shape.stroke?.let { drawPath(path, Color(it), style = Stroke(shape.width, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
}

private fun DrawScope.drawSticker() {
    drawOval(
        Brush.radialGradient(
            0.3f to Color.White,
            1f to Color(MascotArt.PAPER),
            center = Offset(MascotArt.STICKER_GLOW_X, MascotArt.STICKER_GLOW_Y),
            radius = MascotArt.STICKER_GLOW_R,
        ),
        topLeft = Offset(MascotArt.STICKER_CX - MascotArt.STICKER_RX, MascotArt.STICKER_CY - MascotArt.STICKER_RY),
        size = Size(MascotArt.STICKER_RX * 2, MascotArt.STICKER_RY * 2),
    )
}

private fun DrawScope.drawEyes(pose: MascotPose) {
    val ink = Color(MascotArt.EYE_INK)
    for (cx in listOf(MascotArt.EYE_L, MascotArt.EYE_R)) {
        if (pose.open < 0.15f) {
            val lid = Path().apply {
                moveTo(cx - MascotArt.EYE_W, MascotArt.EYE_Y)
                quadraticTo(cx, MascotArt.EYE_Y + 14f, cx + MascotArt.EYE_W, MascotArt.EYE_Y)
            }
            drawPath(lid, ink, style = Stroke(8f, cap = StrokeCap.Round))
            continue
        }
        val ry = MascotArt.EYE_H * pose.open
        val x = cx + pose.look
        drawOval(ink, Offset(x - MascotArt.EYE_W, MascotArt.EYE_Y - ry), Size(MascotArt.EYE_W * 2, ry * 2))
        if (pose.open > 0.3f) drawCircle(Color.White, MascotArt.CATCHLIGHT, Offset(x - 7f, MascotArt.EYE_Y - ry * 0.4f))
    }
}
