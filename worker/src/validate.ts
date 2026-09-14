import { isDifficulty, isRound, isStyle, MAX_JOB_POST, type InterviewSetup } from "./prompt.ts"
import { MAX_TRANSCRIPT_CHARS } from "./report.ts"
import type { Turn } from "./stats.ts"

export const MAX_MINUTES = 20
const MIN_MINUTES = 5

export function parseSetup(body: Record<string, unknown>): InterviewSetup | string {
  const round = body.round ?? "behavioral"
  const difficulty = body.difficulty ?? "medium"
  const style = body.style ?? "friendly"
  if (!isRound(round)) return "round must be behavioral, technical, design or job"
  if (!isDifficulty(difficulty)) return "difficulty must be easy, medium or hard"
  if (!isStyle(style)) return "style must be friendly or sharp"
  const role = typeof body.role === "string" && body.role.trim() ? body.role.trim().slice(0, 60) : "mobile engineer"
  const jobPost = typeof body.jobPost === "string" ? body.jobPost.trim() : ""
  if (round === "job" && !jobPost) return "jobPost is required for the job round"
  if (jobPost.length > MAX_JOB_POST * 2) return `jobPost is too long, keep it under ${MAX_JOB_POST} characters`
  const minutes = typeof body.minutes === "number" ? Math.min(MAX_MINUTES, Math.max(MIN_MINUTES, Math.round(body.minutes))) : 15
  return { round, difficulty, style, role, jobPost: jobPost || undefined, minutes }
}

export function parseTurns(value: unknown): Turn[] | string {
  if (!Array.isArray(value) || value.length === 0) return "transcript must be a non empty array of turns"
  const turns: Turn[] = []
  let chars = 0
  for (const t of value) {
    if (!t || typeof t !== "object") return "each turn must be an object"
    const { speaker, text, startMs, endMs } = t as Record<string, unknown>
    if (speaker !== "interviewer" && speaker !== "candidate") return "speaker must be interviewer or candidate"
    if (typeof text !== "string") return "turn text must be a string"
    if (typeof startMs !== "number" || typeof endMs !== "number") return "startMs and endMs must be numbers"
    chars += text.length
    turns.push({ speaker, text, startMs, endMs })
  }
  if (chars > MAX_TRANSCRIPT_CHARS * 2) return "transcript is too long"
  if (!turns.some((t) => t.speaker === "candidate" && t.text.trim())) return "the candidate never spoke"
  return turns
}
