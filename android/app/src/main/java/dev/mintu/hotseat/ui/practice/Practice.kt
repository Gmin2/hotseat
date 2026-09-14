package dev.mintu.hotseat.ui.practice

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.mintu.hotseat.data.Mock
import dev.mintu.hotseat.data.Speaker as ScriptSpeaker
import dev.mintu.hotseat.live.ConnectStep
import dev.mintu.hotseat.live.InterviewSetup
import dev.mintu.hotseat.live.LiveEvent
import dev.mintu.hotseat.live.LiveReport
import dev.mintu.hotseat.live.LiveSession
import dev.mintu.hotseat.live.Speaker
import dev.mintu.hotseat.live.Transcript
import dev.mintu.hotseat.live.Turn
import dev.mintu.hotseat.live.VoicePlayback
import dev.mintu.hotseat.live.VoiceSource
import dev.mintu.hotseat.live.WorkerException
import dev.mintu.hotseat.ui.components.Mood
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.abs
import kotlin.math.sin

enum class Phase { Idle, Connecting, Live, Scoring, Report, Failed }

val roundKeys = listOf("behavioral", "technical", "design", "job")
val difficultyKeys = listOf("easy", "medium", "hard")
val styleKeys = listOf("friendly", "sharp")

/** A finished interview, what gets saved and what the report screens read. */
data class Finished(val roundIndex: Int, val seconds: Double, val turns: List<Turn>, val report: LiveReport, val voice: VoiceSource? = null)

/**
 * The practice flow. In live mode a [LiveSession] carries the real interview and [score] turns the transcript into a report,
 * in demo mode the scripted interview from [Mock] plays with no network so the app can be shown anywhere.
 */
@Stable
class Practice(
    private val scope: CoroutineScope,
    private val newSession: () -> LiveSession,
    private val score: suspend (round: String, seconds: Double, turns: List<Turn>) -> LiveReport,
    private val onFinished: (Finished) -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
    private val player: VoicePlayback = VoicePlayback.None,
) {
    var phase by mutableStateOf(Phase.Idle)
        private set
    var round by mutableIntStateOf(0)
    var difficulty by mutableIntStateOf(1)
    var style by mutableIntStateOf(0)
    var minutes by mutableIntStateOf(15)
    var jobPost by mutableStateOf("")
    var role by mutableStateOf(Mock.role)
    var demo by mutableStateOf(false)

    var picking by mutableStateOf(false)
    var reviewing by mutableStateOf(false)

    var turns by mutableStateOf<List<Turn>>(emptyList())
        private set
    var elapsed by mutableLongStateOf(0L)
    var limitMs by mutableLongStateOf(minutes * 60_000L)
        private set
    var interviewerLevel by mutableFloatStateOf(0f)
        private set
    var candidateLevel by mutableFloatStateOf(0f)
        private set
    var muted by mutableStateOf(false)
        private set
    var replaying by mutableStateOf(false)
    var report by mutableStateOf<LiveReport?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var step by mutableStateOf(ConnectStep.Idle)
        private set

    /** Where the interviewer's voice comes from for the replay buttons in the chat. */
    var voice by mutableStateOf<VoiceSource?>(null)
        private set
    /** The turn whose voice is playing, and how far through it is. */
    var speaking by mutableStateOf<Int?>(null)
        private set
    var speakProgress by mutableFloatStateOf(0f)
        private set
    private var speakJob: Job? = null

    private var session: LiveSession? = null
    private var jobs = mutableListOf<Job>()
    private var startedAt = 0L

    val setup get() = InterviewSetup(
        round = roundKeys[round],
        difficulty = difficultyKeys[difficulty],
        style = styleKeys[style],
        role = role.ifBlank { Mock.role },
        minutes = minutes,
        jobPost = jobPost.trim().takeIf { round == 3 && it.isNotEmpty() },
    )

    val canStart get() = round != 3 || jobPost.isNotBlank()

    /** The turn on screen: the latest one while live, the one under the playhead while reviewing. */
    val line: Turn?
        get() = if (phase == Phase.Report || replaying) turns.lastOrNull { it.startMs <= elapsed } ?: turns.firstOrNull() else turns.lastOrNull()

    /** Which question the interviewer is on: every interviewer turn that asks something counts, the greeting is question 1. */
    val question: Int
        get() = turns.filter { it.startMs <= (if (phase == Phase.Live) Long.MAX_VALUE else elapsed) }
            .count { it.speaker == Speaker.interviewer && asksSomething(it.text) }.coerceAtLeast(1)

    val progress: Float
        get() = when (phase) {
            Phase.Live -> (elapsed.toFloat() / limitMs).coerceIn(0f, 1f)
            Phase.Report -> (elapsed.toFloat() / maxOf(1L, totalMs)).coerceIn(0f, 1f)
            else -> 0f
        }

    val totalMs: Long get() = turns.maxOfOrNull { it.endMs } ?: 0L

    val level: Float get() = if (phase == Phase.Live || replaying) interviewerLevel else 0f

    /** During the conversation the sky stays light: morning while the interviewer talks, a soft mist while you answer. */
    val mood: Mood
        get() {
            // same rule as the screen: the chat shows while live and while the report playhead sits mid interview
            val chatting = phase == Phase.Connecting || phase == Phase.Live || phase == Phase.Scoring || replaying ||
                (phase == Phase.Report && elapsed < totalMs)
            if (!chatting) return Mood.Day
            val last = line ?: return Mood.Morning
            val candidateTalking = candidateLevel > 0.12f && !muted
            return when {
                phase == Phase.Live && last.speaker == Speaker.candidate && (candidateTalking || elapsed - last.endMs < 2_500) -> Mood.Mist
                phase == Phase.Report && last.speaker == Speaker.candidate -> Mood.Mist
                else -> Mood.Morning
            }
        }

    fun start() {
        if (phase == Phase.Connecting || phase == Phase.Live) return
        if (!canStart) {
            fail("Paste a job post first, the interviewer builds its questions from it.")
            return
        }
        reset()
        picking = false
        if (demo) startDemo() else startLive()
    }

    private fun reset() {
        cancelJobs()
        stopVoice()
        voice = null
        turns = emptyList()
        elapsed = 0
        report = null
        error = null
        muted = false
        replaying = false
        interviewerLevel = 0f
        candidateLevel = 0f
        limitMs = minutes * 60_000L
    }

    private fun startLive() {
        phase = Phase.Connecting
        val live = newSession()
        session = live
        val transcript = Transcript()
        jobs += scope.launch {
            live.events.collect { e ->
                when (e) {
                    is LiveEvent.Delta -> turns = transcript.add(e)
                    is LiveEvent.Closed -> if (phase == Phase.Live) finish()
                    is LiveEvent.Failed -> if (e.code == "connection_failed") fail("The voice connection dropped. Your answers so far are kept.", keepTurns = true)
                    else -> Unit
                }
            }
        }
        jobs += scope.launch { live.step.collect { step = it } }
        jobs += scope.launch { live.interviewerLevel.collect { interviewerLevel = it } }
        jobs += scope.launch { live.candidateLevel.collect { candidateLevel = it } }
        jobs += scope.launch {
            try {
                val res = live.start(setup)
                // the session makes its tape while starting, before any audio arrives
                voice = live.voice
                limitMs = res.maxSeconds * 1000L
                startedAt = now()
                phase = Phase.Live
                while (phase == Phase.Live) {
                    elapsed = now() - startedAt
                    if (elapsed >= limitMs) {
                        end()
                        break
                    }
                    delay(100)
                }
            } catch (e: CancellationException) {
                // ending or leaving the interview cancels this loop, that is not a failure
                throw e
            } catch (e: WorkerException) {
                live.release()
                fail(if (e.status == 429) "Too many interviews in a row. Give it a minute and try again." else "Hotseat could not start the interview (${e.message}).")
            } catch (e: IOException) {
                live.release()
                fail("Could not reach Hotseat. Check your connection and try again.")
            } catch (e: IllegalStateException) {
                live.release()
                fail("The voice connection could not be set up. Try again.")
            }
        }
    }

    /** Ends a live interview and scores it. Safe to call twice. */
    fun end() {
        when {
            phase == Phase.Live && demo -> finishDemo()
            phase == Phase.Live -> finish()
            phase == Phase.Connecting -> {
                cancelJobs()
                session?.release()
                session = null
                phase = Phase.Idle
            }
        }
    }

    private fun finish() {
        if (phase != Phase.Live) return
        val live = session
        session = null
        stopVoice()
        phase = Phase.Scoring
        val seconds = elapsed / 1000.0
        cancelJobs()
        interviewerLevel = 0f
        candidateLevel = 0f
        jobs += scope.launch {
            live?.stop()
            scoreTurns(seconds)
        }
    }

    fun retry() {
        if (phase == Phase.Failed && turns.any { it.speaker == Speaker.candidate }) {
            phase = Phase.Scoring
            error = null
            jobs += scope.launch { scoreTurns(elapsed / 1000.0) }
        } else {
            start()
        }
    }

    private suspend fun scoreTurns(seconds: Double) {
        if (turns.none { it.speaker == Speaker.candidate && it.text.isNotBlank() }) {
            fail("The interviewer did not hear any answers, so there is nothing to score yet.")
            return
        }
        try {
            val result = score(roundKeys[round], seconds, turns)
            report = result
            elapsed = totalMs
            phase = Phase.Report
            onFinished(Finished(round, seconds, turns, result, voice))
        } catch (e: WorkerException) {
            fail(if (e.status == 429) "Scoring is busy, try again in a minute." else "Scoring failed (${e.message}). Tap the arrow to retry.", keepTurns = true)
        } catch (e: IOException) {
            fail("Could not reach Hotseat to score this. Tap the arrow to retry.", keepTurns = true)
        }
    }

    fun canReplay(index: Int): Boolean = voice?.has(turns, index) == true

    /** Plays the interviewer's own voice for a turn again, tapping the one that is playing stops it. */
    fun replayVoice(index: Int) {
        if (speaking == index) {
            stopVoice()
            return
        }
        stopVoice()
        val clip = voice?.clip(turns, index) ?: return
        val call = phase == Phase.Live
        // keep the interviewer from hearing its own words come back through the mic
        if (call && !muted) session?.setMuted(true)
        player.play(clip, call)
        speaking = index
        speakProgress = 0f
        speakJob = scope.launch {
            while (true) {
                speakProgress = player.position()
                if (speakProgress >= 1f) break
                delay(40)
            }
            // stopping early goes through stopVoice, this is only the clip running out
            speakJob = null
            stopVoice()
        }
    }

    private fun stopVoice() {
        speakJob?.cancel()
        speakJob = null
        if (speaking != null) {
            player.stop()
            if (phase == Phase.Live && !muted) session?.setMuted(false)
        }
        speaking = null
        speakProgress = 0f
    }

    fun micDenied() = fail("Hotseat needs the microphone to interview you. Allow it and tap play again.")

    fun toggleMute() {
        if (phase != Phase.Live || demo) return
        muted = !muted
        session?.setMuted(muted)
    }

    fun togglePlayback() {
        if (phase != Phase.Report) return
        if (replaying) {
            replaying = false
            cancelJobs()
            return
        }
        if (elapsed >= totalMs) elapsed = 0
        replaying = true
        jobs += scope.launch {
            var last = now()
            var clock = 0f
            while (replaying && elapsed < totalMs) {
                delay(50)
                val t = now()
                elapsed += t - last
                clock += (t - last) / 1000f
                last = t
                val talking = line?.let { elapsed < it.endMs } == true
                interviewerLevel = if (talking) 0.45f + 0.55f * abs(sin(clock * 11f) * sin(clock * 3.7f)) else 0f
            }
            interviewerLevel = 0f
            replaying = false
        }
    }

    fun seek(fraction: Float) {
        if (phase != Phase.Report) return
        replaying = false
        cancelJobs()
        elapsed = (fraction * totalMs).toLong().coerceIn(0, totalMs)
    }

    fun openFinished(finished: Finished) {
        reset()
        round = finished.roundIndex
        turns = finished.turns
        report = finished.report
        voice = finished.voice
        elapsed = totalMs
        phase = Phase.Report
    }

    fun backToIdle() {
        reset()
        reviewing = false
        phase = Phase.Idle
    }

    /** Leaves a report or a failed run and opens the round picker for the next interview. */
    fun newInterview() {
        backToIdle()
        picking = true
    }

    fun dispose() {
        cancelJobs()
        stopVoice()
        session?.release()
        session = null
    }

    private fun fail(message: String, keepTurns: Boolean = false) {
        cancelJobs()
        stopVoice()
        session = null
        if (!keepTurns) turns = emptyList()
        interviewerLevel = 0f
        candidateLevel = 0f
        error = message
        phase = Phase.Failed
    }

    private fun cancelJobs() {
        jobs.forEach { it.cancel() }
        jobs = mutableListOf()
    }

    private fun startDemo() {
        val script = demoTurns()
        limitMs = Mock.totalMs
        phase = Phase.Live
        startedAt = now()
        jobs += scope.launch {
            var clock = 0f
            var last = now()
            while (phase == Phase.Live) {
                val t = now()
                elapsed = t - startedAt
                clock += (t - last) / 1000f
                last = t
                turns = script.filter { it.startMs <= elapsed }
                val speaking = turns.lastOrNull()?.takeIf { elapsed < it.endMs + 600 }
                val wave = 0.45f + 0.55f * abs(sin(clock * 11f) * sin(clock * 3.7f))
                interviewerLevel = if (speaking?.speaker == Speaker.interviewer) wave else 0f
                candidateLevel = if (speaking?.speaker == Speaker.candidate) wave * 0.6f else 0f
                if (elapsed >= Mock.totalMs) {
                    finishDemo()
                    break
                }
                delay(50)
            }
        }
    }

    private fun finishDemo() = showDemo(round)

    /** Shows a scripted report, used by demo mode and the sample sessions. */
    fun showDemo(roundIndex: Int) {
        reset()
        round = roundIndex
        turns = demoTurns()
        report = Mock.report(roundIndex)
        elapsed = totalMs
        phase = Phase.Report
    }

    private fun demoTurns() = Mock.script(round).map {
        Turn(if (it.speaker == ScriptSpeaker.You) Speaker.candidate else Speaker.interviewer, it.text, it.at, it.at + it.text.length * 1000L / 14)
    }
}

private val ASKING = Regex("""\b(tell me|walk me|how|what|why|when|which|where|describe|explain|design|give me|talk me)\b""", RegexOption.IGNORE_CASE)

// transcripts rarely end in a question mark, so look for question words too
internal fun asksSomething(text: String) = text.contains('?') || ASKING.containsMatchIn(text)
