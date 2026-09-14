package dev.mintu.hotseat.data

import dev.mintu.hotseat.live.LiveReport
import dev.mintu.hotseat.live.ReportAnswer
import dev.mintu.hotseat.live.Speaker
import dev.mintu.hotseat.live.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class StoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun session(id: String, score: Int, day: LocalDate, star: List<Float> = listOf(1f, 0.5f, 0.5f, 0f)) = SavedSession(
        id = id,
        round = 1,
        startedAt = ZonedDateTime.of(day.atTime(10, 0), ZoneOffset.UTC).toInstant().toEpochMilli(),
        seconds = 600.0,
        turns = listOf(Turn(Speaker.interviewer, "Tell me about it?", 0, 1000), Turn(Speaker.candidate, "Sure", 2000, 3000)),
        report = LiveReport(score, "ok", "10:00", 140, 2, listOf(ReportAnswer("Q", score, star, "n", "b"))),
    )

    @Test
    fun savesAndReloadsFromDisk() {
        val file = File(folder.root, "hotseat.json")
        val store = Store(file)
        assertFalse(store.profile.onboarded)
        store.updateProfile { it.copy(name = "Mintu", role = "Mobile engineer", onboarded = true, minutes = 20) }
        store.addSession(session("a", 70, LocalDate.of(2026, 9, 13)))
        store.addSession(session("b", 80, LocalDate.of(2026, 9, 14)))

        val again = Store(file).saved.value
        assertEquals("Mintu", again.profile.name)
        assertEquals(20, again.profile.minutes)
        assertTrue(again.profile.onboarded)
        assertEquals(listOf("b", "a"), again.sessions.map { it.id })
        assertEquals("Sure", again.sessions.last().turns.last().text)
        assertFalse(File(folder.root, "hotseat.json.tmp").exists())
    }

    @Test
    fun deleteAllWipesTheFileAndResets() {
        val file = File(folder.root, "hotseat.json")
        val store = Store(file)
        store.updateProfile { it.copy(name = "Mintu", onboarded = true) }
        store.addSession(session("a", 70, LocalDate.of(2026, 9, 13)))
        assertTrue(file.exists())

        store.deleteAll()
        assertFalse(file.exists())
        assertEquals(Saved(), store.saved.value)
        assertEquals(Saved(), Store(file).saved.value)
    }

    @Test
    fun brokenFileStartsFresh() {
        val file = File(folder.root, "hotseat.json").apply { writeText("{ not json") }
        assertEquals(Saved(), Store(file).saved.value)
    }

    @Test
    fun deletingOneSessionKeepsTheRest() {
        val store = Store(File(folder.root, "hotseat.json"))
        store.addSession(session("a", 70, LocalDate.of(2026, 9, 13)))
        store.addSession(session("b", 80, LocalDate.of(2026, 9, 14)))
        store.deleteSession("a")
        assertEquals(listOf("b"), store.saved.value.sessions.map { it.id })
    }

    @Test
    fun progressUsesRealInterviewsOnceThereAreAny() {
        assertTrue(progress(Saved()).sample)

        val today = LocalDate.of(2026, 9, 14)
        val saved = Saved(sessions = listOf(
            session("c", 82, today, listOf(1f, 1f, 1f, 1f)),
            session("b", 74, today.minusDays(1), listOf(0f, 0f, 0f, 0f)),
            session("a", 60, today.minusDays(3)),
        ))
        val data = progress(saved, today, ZoneOffset.UTC)
        assertFalse(data.sample)
        assertEquals(listOf(60, 74, 82), data.scores)
        assertEquals(2, data.streakDays)
        assertEquals(30, data.minutes)
        assertEquals(3, data.questions)
        assertEquals(listOf("Situation", "Task", "Action", "Result"), data.skills.map { it.name })
        assertEquals(67, data.skills[0].score)
        assertEquals(33, data.skills[3].score)
    }

    @Test
    fun streakCountsBackFromYesterdayWhenTodayIsEmpty() {
        val today = LocalDate.of(2026, 9, 14)
        assertEquals(0, streak(emptyList(), today))
        assertEquals(1, streak(listOf(today.minusDays(1)), today))
        assertEquals(0, streak(listOf(today.minusDays(2)), today))
        assertEquals(3, streak(listOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4)), today))
    }

    @Test
    fun rowsShowDayDateAndAnswerTrend() {
        val row = session("a", 70, LocalDate.of(2026, 9, 14)).row("Android engineer", ZoneOffset.UTC)
        assertEquals("Mon", row.day)
        assertEquals("Sep 14", row.date)
        assertEquals("Android technical", row.title)
        assertEquals("Android engineer · 10 min", row.subtitle)
        assertEquals(listOf(70), row.trend)
    }
}
