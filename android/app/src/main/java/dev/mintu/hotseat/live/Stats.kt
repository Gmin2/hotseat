package dev.mintu.hotseat.live

// same counting as worker/src/stats.ts so the live caption matches the report
private val FILLERS = Regex("""\b(um+|uh+|erm|you know|basically|kind of|sort of|i mean)\b|\blike,""", RegexOption.IGNORE_CASE)

data class Pace(val words: Int, val wpm: Int, val fillers: Int)

fun candidatePace(turns: List<Turn>): Pace {
    val mine = turns.filter { it.speaker == Speaker.candidate }
    val words = mine.sumOf { t -> t.text.trim().split(Regex("""\s+""")).count { it.isNotEmpty() } }
    val speakingMs = mine.sumOf { maxOf(0L, it.endMs - it.startMs) }
    val fillers = mine.sumOf { FILLERS.findAll(it.text).count() }
    val wpm = if (speakingMs > 5_000) Math.round(words / (speakingMs / 60_000.0)).toInt() else 0
    return Pace(words, wpm, fillers)
}

fun clock(ms: Long): String {
    val s = maxOf(0L, ms) / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
