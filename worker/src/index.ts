interface Env {
  OPENAI_API_KEY?: string
}

const LIVE_URL = "https://api.openai.com/v1/live/sessions"

const INSTRUCTIONS =
  "You are a friendly but sharp job interviewer. Ask one question at a time, keep your turns short, and follow up on what the candidate actually said."

function json(body: unknown, status = 200) {
  return Response.json(body, { status })
}

async function createSession(req: Request, env: Env) {
  if (!env.OPENAI_API_KEY) {
    return json({ error: "OPENAI_API_KEY is not set" }, 503)
  }

  let sdp: unknown
  try {
    sdp = ((await req.json()) as { sdp?: unknown }).sdp
  } catch {
    return json({ error: "body must be json" }, 400)
  }
  if (typeof sdp !== "string" || !sdp.trim()) {
    return json({ error: "sdp offer is required" }, 400)
  }

  const res = await fetch(LIVE_URL, {
    method: "POST",
    headers: {
      authorization: `Bearer ${env.OPENAI_API_KEY}`,
      "content-type": "application/json",
    },
    body: JSON.stringify({
      session: {
        model: "gpt-live-1",
        instructions: INSTRUCTIONS,
      },
      transport: { type: "webrtc", sdp },
    }),
  })

  if (!res.ok) {
    const detail = await res.text()
    console.error("live session failed", res.status, detail)
    return json({ error: "live session failed", status: res.status }, 502)
  }

  const data = (await res.json()) as {
    session: { id: string }
    transport: { sdp: string }
  }
  return json({ id: data.session.id, sdp: data.transport.sdp }, 201)
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    const url = new URL(req.url)

    if (url.pathname === "/health") {
      return new Response("ok")
    }

    if (url.pathname === "/session") {
      if (req.method !== "POST") return json({ error: "method not allowed" }, 405)
      return createSession(req, env)
    }

    return new Response("not found", { status: 404 })
  },
}
