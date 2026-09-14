import { test } from "node:test"
import assert from "node:assert/strict"
import worker from "../src/index.ts"
import { parseSetup, parseTurns } from "../src/validate.ts"
import { greeting, instructions, MAX_JOB_POST } from "../src/prompt.ts"
import { candidateStats, clock, type Turn } from "../src/stats.ts"
import { outputText, reportRequest, shapeReport } from "../src/report.ts"

const turns: Turn[] = [
  { speaker: "interviewer", text: "Tell me about yourself.", startMs: 0, endMs: 2000 },
  { speaker: "candidate", text: "Um, I built a wallet app, like, the offline sync part. You know it was hard.", startMs: 3000, endMs: 9000 },
  { speaker: "interviewer", text: "What was hardest?", startMs: 10000, endMs: 11000 },
  { speaker: "candidate", text: "Never sending a payment twice.", startMs: 12000, endMs: 14000 },
]

const env = (over: Record<string, unknown> = {}) => ({ OPENAI_API_KEY: "sk-test", APP_KEY: "k", ...over })
const post = (path: string, body: unknown, headers: Record<string, string> = { "x-hotseat-key": "k" }) =>
  new Request(`http://w${path}`, { method: "POST", headers: { "content-type": "application/json", ...headers }, body: JSON.stringify(body) })

test("setup defaults and validation", () => {
  assert.deepEqual(parseSetup({}), { round: "behavioral", difficulty: "medium", style: "friendly", role: "mobile engineer", jobPost: undefined, minutes: 15 })
  assert.equal(parseSetup({ round: "karaoke" }), "round must be behavioral, technical, design or job")
  assert.equal(parseSetup({ round: "job" }), "jobPost is required for the job round")
  assert.equal(parseSetup({ difficulty: "brutal" }), "difficulty must be easy, medium or hard")
  assert.equal((parseSetup({ minutes: 90 }) as any).minutes, 20)
  assert.equal((parseSetup({ minutes: 1 }) as any).minutes, 5)
  assert.equal(parseSetup({ round: "job", jobPost: "x".repeat(MAX_JOB_POST * 2 + 1) }), `jobPost is too long, keep it under ${MAX_JOB_POST} characters`)
})

test("instructions follow the setup", () => {
  const s = parseSetup({ round: "technical", difficulty: "hard", style: "sharp", role: "Android engineer", minutes: 12 }) as any
  const text = instructions(s)
  assert.match(text, /Android technical mock interview for a Android engineer/)
  assert.match(text, /skeptical/)
  assert.match(text, /push back/)
  assert.match(text, /After about 12 minutes/)
  assert.doesNotMatch(text, /Job post/)
  const job = instructions(parseSetup({ round: "job", jobPost: "We build payments on Kotlin" }) as any)
  assert.match(job, /Job post:\nWe build payments on Kotlin/)
  assert.match(greeting(s), /^Greet the candidate now: Hi, thanks for joining\. I am your interviewer today\. To warm up/)
})

test("candidate stats count words, pace and fillers", () => {
  const s = candidateStats(turns)
  assert.equal(s.words, 21)
  assert.equal(s.fillers, 3)
  assert.equal(s.wpm, 158)
  assert.equal(clock(942), "15:42")
})

test("turn parsing rejects bad transcripts", () => {
  assert.equal(parseTurns([]), "transcript must be a non empty array of turns")
  assert.equal(parseTurns([{ speaker: "bot", text: "", startMs: 0, endMs: 1 }]), "speaker must be interviewer or candidate")
  assert.equal(parseTurns([{ speaker: "interviewer", text: "hi", startMs: 0, endMs: 1 }]), "the candidate never spoke")
  assert.equal((parseTurns(turns) as Turn[]).length, 4)
})

test("report shaping clamps what the model sends back", () => {
  const raw = {
    score: 140,
    summary: "s".repeat(300),
    answers: [
      { question: "q".repeat(80), score: -5, star: [4, 3, 2, 4, 1], note: "n", better: "b" },
      { question: "fine", score: 77, star: [0.5, 0.9], note: "ok", better: "try" },
    ],
  }
  const r = shapeReport(raw, turns, 95)
  assert.equal(r.score, 100)
  assert.equal(r.summary.length, 100)
  assert.equal(r.duration, "01:35")
  assert.equal(r.answers[0].score, 0)
  assert.equal(r.answers[0].question.length, 44)
  assert.deepEqual(r.answers[0].star, [1, 0.75, 0.5, 1])
  assert.deepEqual(r.answers[1].star, [0.5, 0.9, 0, 0])
  assert.equal(r.wpm, 158)
  assert.equal(shapeReport({}, turns, 10).answers.length, 0)
})

test("report request uses a strict schema and reads the message text", () => {
  const req = reportRequest("design", turns) as any
  assert.equal(req.text.format.strict, true)
  assert.match(req.input[0].content, /mobile system design/)
  assert.match(req.input[1].content, /^interviewer: Tell me about yourself\./)
  assert.equal(outputText({ output: [{ type: "reasoning" }, { type: "message", content: [{ type: "output_text", text: "{}" }] }] }), "{}")
  assert.throws(() => outputText({ output: [] }))
})

test("routes check key, method and body before calling openai", async () => {
  let calls = 0
  const e = env({ fetchOpenAI: async () => { calls++; return new Response("{}") } })
  assert.equal((await worker.fetch(new Request("http://w/health"), e)).status, 200)
  assert.equal((await worker.fetch(new Request("http://w/nope"), e)).status, 404)
  assert.equal((await worker.fetch(new Request("http://w/session"), e)).status, 405)
  assert.equal((await worker.fetch(post("/session", { sdp: "v=0" }, {}), e)).status, 401)
  assert.equal((await worker.fetch(post("/session", { sdp: "v=0" }, { "x-hotseat-key": "wrong" }), e)).status, 401)
  assert.equal((await worker.fetch(post("/session", {}), e)).status, 400)
  assert.equal((await worker.fetch(post("/session", { sdp: "v=0", round: "nope" }), e)).status, 400)
  assert.equal((await worker.fetch(post("/report", { transcript: [] }), e)).status, 400)
  assert.equal((await worker.fetch(post("/session", { sdp: "v=0" }), env({ OPENAI_API_KEY: undefined }))).status, 503)
  assert.equal(calls, 0)
})

test("session route sends the built instructions and returns the greeting", async () => {
  let sent: any
  const e = env({
    fetchOpenAI: async (url: string, init: any) => {
      assert.equal(url, "https://api.openai.com/v1/live/sessions")
      sent = JSON.parse(init.body)
      return Response.json({ session: { id: "live_1" }, transport: { sdp: "answer" } }, { status: 201 })
    },
  })
  const res = await worker.fetch(post("/session", { sdp: "offer", round: "technical", minutes: 10 }), e)
  assert.equal(res.status, 201)
  const body: any = await res.json()
  assert.equal(body.id, "live_1")
  assert.equal(body.sdp, "answer")
  assert.equal(body.maxSeconds, 720)
  assert.match(body.greeting, /^Greet the candidate now/)
  assert.equal(sent.session.model, "gpt-live-1")
  assert.equal(sent.transport.sdp, "offer")
  assert.match(sent.session.instructions, /Android technical/)
})

test("upstream failures become 502 without leaking detail", async () => {
  const e = env({ fetchOpenAI: async () => new Response('{"error":{"message":"secret detail"}}', { status: 401 }) })
  const res = await worker.fetch(post("/session", { sdp: "offer" }), e)
  assert.equal(res.status, 502)
  assert.doesNotMatch(await res.text(), /secret/)
  const bad = env({ fetchOpenAI: async () => Response.json({ output: [] }) })
  assert.equal((await worker.fetch(post("/report", { transcript: turns }), bad)).status, 502)
})

test("rate limiter blocks by device", async () => {
  const seen: string[] = []
  const e = env({
    SESSION_LIMIT: { limit: async ({ key }: { key: string }) => { seen.push(key); return { success: false } } },
    fetchOpenAI: async () => { throw new Error("should not be called") },
  })
  const res = await worker.fetch(post("/session", { sdp: "offer" }, { "x-hotseat-key": "k", "x-device-id": "phone-1" }), e)
  assert.equal(res.status, 429)
  assert.deepEqual(seen, ["phone-1"])
})
