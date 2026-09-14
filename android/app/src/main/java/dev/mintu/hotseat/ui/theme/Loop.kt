package dev.mintu.hotseat.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * True when the system "remove animations" setting is on. Ambient loops (drifting clouds, floating art, blinking)
 * hold still, one shot motion like taps and sheets still plays. UI tests turn it on too, since loops never go idle.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** A 0 to 1 value that loops forever, or sits at [still] when motion is reduced. */
@Composable
fun loop(
    durationMs: Int,
    label: String,
    reverse: Boolean = false,
    easing: Easing = LinearEasing,
    delayMs: Int = 0,
    still: Float = 0f,
): Float {
    if (LocalReduceMotion.current) return still
    val transition = rememberInfiniteTransition(label = label)
    val value by transition.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(durationMs, delayMillis = delayMs, easing = easing), if (reverse) RepeatMode.Reverse else RepeatMode.Restart),
        label = label,
    )
    return value
}
