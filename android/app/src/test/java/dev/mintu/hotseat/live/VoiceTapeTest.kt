package dev.mintu.hotseat.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class VoiceTapeTest {
    @get:Rule
    val folder = TemporaryFolder()

    // webrtc hands over 10 ms frames at 48 khz, feed the tape the same way
    private fun VoiceTape.feed(ms: Int, loud: Boolean) {
        repeat(ms / 10) { frame ->
            val pcm = ShortArray(480) { i -> if (loud) (6000 * sin(2 * PI * 220 * (frame * 480 + i) / 48_000)).toInt().toShort() else 0 }
            write(pcm, 48_000)
        }
    }

    private fun tape() = VoiceTape(File(folder.root, "tape.pcm"), now = { 0L })

    // the session clock starts 1.2 s before the tape hears anything, and the second answer drifts 180 ms late
    private val turns = listOf(
        Turn(Speaker.interviewer, "Hi, which app would you like to design?", 1_200, 3_200),
        Turn(Speaker.candidate, "The chat app", 4_000, 5_000),
        Turn(Speaker.interviewer, "Great, what are the core features?", 6_200, 7_700),
    )

    private fun recorded() = tape().apply {
        feed(400, loud = false)
        feed(2_000, loud = true)
        feed(3_180, loud = false)
        feed(1_500, loud = true)
        feed(1_200, loud = false)
    }

    @Test
    fun findsEachInterviewerTurnOnTheTape() {
        val t = recorded()
        // windows are 20 ms, speech starts at window 20 and 279
        val first = t.locate(turns, 0)!!
        assertEquals(20 - 6, first.first)
        assertEquals(120 + 10, first.last)
        val second = t.locate(turns, 2)!!
        assertEquals(279 - 6, second.first)
        assertEquals(354 + 10, second.last)
        assertNull(t.locate(turns, 1))
    }

    @Test
    fun clipsAreTheInterviewersAudioWithoutTheSilence() {
        val t = recorded()
        val clip = t.clip(turns, 2)!!
        assertEquals(VoiceTape.RATE, clip.rate)
        assertEquals((364 - 273) * 20L, clip.ms)
        val speech = clip.pcm.count { abs(it.toInt()) > 1000 }
        assertTrue("mostly speech: $speech of ${clip.pcm.size}", speech > clip.pcm.size / 2)
        assertEquals(0, clip.pcm.first().toInt())
    }

    @Test
    fun theTurnBeingSpokenIsNotReadyUntilItGoesQuiet() {
        val t = tape()
        val talking = listOf(Turn(Speaker.interviewer, "Hi", 0, 800))
        assertFalse(t.has(talking, 0))
        t.feed(1_000, loud = true)
        assertFalse(t.has(talking, 0))
        t.feed(500, loud = false)
        assertFalse(t.has(talking, 0))
        t.feed(500, loud = false)
        assertTrue(t.has(talking, 0))
        assertFalse(t.has(listOf(Turn(Speaker.candidate, "Yo", 0, 800)), 0))
    }

    @Test
    fun closingDeletesTheTape() {
        val file = File(folder.root, "gone.pcm")
        val t = VoiceTape(file, now = { 0L })
        t.feed(1_000, loud = true)
        t.feed(1_000, loud = false)
        assertTrue(file.exists())
        t.close()
        assertFalse(file.exists())
        assertNull(t.clip(listOf(Turn(Speaker.interviewer, "Hi", 0, 1000)), 0))
    }

    @Test
    fun wavRoundTrips() {
        val clip = Clip(ShortArray(2400) { (it * 7).toShort() }, 24_000)
        val file = File(folder.root, "voice/1.wav")
        Wav.write(file, clip)
        val back = Wav.read(file)
        assertNotNull(back)
        assertEquals(24_000, back!!.rate)
        assertTrue(clip.pcm.contentEquals(back.pcm))
        assertEquals(44 + 4800L, file.length())
    }

    @Test
    fun resamplesToTheTapeRate() {
        assertEquals(240, VoiceTape.resample(ShortArray(480) { 100 }, 48_000).size)
        assertEquals(480, VoiceTape.resample(ShortArray(320), 16_000).size)
    }
}
