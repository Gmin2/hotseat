// connects to the local worker like the app would, logs every data channel event
// usage: node scripts/call.mjs [seconds] [events-out.jsonl]
import { writeFileSync, appendFileSync } from "node:fs"
import { RTCPeerConnection } from "werift"

const base = process.env.WORKER_URL ?? "http://127.0.0.1:8790"
const seconds = Number(process.argv[2] ?? 10)
const out = process.argv[3]
if (out) writeFileSync(out, "")

const pc = new RTCPeerConnection({
  bundlePolicy: "max-bundle",
  iceServers: [{ urls: "stun:stun.l.google.com:19302" }],
})
const audio = pc.addTransceiver("audio", { direction: "sendrecv" })
const events = pc.createDataChannel("oai-events")

let packets = 0
audio.onTrack.subscribe((track) => track.onReceiveRtp.subscribe(() => packets++))
pc.connectionStateChange.subscribe((s) => console.log("connection:", s))

events.onMessage.subscribe((raw) => {
  const text = raw.toString()
  if (out) appendFileSync(out, text + "\n")
  const event = JSON.parse(text)
  if (event.type.endsWith("transcript.delta")) process.stdout.write(event.type.startsWith("session.output") ? event.delta : `[you] ${event.delta}`)
  else console.log("\nevent:", event.type)
  // ask the interviewer to open the conversation, like the app will
  if (event.type === "session.started" && process.env.GREET) {
    events.send(JSON.stringify({ type: "session.instructions.append", delegation_id: null, content: process.env.GREET }))
  }
})

await pc.setLocalDescription(await pc.createOffer())
if (pc.iceGatheringState !== "complete") {
  await new Promise((resolve) =>
    pc.iceGatheringStateChange.subscribe((s) => s === "complete" && resolve()),
  )
}

const res = await fetch(`${base}/session`, {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({ sdp: pc.localDescription.sdp }),
})
const body = await res.json()
console.log("worker:", res.status, body.id ?? body)
if (!res.ok) {
  await pc.close()
  process.exit(1)
}

await pc.setRemoteDescription({ type: "answer", sdp: body.sdp })

await new Promise((r) => setTimeout(r, seconds * 1000))
console.log("audio packets received:", packets)
if (events.readyState === "open") events.send(JSON.stringify({ type: "session.close" }))
await new Promise((r) => setTimeout(r, 3000))
await pc.close()
process.exit(0)
