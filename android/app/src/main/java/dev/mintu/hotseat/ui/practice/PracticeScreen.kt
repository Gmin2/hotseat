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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mintu.hotseat.data.Mock
import dev.mintu.hotseat.live.Speaker
import dev.mintu.hotseat.live.candidatePace
import dev.mintu.hotseat.live.clock
import dev.mintu.hotseat.ui.art.HotseatScene
import dev.mintu.hotseat.ui.components.ChipState
import dev.mintu.hotseat.ui.components.PlayState
import dev.mintu.hotseat.ui.components.RollingText
import dev.mintu.hotseat.ui.components.EndButton
import dev.mintu.hotseat.ui.components.RoundButton
import dev.mintu.hotseat.ui.components.Scrubber
import dev.mintu.hotseat.ui.components.StatusChip
import dev.mintu.hotseat.ui.components.TypedText
import dev.mintu.hotseat.ui.components.dots
import dev.mintu.hotseat.ui.components.fillers
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme

@Composable
fun PracticeScreen(state: Practice, onNeedMic: () -> Unit, modifier: Modifier = Modifier) {
    val p = HotseatTheme.palette
    val t = HotseatTheme.type
    val round = Mock.rounds[state.round]
    val report = state.report
    val phase = state.phase
    // in the report the playhead can sit mid interview, then the headline shows that moment instead of the summary
    val reviewingLine = phase == Phase.Report && (state.replaying || state.elapsed < state.totalMs)

    val chat = phase == Phase.Connecting || phase == Phase.Live || phase == Phase.Scoring || reviewingLine ||
        (phase == Phase.Failed && state.turns.isNotEmpty())

    Box(modifier.fillMaxSize()) {
        // before and after an interview the hot seat scene, during it the conversation
        AnimatedContent(
            chat,
            Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(320, delayMillis = 120)).togetherWith(fadeOut(tween(200))) },
            label = "scene or chat",
        ) { showChat ->
            Box(Modifier.fillMaxSize()) {
                if (showChat) {
                    val shown = if (phase == Phase.Report) state.turns.filter { it.startMs <= state.elapsed } else state.turns
                    val last = shown.lastOrNull()
                    val typing = when {
                        phase == Phase.Connecting -> Typing.Interviewer
                        phase != Phase.Live -> Typing.None
                        last == null -> Typing.Interviewer
                        state.interviewerLevel > 0.1f && last.speaker != Speaker.interviewer -> Typing.Interviewer
                        state.candidateLevel > 0.14f && !state.muted && last.speaker != Speaker.candidate -> Typing.Candidate
                        else -> Typing.None
                    }
                    val pace = candidatePace(shown)
                    val status = when {
                        phase == Phase.Connecting -> "Connecting"
                        phase == Phase.Scoring -> "Scoring your answers"
                        phase == Phase.Report -> "Replay"
                        phase == Phase.Failed -> "Paused"
                        state.muted -> "Muted"
                        else -> "Live"
                    }
                    val stats = buildList {
                        add("Q${state.question}")
                        if (pace.wpm > 0) add("${pace.wpm} wpm")
                        add(fillers(pace.fillers))
                    }
                    ChatBubbles(
                        shown,
                        typing,
                        state.level,
                        Modifier.fillMaxSize().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 142.dp, bottom = 262.dp),
                        header = { ChatHeader(status, live = phase == Phase.Live && !state.muted, stats = stats) },
                    )
                } else {
                    HotseatScene(
                        level = state.level,
                        modifier = Modifier.align(Alignment.Center).offset(y = 26.dp).fillMaxWidth(0.8f),
                    )
                }
            }
        }

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
                // the card header already says live, up here it is the setup you picked
                chat && phase != Phase.Failed -> dots(Mock.difficulties[state.difficulty], Mock.styles[state.style])
                phase == Phase.Idle -> if (state.demo) "Demo interview" else "Interviewer"
                phase == Phase.Connecting -> "Connecting"
                phase == Phase.Scoring -> "Scoring"
                phase == Phase.Failed -> "Something went wrong"
                phase == Phase.Report && !reviewingLine -> "Your report"
                phase == Phase.Report -> "Replay"
                state.muted -> "Live · muted"
                else -> "Live"
            }
            val headline = when {
                phase == Phase.Idle -> "Ready when you are. ${round.title} round, ${Mock.difficulties[state.difficulty].lowercase()}."
                phase == Phase.Connecting -> "Pulling up a chair for you."
                phase == Phase.Scoring -> "Reading back your answers."
                phase == Phase.Failed -> state.error.orEmpty()
                phase == Phase.Report && !reviewingLine -> report?.summary.orEmpty()
                // the conversation lives in the bubbles, the headline only says where you are
                else -> "${round.title}, question ${state.question}."
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
                // during the conversation the headline steps back so the card has the room
                if (chat) t.headline.copy(color = p.text, fontSize = 21.sp, lineHeight = 28.sp) else t.headline.copy(color = p.text),
                Modifier.padding(start = Dimens.gutter, end = Dimens.gutter, top = 6.dp),
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
                val size by animateFloatAsState(if (chat) 0.6f else 1f, tween(420), label = "timer size")
                RollingText(
                    number,
                    t.display.copy(color = p.display, fontSize = t.display.fontSize * size, lineHeight = t.display.lineHeight * size),
                    Modifier.offset(y = -Dimens.displayNudge * size),
                )
                Spacer(Modifier.weight(1f))
                when (phase) {
                    Phase.Connecting -> EndButton("Cancel", enabled = true, onClick = state::end)
                    Phase.Live -> EndButton("End", enabled = true, onClick = state::end)
                    Phase.Scoring -> EndButton("Scoring", enabled = false, onClick = {})
                    else -> RoundButton(
                        onClick = {
                            when (phase) {
                                Phase.Idle -> state.picking = true
                                Phase.Report -> state.reviewing = true
                                else -> state.retry()
                            }
                        },
                    )
                }
            }
            BasicText(
                if (chat) dots(state.role.ifBlank { Mock.role }, "${state.minutes} min") else dots(round.title, state.role.ifBlank { Mock.role }),
                Modifier.padding(start = Dimens.gutter),
                style = t.meta.copy(color = p.meta),
            )
            val pace = candidatePace(state.turns)
            val caption = when (phase) {
                Phase.Idle, Phase.Failed, Phase.Connecting -> dots(if (state.demo) "demo, no mic" else "voice interview", Mock.difficulties[state.difficulty], Mock.styles[state.style])
                Phase.Live, Phase.Scoring -> dots("Q${state.question}", if (pace.wpm > 0) "${pace.wpm} wpm" else "listening", fillers(pace.fillers))
                Phase.Report -> report?.let { dots(it.duration, "${it.wpm} wpm", fillers(it.fillers)) }.orEmpty()
            }
            // in the chat these numbers live in the card header instead
            if (!chat) BasicText(caption, Modifier.padding(start = Dimens.gutter, top = Dimens.metaGap), style = t.caption.copy(color = p.caption))

            val label = when {
                chat -> null
                phase == Phase.Report -> "Done"
                else -> "Start"
            }
            val play = when (phase) {
                Phase.Idle, Phase.Failed -> PlayState.Play
                // live the round button is the mic, tap to mute
                Phase.Live -> if (state.muted) PlayState.MicOff else PlayState.Mic
                Phase.Report -> if (state.replaying) PlayState.Pause else if (reviewingLine) PlayState.Play else PlayState.Replay
                else -> PlayState.Mic
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
                modifier = Modifier.padding(top = if (chat) 14.dp else Dimens.captionToTicks + 8.dp),
            )
            Spacer(Modifier.height(Dimens.tabBarBottom + Dimens.tabBarHeight + 36.dp))
        }
    }
}
