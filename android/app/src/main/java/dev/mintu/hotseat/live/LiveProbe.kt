package dev.mintu.hotseat.live

import android.content.Context
import android.provider.Settings
import android.util.Log
import dev.mintu.hotseat.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debug only. Runs one real interview with no UI and logs everything under the HotseatProbe tag, so a session on a
 * real phone can be checked from adb:
 * adb shell am start -n dev.mintu.hotseat/.MainActivity --ei probe 25 --es round technical
 */
object LiveProbe {
    private const val TAG = "HotseatProbe"

    fun api(context: Context) = WorkerApi(
        BuildConfig.WORKER_URL,
        BuildConfig.APP_KEY,
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown",
    )

    fun run(context: Context, scope: CoroutineScope, seconds: Int, round: String) = scope.launch {
        val client = LiveClient(context, api(context), scope)
        val transcript = Transcript()
        var peakInterviewer = 0f
        var peakCandidate = 0f
        val listen = launch {
            client.events.collect { e ->
                when (e) {
                    is LiveEvent.Delta -> transcript.add(e)
                    else -> Log.i(TAG, "event $e")
                }
            }
        }
        val levels = launch {
            while (true) {
                peakInterviewer = maxOf(peakInterviewer, client.interviewerLevel.value)
                peakCandidate = maxOf(peakCandidate, client.candidateLevel.value)
                delay(50)
            }
        }
        try {
            Log.i(TAG, "worker ${BuildConfig.WORKER_URL}")
            val session = client.start(InterviewSetup(round = round, minutes = 10))
            Log.i(TAG, "session ${session.id} maxSeconds=${session.maxSeconds}")
            delay(seconds * 1000L)
            val closed = client.stop()
            Log.i(TAG, "closed $closed")
        } catch (e: Exception) {
            Log.e(TAG, "failed ${e.javaClass.simpleName}: ${e.message}", e)
            client.release()
        }
        listen.cancel()
        levels.cancel()
        Log.i(TAG, "peak interviewer level=$peakInterviewer candidate level=$peakCandidate")
        transcript.all.forEach { Log.i(TAG, "turn ${it.speaker} ${it.startMs}ms: ${it.text.trim()}") }
        Log.i(TAG, "done turns=${transcript.all.size}")
    }
}
