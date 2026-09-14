package dev.mintu.hotseat.ui.practice

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.live.ReportAnswer
import dev.mintu.hotseat.data.Mock
import dev.mintu.hotseat.ui.components.Hairline
import dev.mintu.hotseat.ui.components.Segmented
import dev.mintu.hotseat.ui.components.Sheet
import dev.mintu.hotseat.ui.components.dots
import dev.mintu.hotseat.ui.icons.InkIcon
import dev.mintu.hotseat.ui.theme.Palette
import dev.mintu.hotseat.ui.theme.Type

private val ink = Palette.Light
private val type = Type.Default

@Composable
fun BoxScope.RoundSheet(state: Practice, onStart: () -> Unit) {
    Sheet(state.picking, onDismiss = { state.picking = false }) {
        BasicText("Pick a round", style = type.headline.copy(color = ink.text, fontSize = type.headline.fontSize * 0.82f))
        BasicText("The interviewer adapts to what you say, so no two runs match.", Modifier.padding(top = 6.dp), style = type.meta.copy(color = ink.caption))

        Spacer(Modifier.height(20.dp))
        Mock.rounds.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEachIndexed { col, round ->
                    val index = row * 2 + col
                    RoundCard(round.title, round.blurb, round.art, round.icon, index == state.round, Modifier.weight(1f)) { state.round = index }
                }
            }
        }

        if (state.round == 3) {
            Label("Job post")
            JobPostField(state.jobPost) { state.jobPost = it }
        }

        Label("Difficulty")
        Segmented(Mock.difficulties, state.difficulty, { state.difficulty = it })
        Label("Interviewer")
        Segmented(Mock.styles, state.style, { state.style = it })

        Spacer(Modifier.height(24.dp))
        PrimaryButton(if (state.canStart) "Start interview" else "Paste a job post to start", enabled = state.canStart) { onStart() }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RoundCard(title: String, blurb: String, art: Int, icon: InkIcon, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "card press")
    val border by animateColorAsState(if (selected) ink.tabTint else Color(0x14000000), tween(200), label = "card border")
    val fill by animateColorAsState(if (selected) Color(0xFFEFF5FE) else Color(0xFFF6F6F8), tween(200), label = "card fill")
    val lift = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (!selected) return@LaunchedEffect
        lift.snapTo(-8f)
        lift.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 300f))
    }
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = lift.value * density
            }
            .clip(RoundedCornerShape(22.dp))
            .background(fill)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(22.dp))
            .clickable(press, indication = null, onClick = onClick)
            .padding(14.dp),
    ) {
        Image(
            painterResource(art),
            null,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.35f)
                .padding(horizontal = 10.dp)
                .padding(top = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            InkIcon(icon, 14.dp, if (selected) ink.tabTint else ink.text)
            Spacer(Modifier.width(6.dp))
            BasicText(title, style = type.meta.copy(color = ink.text))
        }
        BasicText(blurb, Modifier.padding(top = 3.dp), style = type.caption.copy(color = ink.caption), maxLines = 2)
    }
}

@Composable
fun BoxScope.ReportSheet(state: Practice) {
    val report = state.report ?: return
    Sheet(state.reviewing, onDismiss = { state.reviewing = false }) {
        BasicText("YOUR REPORT", style = type.tabLabel.copy(color = ink.caption))
        Row(verticalAlignment = Alignment.Bottom) {
            BasicText(report.score.toString(), style = type.display.copy(color = ink.text, fontSize = type.display.fontSize * 0.7f))
            BasicText("/100", Modifier.padding(start = 6.dp, bottom = 14.dp), style = type.meta.copy(color = ink.caption))
        }
        BasicText(report.summary, style = type.meta.copy(color = ink.meta))
        BasicText(dots(report.duration, "${report.wpm} wpm", "${report.fillers} fillers"), Modifier.padding(top = 6.dp, bottom = 18.dp), style = type.caption.copy(color = ink.caption))

        report.answers.forEachIndexed { i, answer ->
            Hairline()
            AnswerRow(i + 1, answer)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Practice again") { state.backToIdle() }
    }
}

@Composable
private fun AnswerRow(number: Int, answer: ReportAnswer) {
    Column(Modifier.padding(vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText("Q$number", style = type.tabLabel.copy(color = ink.caption))
            Spacer(Modifier.width(8.dp))
            BasicText(answer.question, Modifier.weight(1f), style = type.meta.copy(color = ink.text))
            ScorePill(answer.score)
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("S", "T", "A", "R").forEachIndexed { i, letter ->
                StarMeter(letter, answer.star[i], Modifier.weight(1f))
            }
        }
        BasicText(answer.note, Modifier.padding(top = 12.dp), style = type.caption.copy(color = ink.meta, fontSize = type.meta.fontSize * 0.9f))
        Row(Modifier.padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFEFF5FE)).padding(12.dp)) {
            BasicText("Try  ", style = type.caption.copy(color = ink.tabTint))
            BasicText(answer.better, style = type.caption.copy(color = ink.meta))
        }
    }
}

/** Tick meter in the scrubber style, filled ticks show how strong that part of STAR was. */
@Composable
fun StarMeter(letter: String, value: Float, modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(value) { shown.animateTo(value, tween(700)) }
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(16.dp)) {
            val count = 10
            val pitch = size.width / count
            for (i in 0 until count) {
                val on = i < (shown.value * count).toInt()
                val x = i * pitch + pitch / 2
                val h = if (on) size.height else size.height * 0.55f
                drawLine(if (on) ink.tickActive else ink.tick.copy(alpha = 0.5f), Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), 1.6.dp.toPx())
            }
        }
        BasicText(letter, Modifier.padding(top = 4.dp), style = type.tabLabel.copy(color = ink.caption))
    }
}

@Composable
fun ScorePill(score: Int) {
    val color = when {
        score >= 85 -> Color(0xFF1F9D55)
        score >= 75 -> ink.tabTint
        else -> Color(0xFFE08A1E)
    }
    Box(Modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        BasicText(score.toString(), style = type.pill.copy(color = color, fontSize = type.pill.fontSize * 0.85f))
    }
}

@Composable
private fun JobPostField(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF6F6F8))
            .border(1.dp, Color(0x14000000), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        if (value.isEmpty()) {
            BasicText("Paste the role description, responsibilities and must haves.", style = type.meta.copy(color = ink.caption))
        }
        BasicTextField(
            value,
            { onChange(it.take(6000)) },
            Modifier.fillMaxWidth(),
            textStyle = type.meta.copy(color = ink.text),
        )
    }
}

@Composable
private fun Label(text: String) {
    BasicText(text.uppercase(), Modifier.padding(top = 18.dp, bottom = 8.dp), style = type.tabLabel.copy(color = ink.caption))
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "button press")
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (enabled) ink.pill else ink.pill.copy(alpha = 0.3f))
            .clickable(press, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = type.pill.copy(color = Color.White))
    }
}
