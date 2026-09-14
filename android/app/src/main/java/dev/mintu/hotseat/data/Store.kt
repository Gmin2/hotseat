package dev.mintu.hotseat.data

import dev.mintu.hotseat.live.LiveReport
import dev.mintu.hotseat.live.SavedVoice
import dev.mintu.hotseat.live.Speaker
import dev.mintu.hotseat.live.Turn
import dev.mintu.hotseat.live.VoiceSource
import dev.mintu.hotseat.live.Wav
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Profile(
    val name: String = "",
    val role: String = "Android engineer",
    val onboarded: Boolean = false,
    val style: Int = 0,
    val difficulty: Int = 1,
    val minutes: Int = 15,
    val captions: Boolean = true,
)

@Serializable
data class SavedSession(
    val id: String,
    val round: Int,
    val startedAt: Long,
    val seconds: Double,
    val turns: List<Turn>,
    val report: LiveReport,
)

@Serializable
data class Saved(val profile: Profile = Profile(), val sessions: List<SavedSession> = emptyList())

/**
 * Everything Hotseat keeps lives in one json file in the app's private storage: the profile and finished interviews.
 * The interviewer's voice sits next to it, one folder of wavs per interview. Nothing is kept on the worker.
 * [deleteAll] removes all of it and resets to a fresh install.
 */
class Store(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _saved = MutableStateFlow(load())
    val saved: StateFlow<Saved> = _saved

    val profile get() = _saved.value.profile

    private fun load(): Saved = runCatching { json.decodeFromString<Saved>(file.readText()) }.getOrDefault(Saved())

    @Synchronized
    private fun write(next: Saved) {
        _saved.value = next
        file.parentFile?.mkdirs()
        // write next to it and swap, so a crash mid write never leaves a broken file behind
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(Saved.serializer(), next))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    fun updateProfile(change: (Profile) -> Profile) = write(_saved.value.copy(profile = change(_saved.value.profile)))

    fun addSession(session: SavedSession) =
        write(_saved.value.copy(sessions = (listOf(session) + _saved.value.sessions).take(MAX_SESSIONS)))

    fun deleteSession(id: String) {
        write(_saved.value.copy(sessions = _saved.value.sessions.filterNot { it.id == id }))
        voiceDir(id).deleteRecursively()
    }

    @Synchronized
    fun deleteAll() {
        file.delete()
        File(file.parentFile, file.name + ".tmp").delete()
        voiceRoot.deleteRecursively()
        _saved.value = Saved()
    }

    private val voiceRoot get() = File(file.parentFile, "voice")
    private fun voiceDir(id: String) = File(voiceRoot, id)

    fun voice(id: String): VoiceSource? = voiceDir(id).takeIf { it.isDirectory }?.let(::SavedVoice)

    /** Cuts every interviewer turn out of [source] into wavs, keeping voice for the newest [VOICE_KEPT] interviews only. */
    fun saveVoice(id: String, turns: List<Turn>, source: VoiceSource) {
        val dir = voiceDir(id)
        turns.indices.filter { turns[it].speaker == Speaker.interviewer }.forEach { i ->
            source.clip(turns, i)?.let { Wav.write(File(dir, "$i.wav"), it) }
        }
        val keep = _saved.value.sessions.take(VOICE_KEPT).map { it.id }.toSet() + id
        voiceRoot.listFiles()?.filter { it.name !in keep }?.forEach { it.deleteRecursively() }
    }

    companion object {
        const val MAX_SESSIONS = 200
        const val VOICE_KEPT = 30

        @Volatile private var instance: Store? = null

        fun get(context: android.content.Context): Store =
            instance ?: synchronized(this) {
                instance ?: Store(File(context.applicationContext.filesDir, "hotseat.json")).also { instance = it }
            }
    }
}
