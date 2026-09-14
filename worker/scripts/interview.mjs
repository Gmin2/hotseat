// end to end voice test: connects like the app, streams a scripted candidate audio file as the mic,
// and records every event plus a turn by turn transcript
// usage: node scripts/interview.mjs <candidate.wav> <out dir> [body json]
import { writeFileSync, appendFileSync, mkdirSync } from "node:fs"
import { createSocket } from "node:dgram"
import { spawn } from "node:child_process"
import { RTCPeerConnection, MediaStreamTrack } from "werift"

const [wav, out, bodyJson = "{}"] = process.argv.slice(2)
const base = process.env.WORKER_URL ?? "http://127.0.0.1:8790"
const appKey = process.env.APP_KEY ?? "dev"
mkdirSync(out, { recursive: true })
writeFileSync(`${out}/events.jsonl`, "")

const pc = new RTCPeerConnection({ bundlePolicy: "max-bundle", iceServers: process.env.STUN ? [{ urls: "stun:stun.l.google.com:19302" }] : [] })
const mic = new MediaStreamTrack({ kind: "audio" })
const audio = pc.addTransceiver(mic, { direction: "sendrecv" })
const events = pc.createDataChannel("oai-events")

let heard = 0
audio.onTrack.subscribe((t) => t.onReceiveRtp.subscribe(() => heard++ === 0 && mark("first audio packet")))

const started = Date.now()
const turns = []
function add(speaker, delta, startMs, endMs) {
  const last = turns.at(-1)
  // same speaker and close in time continues the turn
  if (last && last.speaker === speaker && startMs - last.endMs < 1500) {
    last.text += delta
    last.endMs = endMs
  } else turns.push({ speaker, text: delta, startMs, endMs })
}

let closed
const done = new Promise((r) => (closed = r))
events.onMessage.subscribe((raw) => {
  const text = raw.toString()
  appendFileSync(`${out}/events.jsonl`, text + "\n")
  const e = JSON.parse(text)
  if (e.type === "session.output_transcript.delta" && !turns.some((t) => t.speaker === "interviewer")) mark("first interviewer words")
  if (e.type === "session.output_transcript.delta") add("interviewer", e.delta, e.start_ms, e.end_ms)
  else if (e.type === "session.input_transcript.delta") add("candidate", e.delta, e.start_ms, e.end_ms)
  else console.log(`${((Date.now() - started) / 1000).toFixed(1)}s`, e.type, e.reason ?? e.error?.code ?? "")
  if (e.type === "session.closed") closed(e)
})

const mark = (label) => console.log(`${((Date.now() - started) / 1000).toFixed(2)}s ${label}`)
await pc.setLocalDescription(await pc.createOffer())
if (pc.iceGatheringState !== "complete") await new Promise((r) => pc.iceGatheringStateChange.subscribe((s) => s === "complete" && r()))
mark("offer ready")

const res = await fetch(`${base}/session`, {
  method: "POST",
  headers: { "content-type": "application/json", "x-hotseat-key": appKey, "x-device-id": "e2e-script" },
  body: JSON.stringify({ sdp: pc.localDescription.sdp, ...JSON.parse(bodyJson) }),
})
const body = await res.json()
mark("worker answered")
console.log("worker:", res.status, body.id ?? body)
if (!res.ok) process.exit(1)
await pc.setRemoteDescription({ type: "answer", sdp: body.sdp })

await new Promise((r) => events.stateChanged.subscribe((s) => s === "open" && r()))
const greeting = process.env.GREET ?? body.greeting
if (greeting) events.send(JSON.stringify({ type: process.env.GREET_TYPE ?? "session.commentary.append", delegation_id: null, content: greeting }))

// ffmpeg plays the candidate file in real time as opus rtp, we forward each packet into the mic track
const udp = createSocket("udp4")
udp.on("message", (packet) => mic.writeRtp(packet))
await new Promise((r) => udp.bind(0, "127.0.0.1", r))
const ff = spawn("ffmpeg", ["-v", "error", "-re", "-i", wav, "-ac", "1", "-ar", "48000", "-c:a", "libopus", "-b:a", "32k", "-payload_type", "96", "-f", "rtp", `rtp://127.0.0.1:${udp.address().port}`])
await new Promise((r) => ff.on("exit", r))

console.log(`${((Date.now() - started) / 1000).toFixed(1)}s candidate audio finished, closing`)
events.send(JSON.stringify({ type: "session.close" }))
const final = await Promise.race([done, new Promise((r) => setTimeout(() => r(null), 20000))])
writeFileSync(`${out}/transcript.json`, JSON.stringify({ turns, seconds: final?.usage?.seconds ?? null }, null, 2))
console.log("interviewer audio packets:", heard, "usage:", final?.usage)
for (const t of turns) console.log(`\n[${t.speaker} ${(t.startMs / 1000).toFixed(1)}s] ${t.text.trim()}`)
udp.close()
await pc.close()
process.exit(0)
