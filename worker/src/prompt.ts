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

interface Playbook {
  name: string
  opener: string
  // the interview in order, each step one main question with follow ups
  steps: string[]
  rules: string
}

// every round follows the same shape a real loop does, the interviewer adapts the wording to the answers
export const playbooks: Record<Round, Playbook> = {
  behavioral: {
    name: "behavioral",
    opener: "To start, tell me a little about yourself and what you are working on right now.",
    steps: [
      "Introduction: who they are and what they work on. Keep it short, pick one thing they mention to use later.",
      "Ownership: a project they drove end to end. Get their personal part, not the team's.",
      "Conflict: a disagreement with a teammate, lead or product manager and how it was resolved.",
      "Failure: something that went wrong because of them, and what they changed afterwards.",
      "Impact: how they knew their work mattered, with a number if they have one.",
      "Close: ask if they have a question for you, answer it in one sentence, then wrap up.",
    ],
    rules: "Push for a clear result in each story, situation, task, action and result. If a story stays vague, ask for one concrete moment.",
  },
  technical: {
    name: "Android technical",
    opener:
      "Which area should we dig into first: Jetpack Compose and UI, Kotlin coroutines and concurrency, app architecture and testing, or performance and memory?",
    steps: [
      "Area choice: wait for their pick. If they are unsure, choose coroutines and concurrency.",
      "Fundamentals in that area. Compose: recomposition and state hoisting. Coroutines: structured concurrency and cancellation. Architecture: layers, ViewModel and process death. Performance: jank, startup and memory leaks.",
      "Applied: a realistic screen or feature in that area, ask how they would build it.",
      "Debugging: describe a bug in that area, like a list that stutters, a leak, a coroutine that never cancels or state lost on rotation, and ask how they find and fix it.",
      "Tradeoff: two reasonable approaches, ask which they pick and why.",
      "Switch: if time is left, move to a second area with one fundamentals question.",
    ],
    rules: "Ask for code level detail when an answer stays at buzzwords, APIs, lifecycles, threads. Do not accept a name dropped without an explanation.",
  },
  design: {
    name: "mobile system design",
    opener:
      "Which app would you like to design together: a file storage app like Dropbox, a social feed like X, a video streaming app like YouTube, or a chat app like WhatsApp?",
    steps: [
      "App choice: wait for their pick. If they are unsure, choose the chat app.",
      "Requirements: let them ask clarifying questions and define the core features and scale. Answer briefly like a product owner.",
      "Client architecture: the layers and modules on the phone, and where state lives.",
      "Data and offline: local storage, what is cached, and how the app syncs when the network comes back.",
      "Deep dive for their pick. File storage: chunked, resumable uploads and conflicts. Feed: pagination, caching and ranking on the client. Streaming: adaptive bitrate, preloading and downloads. Chat: real time delivery, ordering and push.",
      "Failure and limits: poor networks, battery, a server outage.",
      "Tradeoffs and metrics: what they would measure after launch and what they would cut first.",
    ],
    rules: "Let the candidate drive, you steer. When they skip a hard part, bring them back to it. Keep them on the phone side, the backend only as far as the app needs.",
  },
  job: {
    name: "role specific",
    opener: "I have read the role you are going for. To start, what drew you to it?",
    steps: [
      "Motivation: why this role and this kind of company.",
      "Skills: pick the two most important skills from the job post and ask for real experience with each, one at a time.",
      "Scenario: turn one responsibility from the post into a situation they have to handle.",
      "Gaps: pick something the post asks for that they have not shown yet and ask how they would ramp up.",
      "Close: ask if they have a question about the role, then wrap up.",
    ],
    rules: "Everything should come back to the job post. Quote its words when you ask.",
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
  const book = playbooks[s.round]
  const lines = [
    `You are the interviewer in a ${book.name} mock interview for a ${s.role} candidate. ${tone[s.style]}`,
    "Ask one question at a time and keep your turns to one or two sentences. Keep backchannels short.",
    "Follow up on what the candidate actually said. Never coach, give answers or score them during the interview.",
    `Your opening already asked: "${book.opener}"`,
    "Run the interview in this order, moving on once a step has a real answer:",
    ...book.steps.map((step, i) => `${i + 1}. ${step}`),
    book.rules,
    depth[s.difficulty],
    `Pace it for about ${s.minutes} minutes. If time runs short, skip to the last step. At the end thank them and say their report is ready.`,
  ]
  if (s.round === "job" && s.jobPost) {
    lines.push(`Job post:\n${s.jobPost.slice(0, MAX_JOB_POST)}`)
  }
  return lines.join("\n")
}

// sent as session.commentary.append once the data channel opens. instructions.append only got the interviewer to
// speak first in about a third of test sessions, commentary did every time, about 0.9s after session.started
export function greeting(s: InterviewSetup): string {
  return `Greet the candidate now: Hi, thanks for joining. I am your interviewer today. ${playbooks[s.round].opener}`
}

export function roundName(round: Round) {
  return playbooks[round].name
}

export function isRound(v: unknown): v is Round {
  return typeof v === "string" && v in playbooks
}

export function isDifficulty(v: unknown): v is Difficulty {
  return v === "easy" || v === "medium" || v === "hard"
}

export function isStyle(v: unknown): v is Style {
  return v === "friendly" || v === "sharp"
}
