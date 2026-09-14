package dev.mintu.hotseat.live

/**
 * Builds whole turns out of transcript deltas. Deltas from the same speaker that arrive close together join one turn,
 * a different speaker or a long gap starts a new one. Same rule as worker/scripts/interview.mjs.
 */
class Transcript(private val joinGapMs: Long = 1500) {
    private val turns = mutableListOf<Turn>()

    val all: List<Turn> get() = turns.toList()

    fun add(delta: LiveEvent.Delta): List<Turn> {
        val last = turns.lastOrNull()
        if (last != null && last.speaker == delta.speaker && delta.startMs - last.endMs < joinGapMs) {
            turns[turns.lastIndex] = last.copy(text = last.text + delta.text, endMs = maxOf(last.endMs, delta.endMs))
        } else {
            turns += Turn(delta.speaker, delta.text, delta.startMs, delta.endMs)
        }
        return all
    }

    /** The turn being spoken right now, trimmed for display. */
    val current: Turn? get() = turns.lastOrNull()?.let { it.copy(text = it.text.trim()) }

    fun interviewerQuestions() = turns.count { it.speaker == Speaker.interviewer && it.text.trim().endsWith("?") }
}
