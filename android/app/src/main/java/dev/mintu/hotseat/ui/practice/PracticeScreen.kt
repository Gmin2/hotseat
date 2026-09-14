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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.data.Mock
import dev.mintu.hotseat.live.Speaker
import dev.mintu.hotseat.live.candidatePace
import dev.mintu.hotseat.live.clock
import dev.mintu.hotseat.ui.art.HotseatScene
import dev.mintu.hotseat.ui.components.ChipState
import dev.mintu.hotseat.ui.components.PlayState
import dev.mintu.hotseat.ui.components.RollingText
import dev.mintu.hotseat.ui.components.RoundButton
import dev.mintu.hotseat.ui.components.Scrubber
import dev.mintu.hotseat.ui.components.StatusChip
import dev.mintu.hotseat.ui.components.TypedText
import dev.mintu.hotseat.ui.components.dots
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme

@Composable
fun PracticeScreen(state: Practice, onNeedMic: () -> Unit, modifier: Modifier = Modifier) {
    val p = HotseatTheme.palette
    val t = HotseatTheme.type
    val round = Mock.rounds[state.round]
    val report = state.report
    val line = state.line
    val phase = state.phase
    // in the report the playhead can sit mid interview, then the headline shows that moment instead of the summary
    val reviewingLine = phase == Phase.Report && (state.replaying || state.elapsed < state.totalMs)

    Box(modifier.fillMaxSize()) {
        HotseatScene(
            level = state.level,
            modifier = Modifier.align(Alignment.Center).offset(y = 26.dp).fillMaxWidth(0.8f),
        )

        Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
            StatusChip(
                when (phase) {
                    Phase.Idle, Phase.Failed -> ChipState.Ready
                    Phase.Connecting, Phase.Scoring -> ChipState.Connecting
                    Phase.Live -> ChipState.Live
                    Phase.Report -> ChipState.Done
                },
                Modifier.padding(start = Dimens.chipInset),
            )
            val kicker = when {
                phase == Phase.Idle -> if (state.demo) "Demo interview" else "Interviewer"
                phase == Phase.Connecting -> "Connecting"
                phase == Phase.Scoring -> "Scoring"
                phase == Phase.Failed -> "Something went wrong"
                phase == Phase.Report && !reviewingLine -> "Your report"
                line?.speaker == Speaker.candidate -> if (state.muted) "You · muted" else "You"
                else -> "Interviewer"
            }
            val headline = when {
                phase == Phase.Idle -> "Ready when you are. ${round.title} round, ${Mock.difficulties[state.difficulty].lowercase()}."
                phase == Phase.Connecting -> "Pulling up a chair for you."
                phase == Phase.Scoring -> "Reading back your answers."
                phase == Phase.Failed -> state.error.orEmpty()
                phase == Phase.Report && !reviewingLine -> report?.summary.orEmpty()
                line != null -> line.text.trim()
                else -> "Say hi when you hear the interviewer."
            }
            AnimatedContent(
                kicker,
                transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(150))) },
                label = "speaker",
            ) {
                BasicText(it.uppercase(), Modifier.padding(start = Dimens.gutter, top = 24.dp), style = t.tabLabel.copy(color = p.text.copy(alpha = 0.55f)))
            }
            TypedText(
                headline,
                t.headline.copy(color = if (line?.speaker == Speaker.candidate && phase == Phase.Live) p.text.copy(alpha = 0.72f) else p.text),
                Modifier.padding(start = Dimens.gutter, end = Dimens.gutter, top = 6.dp),
                // live transcripts grow a few words at a time, retyping from the start would flicker
                instant = phase == Phase.Live && !state.demo,
            )
        }

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = Dimens.gutter, end = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val number = when (phase) {
                    Phase.Idle, Phase.Failed -> clock(state.minutes * 60_000L)
                    Phase.Connecting -> "00:00"
                    Phase.Live, Phase.Scoring -> clock(state.elapsed)
                    Phase.Report -> if (reviewingLine) clock(state.elapsed) else report?.score?.toString() ?: "--"
                }
                RollingText(number, t.display.copy(color = p.display), Modifier.offset(y = -Dimens.displayNudge))
                Spacer(Modifier.weight(1f))
                RoundButton(
                    onClick = {
                        when (phase) {
                            Phase.Idle -> state.picking = true
                            Phase.Connecting, Phase.Live -> state.end()
                            Phase.Scoring -> Unit
                            Phase.Report -> state.reviewing = true
                            Phase.Failed -> state.retry()
                        }
                    },
                )
            }
            BasicText(
                dots(round.title, Mock.role),
                Modifier.padding(start = Dimens.gutter),
                style = t.meta.copy(color = p.meta),
            )
            val pace = candidatePace(state.turns)
            val caption = when (phase) {
                Phase.Idle, Phase.Failed, Phase.Connecting -> dots(if (state.demo) "demo, no mic" else "voice interview", Mock.difficulties[state.difficulty], Mock.styles[state.style])
                Phase.Live, Phase.Scoring -> dots("Q${state.question}", if (pace.wpm > 0) "${pace.wpm} wpm" else "listening", "${pace.fillers} fillers")
                Phase.Report -> report?.let { dots(it.duration, "${it.wpm} wpm", "${it.fillers} fillers") }.orEmpty()
            }
            BasicText(caption, Modifier.padding(start = Dimens.gutter, top = Dimens.metaGap), style = t.caption.copy(color = p.caption))

            val answers = report?.answers.orEmpty()
            val label = when (phase) {
                Phase.Idle, Phase.Failed -> "Start"
                Phase.Connecting -> "..."
                Phase.Live -> if (state.muted) "Muted" else "Q${state.question}"
                Phase.Scoring -> "Scoring"
                Phase.Report -> if (!reviewingLine) "Done" else answers.getOrNull(state.question - 1)?.let { "Q${state.question} · ${it.score}" } ?: "Q${state.question}"
            }
            val play = when (phase) {
                Phase.Idle, Phase.Failed -> PlayState.Play
                Phase.Live -> if (state.muted) PlayState.Play else PlayState.Pause
                Phase.Report -> if (state.replaying) PlayState.Pause else if (reviewingLine) PlayState.Play else PlayState.Replay
                else -> PlayState.Pause
            }
            Scrubber(
                progress = state.progress,
                label = label,
                play = play,
                level = if (phase == Phase.Live) maxOf(state.interviewerLevel, state.candidateLevel) else state.level,
                onPlay = {
                    when (phase) {
                        Phase.Idle, Phase.Failed -> if (state.demo) state.start() else onNeedMic()
                        Phase.Live -> state.toggleMute()
                        Phase.Report -> state.togglePlayback()
                        else -> Unit
                    }
                },
                onSeek = if (phase == Phase.Report) state::seek else null,
                modifier = Modifier.padding(top = Dimens.captionToTicks + 8.dp),
            )
            Spacer(Modifier.height(Dimens.tabBarBottom + Dimens.tabBarHeight + 36.dp))
        }
    }
}
