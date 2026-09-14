import { roundName, type Round } from "./prompt.ts"
import { candidateStats, clock, type Turn } from "./stats.ts"

export const REPORT_MODEL = "gpt-5.6-luna"
export const MAX_TRANSCRIPT_CHARS = 60_000

export interface Answer {
  question: string
  score: number
  star: number[]
  note: string
  better: string
}

export interface Report {
  score: number
  summary: string
  duration: string
  wpm: number
  fillers: number
  answers: Answer[]
}

const schema = {
  type: "object",
  additionalProperties: false,
  required: ["score", "summary", "answers"],
  properties: {
    score: { type: "integer", description: "overall 0 to 100" },
    summary: { type: "string", description: "one sentence, at most 80 characters, the single most useful takeaway" },
    answers: {
      type: "array",
      description: "one entry per main interviewer question, in order, at most 6",
      items: {
        type: "object",
        additionalProperties: false,
        required: ["question", "score", "star", "note", "better"],
        properties: {
          question: { type: "string", description: "short title for the question, at most 40 characters" },
          score: { type: "integer", description: "0 to 100" },
          star: {
            type: "array",
            description: "exactly 4 numbers from 0 to 1: situation or context, task or goal, action or approach, result or impact",
            items: { type: "number" },
          },
          note: { type: "string", description: "what worked and what was missing, at most 110 characters" },
          better: { type: "string", description: "a concrete rewrite or next step using their own story, at most 130 characters" },
        },
      },
    },
  },
}

export function reportRequest(round: Round, turns: Turn[]) {
  let transcript = turns.map((t) => `${t.speaker}: ${t.text.trim()}`).join("\n")
  if (transcript.length > MAX_TRANSCRIPT_CHARS) transcript = transcript.slice(-MAX_TRANSCRIPT_CHARS)
  return {
    model: REPORT_MODEL,
    input: [
      {
        role: "developer",
        content:
          `You score a ${roundName(round)} mock interview for the candidate. Be honest and specific, quote their story, never invent facts. ` +
          "Ignore greetings and small talk. A question the candidate never answered scores low. Keep every field within its length.",
      },
      { role: "user", content: transcript },
    ],
    text: { format: { type: "json_schema", name: "report", strict: true, schema } },
  }
}

const clamp = (n: unknown, lo: number, hi: number) => Math.min(hi, Math.max(lo, typeof n === "number" && Number.isFinite(n) ? n : lo))
const cut = (s: unknown, max: number) => (typeof s === "string" ? (s.length > max ? s.slice(0, max - 1).trimEnd() + "…" : s) : "")

// the raw api has no output_text, the json lives in the message item
export function outputText(response: any): string {
  const message = response?.output?.find((o: any) => o.type === "message")
  const part = message?.content?.find((c: any) => c.type === "output_text")
  if (typeof part?.text !== "string") throw new Error("no output text in response")
  return part.text
}

// the model is asked for ranges and lengths, this makes sure the app never gets anything outside them
export function shapeReport(raw: any, turns: Turn[], seconds: number): Report {
  const stats = candidateStats(turns)
  const answers = (Array.isArray(raw?.answers) ? raw.answers : []).slice(0, 6).map((a: any): Answer => {
    let star = (Array.isArray(a?.star) ? a.star : []).slice(0, 4).map((v: unknown) => clamp(v, 0, 1))
    // some replies use a 0 to 4 or 0 to 5 scale, bring those back to 0 to 1
    if (Array.isArray(a?.star) && a.star.some((v: unknown) => typeof v === "number" && v > 1)) {
      const top = Math.max(...a.star.filter((v: unknown) => typeof v === "number")) > 4 ? 5 : 4
      star = a.star.slice(0, 4).map((v: unknown) => clamp(typeof v === "number" ? v / top : 0, 0, 1))
    }
    while (star.length < 4) star.push(0)
    return {
      question: cut(a?.question, 44),
      score: Math.round(clamp(a?.score, 0, 100)),
      star,
      note: cut(a?.note, 140),
      better: cut(a?.better, 160),
    }
  })
  return {
    score: Math.round(clamp(raw?.score, 0, 100)),
    summary: cut(raw?.summary, 100),
    duration: clock(seconds),
    wpm: stats.wpm,
    fillers: stats.fillers,
    answers,
  }
}
