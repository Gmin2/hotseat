package dev.mintu.hotseat.live

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonPrimitive
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.AudioTrackSink
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt

/**
 * One GPT-Live interview over WebRTC. The mic goes up as an audio track, the interviewer comes back as one,
 * and JSON events ride the oai-events data channel. The worker swaps our SDP offer for OpenAI's answer.
 */
/** What the practice screen needs from a live interview. LiveClient is the real one, tests use a fake. */
/** Where a starting interview is, for the waiting ui. */
enum class ConnectStep { Idle, Mic, Calling, Joining, Ready }

interface LiveSession {
    val step: StateFlow<ConnectStep>
    val events: SharedFlow<LiveEvent>
    val interviewerLevel: StateFlow<Float>
    val candidateLevel: StateFlow<Float>
    suspend fun start(setup: InterviewSetup): SessionResponse
    suspend fun stop(): LiveEvent.Closed?
    /** The interviewer's voice for replaying a turn, null when there is none. */
    val voice: VoiceSource? get() = null
    fun setMuted(muted: Boolean)
    fun release()
}

class LiveClient(context: Context, private val api: WorkerApi, private val scope: CoroutineScope) : LiveSession {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)

    private val _events = MutableSharedFlow<LiveEvent>(replay = 0, extraBufferCapacity = 512)
    override val events: SharedFlow<LiveEvent> = _events

    private val _step = MutableStateFlow(ConnectStep.Idle)
    override val step: StateFlow<ConnectStep> = _step
    private var greeted = false
    private var firstWords = false
    private var t0 = 0L
    private fun mark(label: String) = Log.i(TAG, "timing ${android.os.SystemClock.elapsedRealtime() - t0}ms $label")

    // 0..1, how loud each side is right now, drives the orb and ticks
    private val _interviewerLevel = MutableStateFlow(0f)
    override val interviewerLevel: StateFlow<Float> = _interviewerLevel
    private val _candidateLevel = MutableStateFlow(0f)
    override val candidateLevel: StateFlow<Float> = _candidateLevel

    private var adm: JavaAudioDeviceModule? = null
    private var factory: PeerConnectionFactory? = null
    private var source: AudioSource? = null
    private var mic: AudioTrack? = null
    private var peer: PeerConnection? = null
    private var channel: DataChannel? = null
    private var stats: Job? = null
    private var closed = CompletableDeferred<LiveEvent.Closed>()
    private var remote: AudioTrack? = null
    private var tape: VoiceTape? = null
    private val sink = AudioTrackSink { data, bits, rate, channels, frames, _ -> tape?.write(data, bits, rate, channels, frames) }
    override val voice: VoiceSource? get() = tape

    var session: SessionResponse? = null
        private set

    override suspend fun start(setup: InterviewSetup): SessionResponse {
        t0 = android.os.SystemClock.elapsedRealtime()
        greeted = false
        firstWords = false
        _step.value = ConnectStep.Mic
        initWebRtc(app)
        closed = CompletableDeferred()
        routeToSpeaker(true)
        tape = VoiceTape(tapeFile())

        val module = JavaAudioDeviceModule.builder(app)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setSamplesReadyCallback { samples -> _candidateLevel.value = rms(samples.data) }
            .createAudioDeviceModule()
        adm = module
        val pcf = PeerConnectionFactory.builder().setAudioDeviceModule(module).createPeerConnectionFactory()
        factory = pcf

        val constraints = MediaConstraints().apply {
            mandatory += MediaConstraints.KeyValuePair("googEchoCancellation", "true")
            mandatory += MediaConstraints.KeyValuePair("googNoiseSuppression", "true")
            mandatory += MediaConstraints.KeyValuePair("googAutoGainControl", "true")
            mandatory += MediaConstraints.KeyValuePair("googHighpassFilter", "true")
        }
        source = pcf.createAudioSource(constraints)
        mic = pcf.createAudioTrack("mic", source)

        val gathered = CompletableDeferred<Unit>()
        val config = PeerConnection.RTCConfiguration(
            // no stun: OpenAI's side is public, our host candidates reach it, and gathering is done in a few ms instead of seconds
            emptyList(),
        ).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }

        val pc = pcf.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                if (state == PeerConnection.IceGatheringState.COMPLETE) gathered.complete(Unit)
            }
            override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
                Log.i(TAG, "connection $state")
                if (state == PeerConnection.PeerConnectionState.FAILED) {
                    _events.tryEmit(LiveEvent.Failed("connection_failed", "the voice connection dropped"))
                    closed.complete(LiveEvent.Closed("connection_lost", 0.0))
                }
            }
            override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) = Unit
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceCandidate(candidate: IceCandidate) = Unit
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
            override fun onAddStream(stream: MediaStream) = Unit
            override fun onRemoveStream(stream: MediaStream) = Unit
            override fun onDataChannel(channel: DataChannel) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
                (receiver.track() as? AudioTrack)?.let { track ->
                    remote = track
                    track.addSink(sink)
                }
            }
        }) ?: error("could not create a peer connection")
        peer = pc
        pc.addTrack(mic, listOf("hotseat"))

        // the channel has to exist before the offer so it lands in the sdp
        val dc = pc.createDataChannel("oai-events", DataChannel.Init())
        channel = dc
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previous: Long) = Unit
            override fun onStateChange() {
                if (dc.state() == DataChannel.State.OPEN) session?.let { greet(it.greeting) }
            }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining()).also { buffer.data.get(it) }
                val event = parseEvent(bytes.decodeToString())
                if (event is LiveEvent.Started) {
                    mark("session started")
                    _step.value = ConnectStep.Ready
                }
                if (event is LiveEvent.Delta && event.speaker == Speaker.interviewer && _step.value != ConnectStep.Idle && !firstWords) {
                    firstWords = true
                    mark("first interviewer words")
                }
                if (event is LiveEvent.Closed) closed.complete(event)
                _events.tryEmit(event)
            }
        })
        val offer = pc.awaitOffer()
        pc.awaitLocal(offer)
        mark("offer created")
        withTimeoutOrNull(1_500) { gathered.await() }
        mark("candidates gathered")
        _step.value = ConnectStep.Calling
        val sdp = pc.localDescription?.description ?: error("no local description")

        val response = api.session(sdp, setup)
        mark("worker answered")
        _step.value = ConnectStep.Joining
        session = response
        pc.awaitRemote(SessionDescription(SessionDescription.Type.ANSWER, response.sdp))
        if (dc.state() == DataChannel.State.OPEN) greet(response.greeting)

        stats = scope.launch { pollInterviewerLevel(pc) }
        return response
    }

    override fun setMuted(muted: Boolean) {
        mic?.setEnabled(!muted)
    }

    /** Asks GPT-Live to finish, waits for its final usage, then tears everything down. */
    override suspend fun stop(): LiveEvent.Closed? {
        val dc = channel
        val result = if (dc != null && dc.state() == DataChannel.State.OPEN) {
            send("""{"type":"session.close"}""")
            withTimeoutOrNull(6_000) { closed.await() }
        } else null
        release()
        return result
    }

    // the tape outlives the call, whoever saves the interview closes it
    override fun release() {
        stats?.cancel()
        remote?.let { runCatching { it.removeSink(sink) } }
        remote = null
        channel?.unregisterObserver()
        channel?.close()
        peer?.close()
        peer?.dispose()
        mic?.dispose()
        source?.dispose()
        factory?.dispose()
        adm?.release()
        channel = null; peer = null; mic = null; source = null; factory = null; adm = null
        _interviewerLevel.value = 0f
        _candidateLevel.value = 0f
        _step.value = ConnectStep.Idle
        routeToSpeaker(false)
    }

    // the channel can open before or after the answer lands, whichever comes second sends it, once
    @Synchronized
    private fun greet(greeting: String) {
        if (greeted) return
        greeted = true
        mark("greeting sent")
        val content = JsonPrimitive(greeting).toString()
        send("""{"type":"session.commentary.append","delegation_id":null,"content":$content}""")
    }

    private fun tapeFile(): File {
        val dir = File(app.cacheDir, "tapes").apply { mkdirs() }
        // a tape from an interview that never got saved is left behind, clear anything older than an hour
        dir.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 3_600_000 }?.forEach { it.delete() }
        return File(dir, "${System.currentTimeMillis()}.pcm")
    }

    private fun send(text: String) {
        channel?.send(DataChannel.Buffer(ByteBuffer.wrap(text.encodeToByteArray()), false))
    }

    private suspend fun pollInterviewerLevel(pc: PeerConnection) {
        while (scope.isActive && peer != null) {
            pc.getStats { report ->
                val inbound = report.statsMap.values.firstOrNull { it.type == "inbound-rtp" && it.members["kind"] == "audio" }
                val level = (inbound?.members?.get("audioLevel") as? Double)?.toFloat() ?: 0f
                // audioLevel is linear 0..1 and rarely goes above 0.3 for speech, stretch it so the orb actually moves
                _interviewerLevel.value = (level * 3.2f).coerceIn(0f, 1f)
            }
            delay(90)
        }
    }

    private fun routeToSpeaker(on: Boolean) {
        if (on) {
            audio.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audio.availableCommunicationDevices
                // prefer a headset if one is connected, otherwise the loudspeaker
                val pick = devices.firstOrNull { it.type in HEADSETS } ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                pick?.let { audio.setCommunicationDevice(it) }
            } else {
                @Suppress("DEPRECATION")
                audio.isSpeakerphoneOn = true
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audio.clearCommunicationDevice()
            audio.mode = AudioManager.MODE_NORMAL
        }
    }

    companion object {
        private const val TAG = "HotseatLive"
        private val HEADSETS = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
        @Volatile private var initialized = false

        private fun initWebRtc(context: Context) {
            if (initialized) return
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions())
            initialized = true
        }

        // 16 bit pcm to a 0..1 loudness, speech sits around 0.05 to 0.3 rms so it gets a boost
        fun rms(pcm: ByteArray): Float {
            if (pcm.size < 2) return 0f
            var sum = 0.0
            var i = 0
            while (i + 1 < pcm.size) {
                val sample = (pcm[i].toInt() and 0xFF) or (pcm[i + 1].toInt() shl 8)
                val v = sample.toShort() / 32768.0
                sum += v * v
                i += 2
            }
            return (sqrt(sum / (pcm.size / 2)) * 4.0).toFloat().coerceIn(0f, 1f)
        }
    }
}

private suspend fun PeerConnection.awaitOffer(): SessionDescription = suspendCancellableCoroutine { cont ->
    createOffer(object : SdpAdapter() {
        override fun onCreateSuccess(sdp: SessionDescription) = cont.resume(sdp)
        override fun onCreateFailure(error: String) = cont.resumeWithException(IllegalStateException("offer failed: $error"))
    }, MediaConstraints())
}

private suspend fun PeerConnection.awaitLocal(sdp: SessionDescription) = suspendCancellableCoroutine { cont ->
    setLocalDescription(object : SdpAdapter() {
        override fun onSetSuccess() = cont.resume(Unit)
        override fun onSetFailure(error: String) = cont.resumeWithException(IllegalStateException("local sdp failed: $error"))
    }, sdp)
}

private suspend fun PeerConnection.awaitRemote(sdp: SessionDescription) = suspendCancellableCoroutine { cont ->
    setRemoteDescription(object : SdpAdapter() {
        override fun onSetSuccess() = cont.resume(Unit)
        override fun onSetFailure(error: String) = cont.resumeWithException(IllegalStateException("remote sdp failed: $error"))
    }, sdp)
}

private open class SdpAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(error: String) = Unit
    override fun onSetFailure(error: String) = Unit
}
