package dev.mintu.hotseat.live

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class WorkerException(val status: Int, message: String) : IOException(message)

/** Talks to the hotseat worker. The OpenAI key lives there, the app only carries the app key. */
class WorkerApi(
    private val baseUrl: String,
    private val appKey: String,
    private val deviceId: String,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val type = "application/json".toMediaType()

    suspend fun session(sdp: String, setup: InterviewSetup): SessionResponse = post(
        "/session",
        json.encodeToString(SessionRequest(sdp, setup.round, setup.difficulty, setup.style, setup.role, setup.minutes, setup.jobPost)),
    )

    suspend fun report(round: String, seconds: Double, transcript: List<Turn>): LiveReport =
        post("/report", json.encodeToString(ReportRequest(round, seconds, transcript)))

    private suspend inline fun <reified T> post(path: String, body: String): T = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .header("x-hotseat-key", appKey)
            .header("x-device-id", deviceId)
            .post(body.toRequestBody(type))
            .build()
        http.newCall(request).execute().use { res ->
            val text = res.body.string()
            if (!res.isSuccessful) {
                val reason = runCatching { json.decodeFromString<WorkerError>(text).error }.getOrDefault("worker returned ${res.code}")
                throw WorkerException(res.code, reason)
            }
            json.decodeFromString<T>(text)
        }
    }
}
