# hotseat worker

cloudflare worker between the app and openai. the api key never leaves it.

## routes

every POST needs `x-hotseat-key` (the `APP_KEY` secret) and should send `x-device-id` for rate limits.

**`POST /session`** starts a GPT-Live interview
```json
{ "sdp": "<webrtc offer>", "round": "behavioral|technical|design|job", "difficulty": "easy|medium|hard",
  "style": "friendly|sharp", "role": "Android engineer", "minutes": 15, "jobPost": "only for the job round" }
```
returns `201 { id, sdp, greeting, maxSeconds }`. apply `sdp` as the answer, and once the `oai-events` data channel opens send
`{ "type": "session.commentary.append", "delegation_id": null, "content": greeting }` so the interviewer speaks first. commentary is what makes it reliable, instructions.append often left the interviewer silent until the candidate spoke.

every round follows a playbook in `src/prompt.ts`, the same order a real loop uses:

- behavioral: intro, ownership, conflict, failure, impact, close
- technical: the candidate picks compose, coroutines, architecture or performance, then fundamentals, an applied feature, a debugging case and a tradeoff
- design: the candidate picks file storage, a feed like X, video streaming or chat, then requirements, client architecture, offline and sync, a deep dive for that app, failure cases and metrics
- job: motivation, two skills from the post, a scenario from it, gaps, close

the greeting asks the choice question, so the first thing the candidate does is pick.
the app should close the session at `maxSeconds`.

**`POST /report`** scores a finished interview with `gpt-5.6-luna`
```json
{ "round": "technical", "seconds": 67, "transcript": [{ "speaker": "interviewer|candidate", "text": "...", "startMs": 0, "endMs": 2000 }] }
```
returns `{ score, summary, duration, wpm, fillers, answers: [{ question, score, star: [4 numbers 0..1], note, better }] }`.
pace and fillers are counted here, not by the model, and every field is clamped to the lengths the report screen expects.

errors: 400 bad body, 401 bad key, 405 not POST, 429 rate limited (3 sessions, 5 reports per device per minute), 502 openai failed, 503 no key.

## run and test

```
npm test                                  # unit tests, node --test
npx wrangler dev                          # http://127.0.0.1:8790, secrets from .dev.vars (OPENAI_API_KEY, APP_KEY)
node scripts/interview.mjs <candidate.wav> <out dir> '{"round":"technical"}'   # real voice interview end to end
```

`interview.mjs` streams a wav file as the microphone over webrtc, the same path the phone uses, and saves every event plus the turn by turn transcript. `test/fixtures/technical-session.*` is one real run: events, transcript and the report made from it.

## deploy

```
npx wrangler login
npx wrangler secret put OPENAI_API_KEY
npx wrangler secret put APP_KEY
npx wrangler deploy
```

deployed at `https://hotseat-worker.mintugogoi567.workers.dev`. `OPENAI_API_KEY` and `APP_KEY` are worker secrets, check them with `npx wrangler secret list`. to change one run `npx wrangler secret put APP_KEY`, then `npx wrangler deploy`.

the app reads the url and key from `android/local.properties`, which stays out of git:

```
hotseat.workerUrl=https://hotseat-worker.mintugogoi567.workers.dev
hotseat.appKey=<the APP_KEY secret>
```

drop those two lines to go back to wrangler dev through `adb reverse`, or pass `-Photseat.workerUrl=http://127.0.0.1:8790 -Photseat.appKey=dev` for one build. the app key ships inside the apk, so it only keeps casual traffic out, the openai budget limit is the real cap.
