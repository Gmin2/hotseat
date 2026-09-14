package dev.mintu.hotseat.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** One row in the Sessions list, real or sample. */
data class SessionRowData(
    val key: String,
    val day: String,
    val date: String,
    val title: String,
    val subtitle: String,
    val score: Int,
    val trend: List<Int>,
)

data class ProgressData(
    val scores: List<Int>,
    val skills: List<Skill>,
    val streakDays: Int,
    val minutes: Int,
    val questions: Int,
    val sample: Boolean,
)

private val dayFormat = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val dateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

fun SavedSession.row(role: String, zone: ZoneId = ZoneId.systemDefault()): SessionRowData {
    val date = Instant.ofEpochMilli(startedAt).atZone(zone)
    val minutes = maxOf(1, (seconds / 60).roundToInt())
    return SessionRowData(
        key = id,
        day = dayFormat.format(date),
        date = dateFormat.format(date),
        title = Mock.rounds.getOrNull(round)?.title ?: "Interview",
        subtitle = "$role · $minutes min",
        score = report.score,
        trend = report.answers.map { it.score }.ifEmpty { listOf(report.score) },
    )
}

fun Mock.sampleRows() = sessions.mapIndexed { i, s ->
    SessionRowData("sample-$i", s.day, s.date, s.round, "${s.role} · ${s.minutes} min", s.score, s.trend)
}

/** Real progress once there is at least one interview, the sample numbers until then. */
fun progress(saved: Saved, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): ProgressData {
    val sessions = saved.sessions
    if (sessions.isEmpty()) {
        return ProgressData(Mock.scores, Mock.skills, Mock.streakDays, Mock.minutesPracticed, Mock.sessions.size * 3, sample = true)
    }
    val oldestFirst = sessions.sortedBy { it.startedAt }
    val answers = sessions.flatMap { it.report.answers }
    val star = listOf("Situation", "Task", "Action", "Result").mapIndexed { i, name ->
        val values = answers.mapNotNull { it.star.getOrNull(i) }
        Skill(name, if (values.isEmpty()) 0 else (values.average() * 100).roundToInt())
    }
    return ProgressData(
        scores = oldestFirst.takeLast(12).map { it.report.score },
        skills = star,
        streakDays = streak(sessions.map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }, today),
        minutes = (sessions.sumOf { it.seconds } / 60).roundToInt(),
        questions = answers.size,
        sample = false,
    )
}

/** Days in a row with at least one interview, counting back from today, or from yesterday if today has none yet. */
fun streak(days: List<LocalDate>, today: LocalDate): Int {
    val set = days.toSet()
    var day = if (today in set) today else today.minusDays(1)
    var count = 0
    while (day in set) {
        count++
        day = day.minusDays(1)
    }
    return count
}
