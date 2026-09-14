export type Round = "behavioral" | "technical" | "design" | "job"
export type Difficulty = "easy" | "medium" | "hard"
export type Style = "friendly" | "sharp"

export interface InterviewSetup {
  round: Round
  difficulty: Difficulty
  style: Style
  role: string
  jobPost?: string
  minutes: number
}

const rounds: Record<Round, { name: string; focus: string; opener: string }> = {
  behavioral: {
    name: "behavioral",
    focus: "ownership of a project, a disagreement, a failure and what changed after, and measuring impact",
    opener: "To start, tell me a little about yourself and what you are working on.",
  },
  technical: {
    name: "Android technical",
    focus: "Kotlin and coroutines, Jetpack Compose state and performance, lifecycle and process death, and testing",
    opener: "To warm up, tell me about the Android work you have done most recently.",
  },
  design: {
    name: "mobile system design",
    focus: "designing one app feature end to end: data model, offline behaviour, sync and failure cases, and what to measure",
    opener: "We will design something together. First, tell me about the most complex app you have shipped.",
  },
  job: {
    name: "role specific",
    focus: "the responsibilities and skills in the job post",
    opener: "To start, tell me why this role caught your eye.",
  },
}

const tone: Record<Style, string> = {
  friendly: "Warm and encouraging, but still honest.",
  sharp: "Direct and a little skeptical, like a senior interviewer at a top company.",
}

const depth: Record<Difficulty, string> = {
  easy: "Keep questions approachable and allow a partial answer before moving on.",
  medium: "Expect concrete examples and ask one follow up when an answer stays general.",
  hard: "Probe deeply, ask for tradeoffs and numbers, and push back when an answer has no result.",
}

export const MAX_JOB_POST = 3000

export function instructions(s: InterviewSetup): string {
  const r = rounds[s.round]
  const lines = [
    `You are the interviewer in a ${r.name} mock interview for a ${s.role} candidate. ${tone[s.style]}`,
    "Run it like a real interview. Ask one question at a time and keep your turns to one or two sentences.",
    "Follow up on what the candidate actually said instead of reading a list. Keep backchannels short.",
    `Cover about four main questions on ${r.focus}. ${depth[s.difficulty]}`,
    "Never coach, give answers or score the candidate during the interview.",
    `After about ${s.minutes} minutes, or once the questions are covered, thank them and say their report is ready.`,
  ]
  if (s.round === "job" && s.jobPost) {
    lines.push(`Job post:\n${s.jobPost.slice(0, MAX_JOB_POST)}`)
  }
  return lines.join("\n")
}

// GPT-Live only speaks first when handed the exact words and told to go
export function greeting(s: InterviewSetup): string {
  const r = rounds[s.round]
  const text = `Hi, thanks for joining. I am your interviewer today. ${r.opener}`
  return `Speak first, right now, before the candidate says anything. Say exactly: "${text}" Then stop and listen.`
}

export function roundName(round: Round) {
  return rounds[round].name
}

export function isRound(v: unknown): v is Round {
  return typeof v === "string" && v in rounds
}

export function isDifficulty(v: unknown): v is Difficulty {
  return v === "easy" || v === "medium" || v === "hard"
}

export function isStyle(v: unknown): v is Style {
  return v === "friendly" || v === "sharp"
}
