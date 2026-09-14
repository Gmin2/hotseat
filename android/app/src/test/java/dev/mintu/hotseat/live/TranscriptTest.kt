package dev.mintu.hotseat.live

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptTest {
    private fun fixture(name: String) = javaClass.classLoader!!.getResource("fixtures/$name")!!.readText()

    @Test
    fun parsesEveryEventInARealSession() {
        val events = fixture("technical-session.events.jsonl").lines().filter { it.isNotBlank() }.map(::parseEvent)
        assertTrue(events.first() is LiveEvent.Started)
        assertEquals("live_u7_EO4G8w7MOX5ul6BVPNfRE", (events.first() as LiveEvent.Started).id)
        val closed = events.last() as LiveEvent.Closed
        assertEquals("close_requested", closed.reason)
        assertEquals(67.0, closed.seconds, 0.0)
        assertTrue(events.any { it is LiveEvent.Usage })
        assertTrue(events.none { it is LiveEvent.Other && it.type == "unparseable" })
    }

    @Test
    fun buildsTheSameTurnsAsTheNodeScript() {
        val transcript = Transcript()
        fixture("technical-session.events.jsonl").lines().filter { it.isNotBlank() }.map(::parseEvent)
            .filterIsInstance<LiveEvent.Delta>().forEach(transcript::add)

        val expected = Json.parseToJsonElement(fixture("technical-session.transcript.json")).jsonObject["turns"]!!.jsonArray
        assertEquals(expected.size, transcript.all.size)
        expected.zip(transcript.all).forEach { (want, got) ->
            val w = want.jsonObject
            assertEquals(w["speaker"]!!.jsonPrimitive.content, got.speaker.name)
            assertEquals(w["text"]!!.jsonPrimitive.content, got.text)
            assertEquals(w["startMs"]!!.jsonPrimitive.long, got.startMs)
            assertEquals(w["endMs"]!!.jsonPrimitive.long, got.endMs)
        }
        assertEquals(Speaker.interviewer, transcript.all.first().speaker)
        assertTrue(transcript.current!!.text.endsWith("sample size?"))
    }

    @Test
    fun joinsCloseDeltasAndSplitsOnSpeakerOrGap() {
        val t = Transcript(joinGapMs = 1500)
        t.add(LiveEvent.Delta(Speaker.interviewer, "Hi ", 0, 400))
        t.add(LiveEvent.Delta(Speaker.interviewer, "there?", 500, 900))
        t.add(LiveEvent.Delta(Speaker.candidate, "Hello", 1000, 1300))
        t.add(LiveEvent.Delta(Speaker.candidate, " again", 5000, 5400))
        assertEquals(listOf("Hi there?", "Hello", " again"), t.all.map { it.text })
        assertEquals(1, t.interviewerQuestions())
    }

    @Test
    fun oddPayloadsDoNotCrash() {
        assertTrue(parseEvent("not json") is LiveEvent.Other)
        assertTrue(parseEvent("""{"no":"type"}""") is LiveEvent.Other)
        val failed = parseEvent("""{"type":"error","error":{"code":"x","message":"y"}}""") as LiveEvent.Failed
        assertEquals("x", failed.code)
        val delta = parseEvent("""{"type":"session.input_transcript.delta","delta":"hm"}""") as LiveEvent.Delta
        assertEquals(0L, delta.startMs)
    }

    @Test
    fun micLoudness() {
        assertEquals(0f, LiveClient.rms(ByteArray(960)), 0f)
        val loud = ByteArray(960) { if (it % 2 == 0) 0 else 0x40 }
        assertTrue(LiveClient.rms(loud) > 0.9f)
    }
}
