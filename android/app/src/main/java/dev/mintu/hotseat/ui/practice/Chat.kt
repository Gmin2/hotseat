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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import dev.mintu.hotseat.ui.theme.loop

// your bubbles take the mascot's blue, the interviewer's sit on a pale blue grey inside the white card
private val YouBlue = Color(0xFF2F6CE5)
private val InterviewerFill = Color(0xFFF2F5FB)

/** What the chat shows at the bottom while nobody's words have landed yet. */
enum class Typing { None, Interviewer, Candidate }

/**
 * The interview as a conversation: the interviewer on the left with the bird, you on the right.
 * Deltas grow the last bubble in place, new turns spring in and the list follows the newest one.
 */
@Composable
fun ChatBubbles(turns: List<Turn>, typing: Typing, interviewerLevel: Float, modifier: Modifier = Modifier, header: @Composable () -> Unit = {}) {
    val list = rememberLazyListState()
    val count = turns.size + if (typing == Typing.None) 0 else 1
    val lastLength = turns.lastOrNull()?.text?.length ?: 0

    LaunchedEffect(count, lastLength) {
        if (count > 0) list.animateScrollToItem(count - 1, scrollOffset = Int.MAX_VALUE / 2)
    }

    Column(
        modifier
            .shadow(24.dp, RoundedCornerShape(30.dp), ambientColor = Color(0x1F2A4A8C), spotColor = Color(0x1F2A4A8C))
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE6EBF5), RoundedCornerShape(30.dp)),
    ) {
    header()
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEF1F7)))
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            // the oldest bubbles fade out at the top of the card
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.1f to Color.Black), blendMode = BlendMode.DstIn)
            },
        state = list,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 14.dp),
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
}

/** The strip at the top of the card: a live dot and what is going on, then small stat chips. */
@Composable
fun ChatHeader(status: String, live: Boolean, stats: List<String>) {
    val t = HotseatTheme.type
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val pulse = loop(900, "live dot", reverse = true, still = 1f)
        Box(contentAlignment = Alignment.Center) {
            if (live) Box(Modifier.size(14.dp).graphicsLayer { scaleX = 0.6f + 0.6f * pulse; scaleY = 0.6f + 0.6f * pulse }.clip(CircleShape).background(Color(0x33E03143)))
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (live) Color(0xFFE03143) else Color(0xFF9AA3B5)))
        }
        BasicText(status, Modifier.padding(start = 8.dp).weight(1f), style = t.meta.copy(color = Color(0xFF141210)), maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            stats.forEach { stat ->
                Box(Modifier.clip(CircleShape).background(Color(0xFFF2F5FB)).padding(horizontal = 9.dp, vertical = 4.dp)) {
                    BasicText(stat, style = t.caption.copy(color = Color(0xFF4A5468)))
                }
            }
        }
    }
}

@Composable
private fun bubbleText(speaker: Speaker) = HotseatTheme.type.meta.copy(
    color = if (speaker == Speaker.candidate) Color.White else Color(0xFF141210),
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
            IdleMascot(40.dp, mic = level)
            Spacer(Modifier.size(6.dp))
        }
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .shadow(if (mine) 8.dp else 0.dp, bubbleShape(mine), ambientColor = Color(0x552F6CE5), spotColor = Color(0x552F6CE5))
                .clip(bubbleShape(mine))
                .background(if (mine) YouBlue else InterviewerFill)
                .then(if (mine) Modifier else Modifier.border(1.dp, Color(0x0F1B3A7A), bubbleShape(mine)))
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
    Row(Modifier.padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { i ->
            val bob = loop(560, "dot $i", reverse = true, delayMs = i * 140, still = 0.6f)
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
