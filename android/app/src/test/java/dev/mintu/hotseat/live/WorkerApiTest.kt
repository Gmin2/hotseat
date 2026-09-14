package dev.mintu.hotseat.live

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class WorkerApiTest {
    private val server = MockWebServer()
    private lateinit var api: WorkerApi

    @Before
    fun setUp() {
        server.start()
        api = WorkerApi(server.url("/").toString(), appKey = "k", deviceId = "phone-1")
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun sessionSendsSetupAndHeaders() = runTest {
        server.enqueue(MockResponse.Builder().code(201).body("""{"id":"live_1","sdp":"answer","greeting":"Speak first","maxSeconds":1020,"extra":true}""").build())
        val res = api.session("offer", InterviewSetup(round = "technical", minutes = 15))
        assertEquals("live_1", res.id)
        assertEquals(1020, res.maxSeconds)

        val req = server.takeRequest()
        assertEquals("/session", req.url.encodedPath)
        assertEquals("k", req.headers["x-hotseat-key"])
        assertEquals("phone-1", req.headers["x-device-id"])
        val body = Json.parseToJsonElement(req.body!!.utf8()).jsonObject
        assertEquals("offer", body["sdp"]!!.jsonPrimitive.content)
        assertEquals("technical", body["round"]!!.jsonPrimitive.content)
        assertNull(body["jobPost"])
    }

    @Test
    fun reportDecodesTheWorkerShape() = runTest {
        val fixture = javaClass.classLoader!!.getResource("fixtures/technical-session.report.json")!!.readText()
        server.enqueue(MockResponse.Builder().body(fixture).build())
        val report = api.report("technical", 67.0, listOf(Turn(Speaker.candidate, "hi", 0, 10)))
        assertEquals(48, report.score)
        assertEquals(3, report.answers.size)
        assertEquals(4, report.answers.first().star.size)
        val body = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertEquals("candidate", body["transcript"].toString().substringAfter("speaker\":\"").substringBefore("\""))
    }

    @Test
    fun errorsCarryStatusAndReason() = runTest {
        server.enqueue(MockResponse.Builder().code(429).body("""{"error":"too many sessions, wait a minute"}""").build())
        try {
            api.session("offer", InterviewSetup())
            fail("expected an error")
        } catch (e: WorkerException) {
            assertEquals(429, e.status)
            assertEquals("too many sessions, wait a minute", e.message)
        }
        server.enqueue(MockResponse.Builder().code(502).body("gateway down").build())
        try {
            api.report("behavioral", 1.0, emptyList())
            fail("expected an error")
        } catch (e: WorkerException) {
            assertTrue(e.message!!.contains("502"))
        }
    }
}
