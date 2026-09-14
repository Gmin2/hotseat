package dev.mintu.hotseat.live

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/** Plays one clip at a time. */
interface VoicePlayback {
    /** [call] plays it on the call route, used while the interview is still live. */
    fun play(clip: Clip, call: Boolean)

    /** 0..1 through the clip, 1 once it has finished or nothing is playing. */
    fun position(): Float
    fun stop()

    object None : VoicePlayback {
        override fun play(clip: Clip, call: Boolean) = Unit
        override fun position() = 1f
        override fun stop() = Unit
    }
}

class VoicePlayer : VoicePlayback {
    private var track: AudioTrack? = null
    private var frames = 0

    override fun play(clip: Clip, call: Boolean) {
        stop()
        if (clip.pcm.isEmpty()) return
        val attributes = AudioAttributes.Builder()
            .setUsage(if (call) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(clip.rate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val next = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(clip.pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        }.getOrNull() ?: return
        next.write(clip.pcm, 0, clip.pcm.size)
        next.play()
        frames = clip.pcm.size
        track = next
    }

    override fun position(): Float {
        val t = track ?: return 1f
        return (t.playbackHeadPosition.toFloat() / frames).coerceIn(0f, 1f)
    }

    override fun stop() {
        track?.let {
            runCatching { it.stop() }
            it.release()
        }
        track = null
    }
}
