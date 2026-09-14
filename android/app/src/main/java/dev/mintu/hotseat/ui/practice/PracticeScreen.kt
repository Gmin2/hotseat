package dev.mintu.hotseat.ui.practice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.data.Line
import dev.mintu.hotseat.data.Mock
import dev.mintu.hotseat.data.Speaker
import dev.mintu.hotseat.ui.art.HotseatScene
import dev.mintu.hotseat.ui.components.ChipState
import dev.mintu.hotseat.ui.components.Mood
import dev.mintu.hotseat.ui.components.PlayState
import dev.mintu.hotseat.ui.components.RollingText
import dev.mintu.hotseat.ui.components.RoundButton
import dev.mintu.hotseat.ui.components.Scrubber
import dev.mintu.hotseat.ui.components.StatusChip
import dev.mintu.hotseat.ui.components.TypedText
import dev.mintu.hotseat.ui.components.dots
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme
import kotlin.math.abs
import kotlin.math.sin

enum class Phase { Idle, Live, Report }

@Stable
class Practice {
    var phase by mutableStateOf(Phase.Idle)
    var round by mutableIntStateOf(0)
    var difficulty by mutableIntStateOf(1)
    var style by mutableIntStateOf(0)
    var elapsed by mutableLongStateOf(0L)
    var playing by mutableStateOf(false)
    var clock by mutableFloatStateOf(0f)
    var picking by mutableStateOf(false)
    var reviewing by mutableStateOf(false)

    val progress get() = (elapsed.toFloat() / Mock.totalMs).coerceIn(0f, 1f)

    val line: Line? get() = Mock.script(round).lastOrNull { it.at <= elapsed }

    val question get() = line?.question ?: 1

    // the voice stays up while a line is still being "said"
    val level: Float
        get() {
            val l = line ?: return 0f
            if (!playing) return 0f
            val talking = elapsed - l.at < l.text.length * 1000L / 14 + 600
            return if (talking) 0.45f + 0.55f * abs(sin(clock * 11f) * sin(clock * 3.7f)) else 0f
        }

    val mood: Mood
        get() = when (phase) {
            Phase.Idle -> Mood.Day
            Phase.Report -> if (playing) liveMood() else Mood.Day
            Phase.Live -> liveMood()
        }

    private fun liveMood(): Mood {
        val l = line ?: return Mood.Day
        return when {
            l.pushback && elapsed - l.at < 900 -> Mood.Dusk
            l.pushback -> Mood.Overcast
            l.speaker == Speaker.You -> Mood.Night
            else -> Mood.Day
        }
    }

    fun start() {
        picking = false
        elapsed = 0
        phase = Phase.Live
        playing = true
    }

    fun end() {
        playing = false
        elapsed = Mock.totalMs
        phase = Phase.Report
    }

    fun replay() {
        elapsed = 0
        playing = true
    }
}

@Composable
fun rememberPractice() = remember { Practice() }

@Composable
fun PracticeScreen(state: Practice, modifier: Modifier = Modifier) {
    val p = HotseatTheme.palette
    val t = HotseatTheme.type

    LaunchedEffect(state.playing) {
        if (!state.playing) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (state.playing) {
            withFrameMillis { now ->
                state.elapsed += now - last
                state.clock += (now - last) / 1000f
                last = now
            }
            if (state.elapsed >= Mock.totalMs) {
                if (state.phase == Phase.Live) state.end() else {
                    state.elapsed = Mock.totalMs
                    state.playing = false
                }
            }
        }
    }

    val round = Mock.rounds[state.round]
    val report = Mock.report(state.round)
    val line = state.line

    Box(modifier.fillMaxSize()) {
        HotseatScene(
            level = state.level,
            modifier = Modifier.align(Alignment.Center).offset(y = 26.dp).fillMaxWidth(0.8f),
        )

        Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
            StatusChip(
                when (state.phase) {
                    Phase.Idle -> ChipState.Ready
                    Phase.Live -> ChipState.Live
                    Phase.Report -> ChipState.Done
                },
                Modifier.padding(start = Dimens.chipInset),
            )
            val headline = when {
                state.phase == Phase.Idle -> "Ready when you are. ${round.title} round, ${Mock.difficulties[state.difficulty].lowercase()}."
                state.phase == Phase.Report && !state.playing -> report.summary
                line != null -> line.text
                else -> ""
            }
            val speaker = when {
                state.phase == Phase.Idle -> "Interviewer"
                state.phase == Phase.Report && !state.playing -> "Your report"
                line?.speaker == Speaker.You -> "You"
                else -> "Interviewer"
            }
            AnimatedContent(
                speaker,
                transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(150))) },
                label = "speaker",
            ) {
                BasicText(it.uppercase(), Modifier.padding(start = Dimens.gutter, top = 24.dp), style = t.tabLabel.copy(color = p.text.copy(alpha = 0.55f)))
            }
            TypedText(
                headline,
                t.headline.copy(color = if (line?.speaker == Speaker.You && state.phase != Phase.Idle && state.playing) p.text.copy(alpha = 0.72f) else p.text),
                Modifier.padding(start = Dimens.gutter, end = Dimens.gutter, top = 6.dp),
            )
        }

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = Dimens.gutter, end = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val number = when (state.phase) {
                    Phase.Idle -> "15:00"
                    Phase.Live -> clock(state.elapsed)
                    Phase.Report -> if (state.playing) clock(state.elapsed) else report.score.toString()
                }
                RollingText(number, t.display.copy(color = p.display), Modifier.offset(y = -Dimens.displayNudge))
                Spacer(Modifier.weight(1f))
                RoundButton(
                    onClick = {
                        when (state.phase) {
                            Phase.Idle -> state.picking = true
                            Phase.Live -> state.end()
                            Phase.Report -> state.reviewing = true
                        }
                    },
                )
            }
            BasicText(
                dots(round.title, Mock.role),
                Modifier.padding(start = Dimens.gutter),
                style = t.meta.copy(color = p.meta),
            )
            val caption = when (state.phase) {
                Phase.Idle -> dots("${Mock.questions} questions", Mock.difficulties[state.difficulty], Mock.styles[state.style])
                Phase.Live -> dots("Q${state.question} of ${Mock.questions}", "${128 + (state.elapsed / 1000 % 19)} wpm", "${Mock.script(state.round).count { it.speaker == Speaker.You && it.at <= state.elapsed } * 2} fillers")
                Phase.Report -> dots(report.duration, "${report.wpm} wpm", "${report.fillers} fillers")
            }
            BasicText(caption, Modifier.padding(start = Dimens.gutter, top = Dimens.metaGap), style = t.caption.copy(color = p.caption))

            val label = when (state.phase) {
                Phase.Idle -> "Start"
                Phase.Live -> "Q${state.question}"
                Phase.Report -> if (state.elapsed >= Mock.totalMs && !state.playing) "Done" else "Q${state.question} · ${report.answers[state.question - 1].score}"
            }
            val play = when {
                state.phase == Phase.Idle -> PlayState.Play
                state.playing -> PlayState.Pause
                state.phase == Phase.Report && state.elapsed >= Mock.totalMs -> PlayState.Replay
                else -> PlayState.Play
            }
            Scrubber(
                progress = state.progress,
                label = label,
                play = play,
                level = state.level,
                onPlay = {
                    when {
                        state.phase == Phase.Idle -> state.start()
                        play == PlayState.Replay -> state.replay()
                        else -> state.playing = !state.playing
                    }
                },
                onSeek = if (state.phase == Phase.Report) { f ->
                    state.playing = false
                    state.elapsed = (f * Mock.totalMs).toLong().coerceAtMost(Mock.totalMs - 1)
                } else null,
                modifier = Modifier.padding(top = Dimens.captionToTicks + 8.dp),
            )
            Spacer(Modifier.height(Dimens.tabBarBottom + Dimens.tabBarHeight + 36.dp))
        }
    }
}

private fun clock(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
