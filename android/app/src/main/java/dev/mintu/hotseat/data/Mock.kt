package dev.mintu.hotseat.data

import androidx.annotation.DrawableRes
import dev.mintu.hotseat.R
import dev.mintu.hotseat.ui.icons.InkIcon
import dev.mintu.hotseat.ui.icons.InkIcons

data class Round(
    val title: String,
    val blurb: String,
    val icon: InkIcon,
    @DrawableRes val art: Int,
)

enum class Speaker { Interviewer, You }

// one line of the scripted mock interview, at is when it starts in the session
data class Line(val speaker: Speaker, val text: String, val at: Long, val question: Int, val pushback: Boolean = false)

data class Answer(
    val question: String,
    val score: Int,
    val star: List<Float>,
    val note: String,
    val better: String,
)

data class Report(
    val score: Int,
    val summary: String,
    val duration: String,
    val wpm: Int,
    val fillers: Int,
    val answers: List<Answer>,
)

data class Session(
    val day: String,
    val date: String,
    val round: String,
    val role: String,
    val score: Int,
    val minutes: Int,
    val trend: List<Int>,
)

data class Skill(val name: String, val score: Int)

object Mock {
    const val role = "Android engineer"

    val rounds = listOf(
        Round("Behavioral", "Stories about you, told with STAR", InkIcons.Behavioral, R.drawable.illo_behavioral),
        Round("Android technical", "Kotlin, Compose, lifecycles, testing", InkIcons.Technical, R.drawable.illo_technical),
        Round("System design", "Design an app end to end, out loud", InkIcons.SystemDesign, R.drawable.illo_system),
        Round("From a job post", "Paste a role, get its questions", InkIcons.JobDescription, R.drawable.illo_job),
    )

    val difficulties = listOf("Easy", "Medium", "Hard")
    val styles = listOf("Friendly", "Sharp")

    const val totalMs = 96_000L
    const val questions = 4

    private val behavioral = listOf(
        Line(Speaker.Interviewer, "Hey, thanks for jumping on. Tell me about a project you are really proud of.", 0, 1),
        Line(Speaker.You, "At ecashlabs I built the mobile wallet in Flutter, from the first screen to the store release.", 7_000, 1),
        Line(Speaker.Interviewer, "Nice. Which part of that was actually yours, not the team's?", 17_000, 1, pushback = true),
        Line(Speaker.You, "The offline sync. I designed the queue that replays payments when the network comes back.", 23_000, 1),
        Line(Speaker.Interviewer, "Walk me through a time you disagreed with a product decision.", 34_000, 2),
        Line(Speaker.You, "Product wanted a seed phrase screen on first launch, I pushed for a delayed backup flow and showed drop off data.", 40_000, 2),
        Line(Speaker.Interviewer, "How did you measure that it worked?", 52_000, 3),
        Line(Speaker.You, "Onboarding completion went from sixty one to eighty percent in two weeks, and backups still landed within a day.", 57_000, 3),
        Line(Speaker.Interviewer, "Last one. What would you do differently if you rebuilt it natively?", 70_000, 4),
        Line(Speaker.You, "I would lean on Compose and WorkManager for sync, and keep the crypto in a shared Rust core.", 76_000, 4),
        Line(Speaker.Interviewer, "Great, that is all from me. Your report is ready.", 88_000, 4),
    )

    private val technical = listOf(
        Line(Speaker.Interviewer, "Let's start simple. What actually happens to a ViewModel on rotation?", 0, 1),
        Line(Speaker.You, "It survives. The activity is recreated but the ViewModelStore is kept, so state stays put.", 7_000, 1),
        Line(Speaker.Interviewer, "And when the process dies in the background?", 17_000, 1, pushback = true),
        Line(Speaker.You, "Then it is gone, so anything important goes through SavedStateHandle or gets reloaded.", 23_000, 1),
        Line(Speaker.Interviewer, "How would you stop a Compose list from recomposing every row on each update?", 34_000, 2),
        Line(Speaker.You, "Stable keys in LazyColumn, immutable item models, and derivedStateOf for anything computed from scroll.", 40_000, 2),
        Line(Speaker.Interviewer, "How do you test a flow that emits over time?", 52_000, 3),
        Line(Speaker.You, "runTest with a test dispatcher, then Turbine to await each emission and assert in order.", 57_000, 3),
        Line(Speaker.Interviewer, "Last one. Where does WorkManager fit and where does it not?", 70_000, 4),
        Line(Speaker.You, "Deferrable work that must finish, like syncing. Not for exact timing or anything user facing right now.", 76_000, 4),
        Line(Speaker.Interviewer, "Solid answers. Your report is ready.", 88_000, 4),
    )

    private val design = listOf(
        Line(Speaker.Interviewer, "Design the offline first chat screen of a messaging app. Where do you start?", 0, 1),
        Line(Speaker.You, "With the source of truth. A local database the UI observes, and the network only ever writes into it.", 8_000, 1),
        Line(Speaker.Interviewer, "What happens to a message sent with no signal?", 18_000, 1, pushback = true),
        Line(Speaker.You, "It lands in an outbox table as pending, WorkManager retries it with backoff once we are online.", 24_000, 1),
        Line(Speaker.Interviewer, "How do you keep ordering right when messages arrive late?", 35_000, 2),
        Line(Speaker.You, "Server timestamps plus a per chat sequence number, and the list sorts by sequence, not arrival.", 41_000, 2),
        Line(Speaker.Interviewer, "How would you page a chat with ten thousand messages?", 53_000, 3),
        Line(Speaker.You, "Paging 3 with a RemoteMediator, keyed by sequence so scrolling up fetches older pages.", 58_000, 3),
        Line(Speaker.Interviewer, "What would you measure after launch?", 71_000, 4),
        Line(Speaker.You, "Send success rate, time to first message on open, and how often the outbox has stuck items.", 77_000, 4),
        Line(Speaker.Interviewer, "Nice structure. Your report is ready.", 88_000, 4),
    )

    fun script(round: Int) = when (round) {
        1 -> technical
        2 -> design
        else -> behavioral
    }

    private val behavioralReport = Report(
        score = 82,
        summary = "Strong ownership stories. Land the result before you move on.",
        duration = "15:42",
        wpm = 142,
        fillers = 9,
        answers = listOf(
            Answer(
                "A project you are proud of",
                78,
                listOf(0.9f, 0.7f, 0.85f, 0.5f),
                "Clear situation and action, the result came only after a nudge.",
                "Open with the offline sync you owned, then say what it saved users: no lost payments in six months.",
            ),
            Answer(
                "Disagreeing with product",
                86,
                listOf(0.8f, 0.9f, 0.9f, 0.8f),
                "Good use of data to disagree without drama.",
                "Name the tradeoff you gave up, it shows judgement, not just persistence.",
            ),
            Answer(
                "Measuring that it worked",
                90,
                listOf(0.7f, 0.8f, 0.9f, 1f),
                "Concrete numbers and a clear window of time.",
                "Add how you would have noticed if it failed, what alert or metric.",
            ),
            Answer(
                "Rebuilding it natively",
                74,
                listOf(0.6f, 0.6f, 0.8f, 0.6f),
                "Good stack picks, thin on why.",
                "Tie each choice to a pain you hit in Flutter, like background sync limits.",
            ),
        ),
    )

    private val technicalReport = Report(
        score = 79,
        summary = "You know the platform. Say the tradeoff out loud, not just the API.",
        duration = "14:10",
        wpm = 151,
        fillers = 6,
        answers = listOf(
            Answer("ViewModel on rotation and process death", 84, listOf(0.9f, 0.8f, 0.85f, 0.7f), "Correct on both cases, the pushback did not rattle you.", "Mention what the user sees after process death, a reload or a restored screen."),
            Answer("Stopping extra recomposition", 81, listOf(0.8f, 0.8f, 0.9f, 0.7f), "Right tools, stable keys and derivedStateOf.", "Say how you would prove it, layout inspector recomposition counts."),
            Answer("Testing flows over time", 76, listOf(0.7f, 0.7f, 0.8f, 0.6f), "Named runTest and Turbine but skipped the dispatcher detail.", "Explain why a test dispatcher matters, delays run instantly and deterministically."),
            Answer("Where WorkManager fits", 74, listOf(0.6f, 0.7f, 0.8f, 0.6f), "Good split between deferrable and immediate work.", "Give a counter example, a timer or an alarm, and what you would use instead."),
        ),
    )

    private val designReport = Report(
        score = 71,
        summary = "Clear source of truth. Go deeper on failure cases before scale.",
        duration = "24:36",
        wpm = 138,
        fillers = 11,
        answers = listOf(
            Answer("Offline first chat, where to start", 82, listOf(0.9f, 0.8f, 0.8f, 0.7f), "Leading with the local database set up everything after it.", "Draw the data flow in one sentence before naming libraries."),
            Answer("Ordering late messages", 70, listOf(0.7f, 0.6f, 0.8f, 0.6f), "Sequence numbers were right, conflicts were not covered.", "Say what happens when two devices send at the same moment."),
            Answer("Paging a long chat", 66, listOf(0.6f, 0.6f, 0.7f, 0.5f), "Named Paging 3 but not how the cache is kept small.", "Add a limit on stored pages and when old ones get evicted."),
            Answer("What to measure after launch", 68, listOf(0.6f, 0.7f, 0.7f, 0.6f), "Useful metrics, no targets.", "Put a number on each one, like send success above 99.5 percent."),
        ),
    )

    fun report(round: Int) = when (round) {
        1 -> technicalReport
        2 -> designReport
        else -> behavioralReport
    }

    val sessions = listOf(
        Session("Mon", "Sep 14", "Behavioral", role, 82, 16, listOf(62, 70, 78, 74, 86, 90, 74)),
        Session("Sat", "Sep 12", "Android technical", role, 76, 22, listOf(50, 64, 80, 72, 76, 81, 70)),
        Session("Thu", "Sep 10", "System design", role, 69, 28, listOf(40, 58, 66, 72, 70, 74, 69)),
        Session("Tue", "Sep 8", "Behavioral", role, 74, 14, listOf(60, 66, 71, 78, 70, 80, 74)),
        Session("Sun", "Sep 6", "From a job post", "Mobile engineer, fintech", 71, 18, listOf(55, 62, 70, 68, 75, 72, 71)),
        Session("Fri", "Sep 4", "Android technical", role, 63, 20, listOf(40, 52, 60, 66, 58, 70, 63)),
    )

    val scores = listOf(58, 61, 63, 60, 67, 71, 69, 74, 72, 76, 78, 82)

    val skills = listOf(
        Skill("Structure", 78),
        Skill("Clarity", 84),
        Skill("Pace", 71),
        Skill("Confidence", 88),
    )

    const val streakDays = 5
    const val minutesPracticed = 142
}
