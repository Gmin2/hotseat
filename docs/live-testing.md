# testing live voice

voice has to be tested on a real phone. the emulator mic is a fake 8 kHz device, GPT-Live connects but never hears a
usable stream so the interviewer stays silent. the Nothing Phone (4a) works.

## run the worker and point the phone at it

```
cd worker && npx wrangler dev                 # 127.0.0.1:8790, needs OPENAI_API_KEY and APP_KEY in .dev.vars
adb -s <phone> reverse tcp:8790 tcp:8790      # the phone's 127.0.0.1:8790 is now the mac
```

debug builds use `http://127.0.0.1:8790` and app key `dev`. override with `-Photseat.workerUrl=... -Photseat.appKey=...`.

## probe a session without the ui (debug builds only)

```
adb shell pm grant dev.mintu.hotseat android.permission.RECORD_AUDIO
adb shell am start -S -n dev.mintu.hotseat/.MainActivity --ei probe 20 --es round technical
adb logcat -d | grep -E "HotseatProbe|HotseatLive"
```

logs the session id, every event, peak interviewer and mic levels, and the turn by turn transcript.
the phone has to be unlocked, a locked phone cannot use the mic.

## without a phone

`worker/scripts/interview.mjs` streams a wav file as the mic over webrtc through the same worker, see worker/README.md.

## how long the start takes

every live start logs a breakdown under `HotseatLive`:

```
adb logcat -s HotseatLive | grep timing
timing 655ms offer created
timing 723ms candidates gathered
timing 1400ms worker answered
timing 2668ms greeting sent
timing 2940ms session started
timing 3580ms first interviewer words
```

two things made the first question slow before. the greeting went out as `session.instructions.append`, which in node tests only got the interviewer to speak first in about one session in three, the rest stayed silent until the candidate talked. `session.commentary.append` spoke every time (7 of 7), about 0.9s after session.started. and the app waited up to 5s for ICE gathering against a STUN server, now it has no STUN (OpenAI's side is public, host candidates reach it) and gathering finishes in milliseconds.
