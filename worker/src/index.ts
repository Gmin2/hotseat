import { greeting, instructions, isRound } from "./prompt.ts"
import { outputText, reportRequest, shapeReport } from "./report.ts"
import { parseSetup, parseTurns } from "./validate.ts"

interface Limiter {
  limit(options: { key: string }): Promise<{ success: boolean }>
}

export interface Env {
  OPENAI_API_KEY?: string
  APP_KEY?: string
  SESSION_LIMIT?: Limiter
  REPORT_LIMIT?: Limiter
  fetchOpenAI?: typeof fetch
}

const OPENAI = "https://api.openai.com/v1"

const json = (body: unknown, status = 200) => Response.json(body, { status })

async function readJson(req: Request): Promise<Record<string, unknown> | null> {
  try {
    const body = await req.json()
    return body && typeof body === "object" && !Array.isArray(body) ? (body as Record<string, unknown>) : null
  } catch {
    return null
  }
}

function who(req: Request) {
  return req.headers.get("x-device-id")?.slice(0, 64) || req.headers.get("cf-connecting-ip") || "unknown"
}

async function limited(limiter: Limiter | undefined, key: string) {
  if (!limiter) return false
  const { success } = await limiter.limit({ key })
  return !success
}

function openai(env: Env, path: string, body: unknown) {
  const call = env.fetchOpenAI ?? fetch
  return call(`${OPENAI}${path}`, {
    method: "POST",
    headers: { authorization: `Bearer ${env.OPENAI_API_KEY}`, "content-type": "application/json" },
    body: JSON.stringify(body),
  })
}

async function createSession(req: Request, env: Env) {
  const body = await readJson(req)
  if (!body) return json({ error: "body must be a json object" }, 400)
  if (typeof body.sdp !== "string" || !body.sdp.trim()) return json({ error: "sdp offer is required" }, 400)
  const setup = parseSetup(body)
  if (typeof setup === "string") return json({ error: setup }, 400)
  if (await limited(env.SESSION_LIMIT, who(req))) return json({ error: "too many sessions, wait a minute" }, 429)

  const res = await openai(env, "/live/sessions", {
    session: { model: "gpt-live-1", instructions: instructions(setup) },
    transport: { type: "webrtc", sdp: body.sdp },
  })
  if (!res.ok) {
    console.error("live session failed", res.status, await res.text())
    return json({ error: "live session failed", status: res.status }, 502)
  }
  const data = (await res.json()) as { session: { id: string }; transport: { sdp: string } }
  return json(
    {
      id: data.session.id,
      sdp: data.transport.sdp,
      greeting: greeting(setup),
      maxSeconds: setup.minutes * 60 + 120,
    },
    201,
  )
}

async function createReport(req: Request, env: Env) {
  const body = await readJson(req)
  if (!body) return json({ error: "body must be a json object" }, 400)
  const round = body.round ?? "behavioral"
  if (!isRound(round)) return json({ error: "round must be behavioral, technical, design or job" }, 400)
  const turns = parseTurns(body.transcript)
  if (typeof turns === "string") return json({ error: turns }, 400)
  const seconds = typeof body.seconds === "number" && body.seconds > 0 ? body.seconds : (turns.at(-1)!.endMs / 1000)
  if (await limited(env.REPORT_LIMIT, who(req))) return json({ error: "too many reports, wait a minute" }, 429)

  const res = await openai(env, "/responses", reportRequest(round, turns))
  if (!res.ok) {
    console.error("report failed", res.status, await res.text())
    return json({ error: "report failed", status: res.status }, 502)
  }
  try {
    const raw = JSON.parse(outputText(await res.json()))
    return json(shapeReport(raw, turns, seconds))
  } catch (err) {
    console.error("report parse failed", err)
    return json({ error: "report could not be read" }, 502)
  }
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    const url = new URL(req.url)

    if (url.pathname === "/health") return new Response("ok")

    const routes: Record<string, (req: Request, env: Env) => Promise<Response>> = {
      "/session": createSession,
      "/report": createReport,
    }
    const route = routes[url.pathname]
    if (!route) return new Response("not found", { status: 404 })
    if (req.method !== "POST") return json({ error: "method not allowed" }, 405)
    if (!env.APP_KEY || req.headers.get("x-hotseat-key") !== env.APP_KEY) return json({ error: "unauthorized" }, 401)
    if (!env.OPENAI_API_KEY) return json({ error: "OPENAI_API_KEY is not set" }, 503)
    return route(req, env)
  },
}
