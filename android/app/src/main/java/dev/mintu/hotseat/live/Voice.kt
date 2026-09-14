package dev.mintu.hotseat.live

import java.io.DataInputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/** A bit of the interviewer's voice, mono 16 bit pcm. */
class Clip(val pcm: ShortArray, val rate: Int) {
    val ms get() = pcm.size * 1000L / rate
}

/** Where the chat gets the interviewer's voice for a turn from: the tape while live, wav files once saved. */
interface VoiceSource {
    fun has(turns: List<Turn>, index: Int): Boolean
    fun clip(turns: List<Turn>, index: Int): Clip?
    fun close() = Unit
}

/**
 * Records what the interviewer says during a live interview into a raw pcm file, and cuts out the audio for one turn.
 *
 * Transcript times and the audio share the session clock, so the tape is lined up on the first time the interviewer
 * makes a sound, each turn is looked for where its start time says, then snapped to the nearest start of speech.
 */
class VoiceTape(private val file: File, private val now: () -> Long = System::nanoTime) : VoiceSource {
    private val out = RandomAccessFile(file, "rw").apply { setLength(0) }
    private val buffer = ByteBuffer.allocate(RATE).order(ByteOrder.LITTLE_ENDIAN)
    private var written = 0L
    private var startedAt = -1L
    // loudness of every 20 ms window, enough to find speech without keeping the audio in memory
    private var loud = BooleanArray(3000)
    private var windows = 0
    private var windowSum = 0.0
    private var windowFill = 0
    private var quietWindows = 0
    private var closed = false

    @Synchronized
    fun write(data: ByteBuffer, bits: Int, rate: Int, channels: Int, frames: Int) {
        if (closed || bits != 16 || channels < 1 || rate <= 0) return
        val src = data.duplicate().order(ByteOrder.nativeOrder())
        val mono = ShortArray(frames)
        for (i in 0 until frames) {
            var sum = 0
            for (c in 0 until channels) sum += src.getShort()
            mono[i] = (sum / channels).toShort()
        }
        val t = now()
        if (startedAt < 0) startedAt = t
        // if frames stop coming for a while keep the timeline honest with silence
        val due = (t - startedAt) / 1_000_000L * RATE / 1000
        if (due - written > RATE / 2) repeat((due - written).toInt()) { append(0) }
        resample(mono, rate).forEach { append(it) }
    }

    fun write(samples: ShortArray, rate: Int) {
        val bytes = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.nativeOrder())
        samples.forEach { bytes.putShort(it) }
        bytes.flip()
        write(bytes, 16, rate, 1, samples.size)
    }

    private fun append(sample: Short) {
        if (!buffer.hasRemaining()) flush()
        buffer.putShort(sample)
        written++
        val v = sample / 32768.0
        windowSum += v * v
        if (++windowFill == WINDOW) {
            if (windows == loud.size) loud = loud.copyOf(loud.size * 2)
            val speech = sqrt(windowSum / WINDOW) > THRESHOLD
            loud[windows++] = speech
            quietWindows = if (speech) 0 else quietWindows + 1
            windowSum = 0.0
            windowFill = 0
        }
    }

    private fun flush() {
        buffer.flip()
        out.seek(out.length())
        out.write(buffer.array(), 0, buffer.limit())
        buffer.clear()
    }

    @Synchronized
    override fun has(turns: List<Turn>, index: Int): Boolean {
        val turn = turns.getOrNull(index) ?: return false
        if (closed || turn.speaker != Speaker.interviewer || firstSpeech() < 0) return false
        // the turn being spoken right now is not ready until the interviewer has gone quiet
        return index < turns.lastIndex || quietWindows >= 45
    }

    @Synchronized
    override fun clip(turns: List<Turn>, index: Int): Clip? {
        if (closed) return null
        val range = locate(turns, index) ?: return null
        flush()
        val from = range.first * WINDOW.toLong()
        val count = ((range.last - range.first) * WINDOW).coerceAtMost((written - from).toInt())
        if (count <= 0) return null
        val bytes = ByteArray(count * 2)
        out.seek(from * 2)
        out.readFully(bytes)
        val pcm = ShortArray(count)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm)
        return Clip(fade(pcm), RATE)
    }

    /** Window range of a turn on the tape, or null when it cannot be found. */
    internal fun locate(turns: List<Turn>, index: Int): IntRange? {
        val turn = turns.getOrNull(index)?.takeIf { it.speaker == Speaker.interviewer } ?: return null
        val firstIndex = turns.indexOfFirst { it.speaker == Speaker.interviewer }
        val first = turns[firstIndex]
        val onset = firstSpeech().takeIf { it >= 0 } ?: return null
        val start = if (index == firstIndex) onset else {
            val guess = onset + ((turn.startMs - first.startMs) / WINDOW_MS).toInt()
            nearestStart(guess) ?: return null
        }
        val next = turns.drop(index + 1).firstOrNull { it.speaker == Speaker.interviewer }
            ?.let { onset + ((it.startMs - first.startMs) / WINDOW_MS).toInt() - 10 } ?: Int.MAX_VALUE
        var end = (start + ((turn.endMs - turn.startMs) / WINDOW_MS).toInt()).coerceAtMost(windows)
        // run on while the speech keeps going, a pause of half a second ends it
        while (end < windows && end < next && (end until minOf(windows, end + 25)).any { loud[it] }) end++
        while (end > start && !loud[end - 1]) end--
        if (end <= start) return null
        return (start - LEAD).coerceAtLeast(0)..(end + TAIL).coerceAtMost(windows)
    }

    private var onset = -1

    private fun firstSpeech(): Int {
        if (onset < 0) onset = (0 until windows).firstOrNull { loud[it] } ?: -1
        return onset
    }

    // where speech begins after at least a quarter second of quiet, closest to the guess
    private fun nearestStart(guess: Int): Int? =
        (maxOf(1, guess - SEARCH)..minOf(windows - 1, guess + SEARCH))
            .filter { w -> loud[w] && (maxOf(0, w - 12) until w).none { loud[it] } }
            .minByOrNull { abs(it - guess) }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        out.close()
        file.delete()
    }

    companion object {
        const val RATE = 24_000
        private const val WINDOW_MS = 20L
        private const val WINDOW = RATE / 50
        private const val THRESHOLD = 0.012
        private const val SEARCH = 60
        private const val LEAD = 6
        private const val TAIL = 10

        internal fun resample(input: ShortArray, rate: Int): ShortArray = when (rate) {
            RATE -> input
            RATE * 2 -> ShortArray(input.size / 2) { ((input[it * 2] + input[it * 2 + 1]) / 2).toShort() }
            else -> {
                val size = (input.size.toLong() * RATE / rate).toInt()
                ShortArray(size) {
                    val pos = it.toDouble() * rate / RATE
                    val i = pos.toInt().coerceAtMost(input.size - 1)
                    val j = (i + 1).coerceAtMost(input.size - 1)
                    (input[i] + (input[j] - input[i]) * (pos - i)).toInt().toShort()
                }
            }
        }

        // a few ms of fade so the cut never clicks
        private fun fade(pcm: ShortArray): ShortArray {
            val n = minOf(pcm.size / 2, RATE / 100)
            for (i in 0 until n) {
                val g = i.toFloat() / n
                pcm[i] = (pcm[i] * g).toInt().toShort()
                pcm[pcm.size - 1 - i] = (pcm[pcm.size - 1 - i] * g).toInt().toShort()
            }
            return pcm
        }
    }
}

/** A finished interview's voice, one wav per interviewer turn, named by turn index. */
class SavedVoice(private val dir: File) : VoiceSource {
    override fun has(turns: List<Turn>, index: Int) = File(dir, "$index.wav").isFile
    override fun clip(turns: List<Turn>, index: Int) = File(dir, "$index.wav").takeIf { it.isFile }?.let(Wav::read)
}

object Wav {
    fun write(file: File, clip: Clip) {
        val data = clip.pcm.size * 2
        val bytes = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
        bytes.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(clip.rate).putInt(clip.rate * 2).putShort(2).putShort(16)
        bytes.put("data".toByteArray()).putInt(data)
        clip.pcm.forEach { bytes.putShort(it) }
        file.parentFile?.mkdirs()
        file.writeBytes(bytes.array())
    }

    /** Reads the wavs [write] makes: 44 byte header, mono 16 bit. */
    fun read(file: File): Clip? = runCatching {
        DataInputStream(file.inputStream().buffered()).use { input ->
            val header = ByteArray(44).also { input.readFully(it) }
            val h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val rate = h.getInt(24)
            val size = h.getInt(40)
            val data = ByteArray(size).also { input.readFully(it) }
            val pcm = ShortArray(size / 2)
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm)
            Clip(pcm, rate)
        }
    }.getOrNull()
}
