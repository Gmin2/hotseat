package dev.mintu.hotseat.live

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** The GPT-Live data channel events the app cares about. Everything else is [Other]. */
sealed interface LiveEvent {
    data class Started(val id: String) : LiveEvent
    data class Delta(val speaker: Speaker, val text: String, val startMs: Long, val endMs: Long) : LiveEvent
    data class Usage(val seconds: Double) : LiveEvent
    data class Closed(val reason: String, val seconds: Double) : LiveEvent
    data class Failed(val code: String, val message: String) : LiveEvent
    data class Other(val type: String) : LiveEvent
}

private val json = Json { ignoreUnknownKeys = true }

fun parseEvent(text: String): LiveEvent {
    val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrElse { return LiveEvent.Other("unparseable") }
    val type = obj.string("type") ?: return LiveEvent.Other("untyped")
    return when (type) {
        "session.started" -> LiveEvent.Started(obj["session"]?.jsonObject?.string("id").orEmpty())
        "session.output_transcript.delta", "session.input_transcript.delta" -> LiveEvent.Delta(
            if (type.startsWith("session.output")) Speaker.interviewer else Speaker.candidate,
            obj.string("delta").orEmpty(),
            obj["start_ms"]?.jsonPrimitive?.longOrNull ?: 0,
            obj["end_ms"]?.jsonPrimitive?.longOrNull ?: 0,
        )
        "session.usage.updated" -> LiveEvent.Usage(obj["usage"]?.jsonObject?.get("seconds")?.jsonPrimitive?.doubleOrNull ?: 0.0)
        "session.closed" -> LiveEvent.Closed(
            obj.string("reason").orEmpty(),
            obj["usage"]?.jsonObject?.get("seconds")?.jsonPrimitive?.doubleOrNull ?: 0.0,
        )
        "error" -> obj["error"]?.jsonObject.let { LiveEvent.Failed(it?.string("code").orEmpty(), it?.string("message").orEmpty()) }
        else -> LiveEvent.Other(type)
    }
}

private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.takeIf { it.isString }?.content
