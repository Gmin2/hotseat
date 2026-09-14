package dev.mintu.hotseat.ui.practice

import dev.mintu.hotseat.data.Mock
import dev.mintu.hotseat.live.ConnectStep
import dev.mintu.hotseat.live.InterviewSetup
import dev.mintu.hotseat.live.LiveEvent
import dev.mintu.hotseat.live.LiveReport
import dev.mintu.hotseat.live.LiveSession
import dev.mintu.hotseat.live.ReportAnswer
import dev.mintu.hotseat.live.SessionResponse
import dev.mintu.hotseat.live.Speaker
import dev.mintu.hotseat.live.Turn
import dev.mintu.hotseat.live.WorkerException
import dev.mintu.hotseat.ui.components.Mood
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeTest {
    private open class FakeSession(var failWith: Exception? = null, val maxSeconds: Int = 60) : LiveSession {
        override val events = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 64)
        override val step = MutableStateFlow(ConnectStep.Idle)
        override val interviewerLevel = MutableStateFlow(0f)
        override val candidateLevel = MutableStateFlow(0f)
        var setup: InterviewSetup? = null
        var stopped = false
        var released = false
        var micMuted = false
        override suspend fun start(setup: InterviewSetup): SessionResponse {
            this.setup = setup
            failWith?.let { throw it }
            return SessionResponse("live_1", "answer", "Speak first", maxSeconds)
        }
        override suspend fun stop(): LiveEvent.Closed? { stopped = true; return LiveEvent.Closed("close_requested", 10.0) }
        override fun setMuted(muted: Boolean) { micMuted = muted }
        override fun release() { released = true }
    }

    private val report = LiveReport(80, "Good", "00:30", 140, 2, listOf(ReportAnswer("Q", 80, listOf(1f, 1f, 1f, 1f), "n", "b")))

    private class Harness(val practice: Practice, val sessions: MutableList<FakeSession>, val scored: MutableList<Pair<String, List<Turn>>>, val finished: MutableList<Finished>)

    private fun TestScope.harness(
        session: () -> FakeSession = { FakeSession() },
        score: suspend (String, Double, List<Turn>) -> LiveReport = { _, _, _ -> report },
    ): Harness {
        val sessions = mutableListOf<FakeSession>()
        val scored = mutableListOf<Pair<String, List<Turn>>>()
        val finished = mutableListOf<Finished>()
        val practice = Practice(
            scope = backgroundScope,
            newSession = { session().also { sessions += it } },
            score = { r, s, t -> scored += r to t; score(r, s, t) },
            onFinished = { finished += it },
            now = { testScheduler.currentTime },
        )
        return Harness(practice, sessions, scored, finished)
    }

    private suspend fun FakeSession.say(speaker: Speaker, text: String, start: Long, end: Long) =
        events.emit(LiveEvent.Delta(speaker, text, start, end))

    @Test
    fun liveInterviewGoesFromConnectToReport() = runTest {
        val h = harness()
        val p = h.practice
        p.round = 1
        p.difficulty = 2
        p.style = 1
        p.start()
        assertEquals(Phase.Connecting, p.phase)
        runCurrent()
        assertEquals(Phase.Live, p.phase)
        assertEquals(Mood.Morning, p.mood)
        val s = h.sessions.single()
        s.step.value = ConnectStep.Joining
        runCurrent()
        assertEquals(ConnectStep.Joining, p.step)
        assertEquals(InterviewSetup("technical", "hard", "sharp", Mock.role, 15, null), s.setup)
        assertEquals(60_000L, p.limitMs)

        s.say(Speaker.interviewer, "Hi, tell me about ", 0, 900)
        s.say(Speaker.interviewer, "your work?", 950, 1400)
        s.interviewerLevel.value = 0.8f
        runCurrent()
        assertEquals(1, p.turns.size)
        assertEquals("Hi, tell me about your work?", p.line!!.text)
        assertEquals(0.8f, p.level, 0f)

        s.say(Speaker.candidate, "Um, I built a wallet, like, with offline sync and a queue for payments", 3000, 9000)
        s.candidateLevel.value = 0.4f
        advanceTimeBy(9_100)
        runCurrent()
        assertEquals(Mood.Mist, p.mood)
        assertEquals(2, p.turns.size)

        p.end()
        assertEquals(Phase.Scoring, p.phase)
        runCurrent()
        assertTrue(s.stopped)
        assertEquals(Phase.Report, p.phase)
        assertEquals(report, p.report)
        assertEquals("technical", h.scored.single().first)
        assertEquals(2, h.scored.single().second.size)
        assertEquals(1, h.finished.size)
        assertEquals(0f, p.level, 0f)
        assertEquals(p.totalMs, p.elapsed)
        assertEquals(Mood.Day, p.mood)
    }

    @Test
    fun hitsTheTimeCap() = runTest {
        val h = harness(session = { FakeSession(maxSeconds = 30) })
        val p = h.practice
        p.start()
        runCurrent()
        h.sessions.single().say(Speaker.candidate, "an answer", 1000, 2000)
        advanceTimeBy(29_000)
        runCurrent()
        assertEquals(Phase.Live, p.phase)
        advanceTimeBy(1_200)
        runCurrent()
        assertEquals(Phase.Report, p.phase)
        assertTrue(h.sessions.single().stopped)
    }

    @Test
    fun serverClosingTheSessionScoresIt() = runTest {
        val h = harness()
        val p = h.practice
        p.start()
        runCurrent()
        val s = h.sessions.single()
        s.say(Speaker.candidate, "an answer", 1000, 2000)
        s.events.emit(LiveEvent.Closed("expired", 3600.0))
        runCurrent()
        assertEquals(Phase.Report, p.phase)
    }

    @Test
    fun tooManySessionsShowsAFriendlyError() = runTest {
        val h = harness(session = { FakeSession(failWith = WorkerException(429, "too many sessions")) })
        h.practice.start()
        runCurrent()
        assertEquals(Phase.Failed, h.practice.phase)
        assertTrue(h.practice.error!!.contains("Give it a minute"))
        assertTrue(h.sessions.single().released)
    }

    @Test
    fun unreachableWorker() = runTest {
        val h = harness(session = { FakeSession(failWith = IOException("no route")) })
        h.practice.start()
        runCurrent()
        assertEquals(Phase.Failed, h.practice.phase)
        assertTrue(h.practice.error!!.startsWith("Could not reach Hotseat"))
        h.practice.retry()
        runCurrent()
        assertEquals(2, h.sessions.size)
    }

    @Test
    fun scoringFailureKeepsTheAnswersAndRetries() = runTest {
        var attempts = 0
        val h = harness(score = { _, _, _ -> if (attempts++ == 0) throw WorkerException(502, "report failed") else report })
        val p = h.practice
        p.start()
        runCurrent()
        h.sessions.single().say(Speaker.candidate, "an answer", 1000, 2000)
        runCurrent()
        p.end()
        runCurrent()
        assertEquals(Phase.Failed, p.phase)
        assertEquals(1, p.turns.size)
        p.retry()
        runCurrent()
        assertEquals(Phase.Report, p.phase)
        assertEquals(1, h.sessions.size)
        assertEquals(2, h.scored.size)
    }

    @Test
    fun silenceIsNotScored() = runTest {
        val h = harness()
        val p = h.practice
        p.start()
        runCurrent()
        h.sessions.single().say(Speaker.interviewer, "Hello?", 0, 1000)
        runCurrent()
        p.end()
        runCurrent()
        assertEquals(Phase.Failed, p.phase)
        assertTrue(p.error!!.contains("did not hear any answers"))
        assertTrue(h.scored.isEmpty())
    }

    @Test
    fun jobRoundNeedsAPost() = runTest {
        val h = harness()
        val p = h.practice
        p.round = 3
        assertFalse(p.canStart)
        p.start()
        assertEquals(Phase.Failed, p.phase)
        assertTrue(h.sessions.isEmpty())
        p.jobPost = "  Kotlin payments team  "
        p.start()
        runCurrent()
        assertEquals("Kotlin payments team", h.sessions.single().setup!!.jobPost)
    }

    @Test
    fun muteReachesTheMic() = runTest {
        val h = harness()
        val p = h.practice
        p.start()
        runCurrent()
        p.toggleMute()
        assertTrue(p.muted)
        assertTrue(h.sessions.single().micMuted)
        p.toggleMute()
        assertFalse(h.sessions.single().micMuted)
    }

    @Test
    fun cancellingWhileConnectingGoesBackToIdle() = runTest {
        val h = harness()
        val p = h.practice
        p.start()
        p.end()
        assertEquals(Phase.Idle, p.phase)
        assertTrue(h.sessions.single().released)
    }

    @Test
    fun demoPlaysTheScriptWithoutANetwork() = runTest {
        val h = harness()
        val p = h.practice
        p.demo = true
        p.round = 2
        p.start()
        assertEquals(Phase.Live, p.phase)
        advanceTimeBy(9_000)
        runCurrent()
        assertTrue(p.turns.size >= 2)
        assertTrue(p.interviewerLevel >= 0f)
        p.end()
        assertEquals(Phase.Report, p.phase)
        assertEquals(Mock.report(2), p.report)
        assertTrue(h.sessions.isEmpty())
        assertTrue(h.scored.isEmpty())

        p.seek(0f)
        assertEquals(Speaker.interviewer, p.line!!.speaker)
        p.togglePlayback()
        assertTrue(p.replaying)
        advanceTimeBy(500)
        runCurrent()
        assertTrue(p.elapsed > 0)
        p.backToIdle()
        assertEquals(Phase.Idle, p.phase)
        assertNull(p.report)
        assertNotNull(p.setup)
    }
}
