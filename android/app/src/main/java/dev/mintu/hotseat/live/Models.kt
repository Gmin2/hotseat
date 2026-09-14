package dev.mintu.hotseat.live

import kotlinx.serialization.Serializable

/** What the worker needs to shape the interviewer. Keys match worker/src/prompt.ts. */
@Serializable
data class InterviewSetup(
    val round: String = "behavioral",
    val difficulty: String = "medium",
    val style: String = "friendly",
    val role: String = "Android engineer",
    val minutes: Int = 15,
    val jobPost: String? = null,
)

@Serializable
data class SessionRequest(
    val sdp: String,
    val round: String,
    val difficulty: String,
    val style: String,
    val role: String,
    val minutes: Int,
    val jobPost: String? = null,
)

@Serializable
data class SessionResponse(val id: String, val sdp: String, val greeting: String, val maxSeconds: Int)

enum class Speaker { interviewer, candidate }

@Serializable
data class Turn(val speaker: Speaker, val text: String, val startMs: Long, val endMs: Long)

@Serializable
data class ReportRequest(val round: String, val seconds: Double, val transcript: List<Turn>)

@Serializable
data class ReportAnswer(val question: String, val score: Int, val star: List<Float>, val note: String, val better: String)

@Serializable
data class LiveReport(
    val score: Int,
    val summary: String,
    val duration: String,
    val wpm: Int,
    val fillers: Int,
    val answers: List<ReportAnswer>,
)

@Serializable
data class WorkerError(val error: String)
