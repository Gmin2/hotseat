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
