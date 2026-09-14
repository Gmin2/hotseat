# Hotseat

practice job interviews out loud. a voice interviewer asks, listens and follows up, then you get a report on every answer.

<p align="center">
  <img src="media/demo.gif" width="320" alt="hotseat demo">
</p>

<p align="center"><a href="media/demo.mp4">watch the sharper mp4</a></p>

- pick a round: behavioral, android technical, system design or one built from a job post
- talk it through, the interviewer runs a real interview pattern and digs into what you said
- tap the play button beside any question to hear it again
- when you end, you get a score, a STAR breakdown and a better way to say each answer

built with kotlin and jetpack compose on android. the voice runs on openai gpt-live over webrtc, through a small cloudflare worker that holds the key.
