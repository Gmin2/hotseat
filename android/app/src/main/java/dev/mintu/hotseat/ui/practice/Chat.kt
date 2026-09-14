package dev.mintu.hotseat.ui.practice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.live.Speaker
import dev.mintu.hotseat.live.Turn
import dev.mintu.hotseat.ui.brand.IdleMascot
import dev.mintu.hotseat.ui.theme.HotseatTheme

/** What the chat shows at the bottom while nobody's words have landed yet. */
enum class Typing { None, Interviewer, Candidate }

/**
 * The interview as a conversation: the interviewer on the left with the bird, you on the right.
 * Deltas grow the last bubble in place, new turns spring in and the list follows the newest one.
 */
@Composable
fun ChatBubbles(turns: List<Turn>, typing: Typing, interviewerLevel: Float, modifier: Modifier = Modifier) {
    val list = rememberLazyListState()
    val count = turns.size + if (typing == Typing.None) 0 else 1
    val lastLength = turns.lastOrNull()?.text?.length ?: 0

    LaunchedEffect(count, lastLength) {
        if (count > 0) list.animateScrollToItem(count - 1, scrollOffset = Int.MAX_VALUE / 2)
    }

    LazyColumn(
        modifier
            // the oldest bubbles fade out under the headline
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.16f to Color.Black), blendMode = BlendMode.DstIn)
            },
        state = list,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 8.dp),
    ) {
        itemsIndexed(turns, key = { i, t -> "${t.speaker}-${t.startMs}-$i" }) { i, turn ->
            val newest = i == turns.lastIndex
            Bubble(turn.speaker, if (newest && turn.speaker == Speaker.interviewer) interviewerLevel else 0f) {
                BasicText(turn.text.trim(), style = bubbleText(turn.speaker))
            }
        }
        if (typing != Typing.None) {
            item(key = "typing") {
                val speaker = if (typing == Typing.Interviewer) Speaker.interviewer else Speaker.candidate
                Bubble(speaker, if (typing == Typing.Interviewer) interviewerLevel else 0f) { Dots(bubbleText(speaker).color) }
            }
        }
    }
}

@Composable
private fun bubbleText(speaker: Speaker) = HotseatTheme.type.meta.copy(
    color = if (speaker == Speaker.candidate) HotseatTheme.palette.pillText else Color(0xFF141210),
    lineHeight = HotseatTheme.type.meta.lineHeight * 1.1f,
)

@Composable
private fun Bubble(speaker: Speaker, level: Float, content: @Composable () -> Unit) {
    val p = HotseatTheme.palette
    val mine = speaker == Speaker.candidate
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f)) }

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enter.value.coerceIn(0f, 1f)
                scaleX = 0.85f + 0.15f * enter.value
                scaleY = 0.85f + 0.15f * enter.value
                translationY = (1f - enter.value) * 16.dp.toPx()
                transformOrigin = TransformOrigin(if (mine) 1f else 0f, 1f)
            },
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!mine) {
            IdleMascot(34.dp, mic = level)
            Spacer(Modifier.size(6.dp))
        }
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .shadow(if (mine) 0.dp else 10.dp, bubbleShape(mine), ambientColor = Color(0x223B5BA8), spotColor = Color(0x223B5BA8))
                .clip(bubbleShape(mine))
                .background(if (mine) p.pill else Color.White)
                .then(if (mine) Modifier else Modifier.border(1.dp, Color(0x14000000), bubbleShape(mine)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) { content() }
    }
}

private fun bubbleShape(mine: Boolean) = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = if (mine) 20.dp else 6.dp,
    bottomEnd = if (mine) 6.dp else 20.dp,
)

@Composable
private fun Dots(color: Color, size: Dp = 7.dp) {
    val beat = rememberInfiniteTransition(label = "typing")
    Row(Modifier.padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { i ->
            val bob by beat.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(560, delayMillis = i * 140), RepeatMode.Reverse),
                label = "dot $i",
            )
            Box(
                Modifier
                    .size(size)
                    .graphicsLayer { translationY = -bob * 4.dp.toPx() }
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.35f + 0.5f * bob)),
            )
        }
    }
}
