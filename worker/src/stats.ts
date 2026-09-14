export interface Turn {
  speaker: "interviewer" | "candidate"
  text: string
  startMs: number
  endMs: number
}

const FILLERS = /\b(um+|uh+|erm|you know|basically|kind of|sort of|i mean)\b|\blike,/gi

export function candidateStats(turns: Turn[]) {
  const mine = turns.filter((t) => t.speaker === "candidate")
  const words = mine.reduce((n, t) => n + (t.text.trim().match(/\S+/g)?.length ?? 0), 0)
  const speakingMs = mine.reduce((n, t) => n + Math.max(0, t.endMs - t.startMs), 0)
  const fillers = mine.reduce((n, t) => n + (t.text.match(FILLERS)?.length ?? 0), 0)
  const wpm = speakingMs > 5_000 ? Math.round(words / (speakingMs / 60_000)) : 0
  return { words, wpm, fillers }
}

export function clock(seconds: number) {
  const s = Math.max(0, Math.round(seconds))
  return `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`
}
