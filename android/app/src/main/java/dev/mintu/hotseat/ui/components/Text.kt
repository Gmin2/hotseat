package dev.mintu.hotseat.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.mintu.hotseat.ui.theme.Motion

fun dots(vararg parts: String) = parts.joinToString(" · ")

/**
 * Types [text] out like the reference headline, newest characters fade in.
 * Changing [text] starts over. [instant] skips the typing.
 */
@Composable
fun TypedText(text: String, style: TextStyle, modifier: Modifier = Modifier, instant: Boolean = false) {
    val shown = remember(text) { Animatable(if (instant) text.length.toFloat() else 0f) }
    LaunchedEffect(text, instant) {
        if (instant) {
            shown.snapTo(text.length.toFloat())
            return@LaunchedEffect
        }
        val ms = (text.length * 1000 / Motion.TYPE_CHARS_PER_SEC).coerceAtLeast(1)
        shown.animateTo(text.length.toFloat(), tween(ms, easing = LinearEasing))
    }
    val color = style.color
    val count = shown.value
    val fade = 6f
    BasicText(
        buildAnnotatedString {
            val whole = count.toInt().coerceIn(0, text.length)
            append(text.substring(0, (whole - fade.toInt()).coerceAtLeast(0)))
            for (i in (whole - fade.toInt()).coerceAtLeast(0) until whole) {
                val alpha = ((count - i) / fade).coerceIn(0.25f, 1f)
                withStyle(SpanStyle(color = color.copy(alpha = color.alpha * alpha))) { append(text[i]) }
            }
            // keep the rest in layout so lines do not jump while typing
            withStyle(SpanStyle(color = Color.Transparent)) { append(text.substring(whole)) }
        },
        modifier,
        style = style,
    )
}

/** Each character rolls up when it changes, like the reference temperature. */
@Composable
fun RollingText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    Row(modifier) {
        text.forEachIndexed { i, ch ->
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) { it / 2 } + fadeIn(tween(160)))
                        .togetherWith(slideOutVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) { -it / 2 } + fadeOut(tween(120)))
                },
                label = "digit $i",
            ) { c -> BasicText(c.toString(), style = style) }
        }
    }
}
