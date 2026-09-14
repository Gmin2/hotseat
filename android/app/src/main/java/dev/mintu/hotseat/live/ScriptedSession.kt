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
    override val interviewerLevel = MutableStateFlow(0f)
    override val candidateLevel = MutableStateFlow(0f)
    private var job: Job? = null

    override suspend fun start(setup: InterviewSetup): SessionResponse {
        val lines = context.assets.open("fixtures/technical-session.events.jsonl").bufferedReader().readLines()
        val parsed = lines.filter { it.isNotBlank() }.map(::parseEvent)
        job = scope.launch {
            delay(600)
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

    override fun release() {
        job?.cancel()
    }
}
