package dev.mintu.hotseat.live

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Debug only. Plays a recorded GPT-Live session back through the live path, so everything after the voice
 * (transcript, levels, ending, scoring through the worker, saving) can be exercised without speaking.
 * adb shell am start -n dev.mintu.hotseat/.MainActivity --ez scripted true
 */
class ScriptedSession(private val context: Context, private val scope: CoroutineScope) : LiveSession {
    override val events = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 256)
    override val step = MutableStateFlow(ConnectStep.Idle)
    override val interviewerLevel = MutableStateFlow(0f)
    override val candidateLevel = MutableStateFlow(0f)
    private var job: Job? = null
    private var tape: VoiceTape? = null
    override val voice: VoiceSource? get() = tape

    override suspend fun start(setup: InterviewSetup): SessionResponse {
        val lines = context.assets.open("fixtures/technical-session.events.jsonl").bufferedReader().readLines()
        val parsed = lines.filter { it.isNotBlank() }.map(::parseEvent)
        tape = toneTape(parsed.filterIsInstance<LiveEvent.Delta>())
        job = scope.launch {
            step.value = ConnectStep.Calling
            delay(300)
            step.value = ConnectStep.Joining
            delay(300)
            step.value = ConnectStep.Ready
            var clock = 0L
            for (e in parsed) {
                when (e) {
                    is LiveEvent.Delta -> {
                        if (e.endMs > clock) delay(e.endMs - clock)
                        clock = maxOf(clock, e.endMs)
                        val level = if (e.speaker == Speaker.interviewer) interviewerLevel else candidateLevel
                        level.value = 0.6f
                        events.emit(e)
                        scope.launch {
                            delay(250)
                            level.value = 0f
                        }
                    }
                    is LiveEvent.Closed -> Unit
                    else -> events.emit(e)
                }
            }
        }
        return SessionResponse("scripted", "", "", 600)
    }

    override suspend fun stop(): LiveEvent.Closed {
        job?.cancel()
        return LiveEvent.Closed("close_requested", 0.0)
    }

    override fun setMuted(muted: Boolean) = Unit

    // there is no recorded voice for the fixture, a soft hum where the interviewer talks stands in for it
    private fun toneTape(deltas: List<LiveEvent.Delta>): VoiceTape {
        val turns = deltas.fold(Transcript()) { t, d -> t.also { it.add(d) } }.all.filter { it.speaker == Speaker.interviewer }
        val rate = VoiceTape.RATE
        val total = ((turns.maxOfOrNull { it.endMs } ?: 0L) + 2_000L) * rate / 1000
        val pcm = ShortArray(total.toInt())
        turns.forEach { turn ->
            val from = (turn.startMs * rate / 1000).toInt()
            val to = minOf(pcm.size, (turn.endMs * rate / 1000).toInt())
            for (i in from until to) {
                val t = (i - from).toDouble() / rate
                val envelope = minOf(1.0, t * 20, (to - i).toDouble() / rate * 20) * (0.6 + 0.4 * kotlin.math.sin(t * 9))
                pcm[i] = (envelope * 3000 * kotlin.math.sin(2 * Math.PI * 262 * t)).toInt().toShort()
            }
        }
        return VoiceTape(java.io.File(context.cacheDir, "scripted.pcm"), now = { 0L }).also { it.write(pcm, rate) }
    }

    override fun release() {
        job?.cancel()
    }
}
