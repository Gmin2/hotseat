# illustrations

flat vector style to match the ink icons and the sky: sky blue, pale blue, white, near black details and one small red accent. generated with `gpt-image-2.5-sunburst` on a transparent background, trimmed to the art, shrunk to 768px and saved as webp in `drawable-nodpi`.

```
node tools/illustrate.mjs <name> "<subject>"
REUSE=1 node tools/illustrate.mjs <name> x   # redo trim and export from the last raw png, no api call
```

| name | where | subject |
|---|---|---|
| behavioral | round card | two speech bubbles in conversation, red sparkle |
| technical | round card | phone with code brackets on screen |
| system | round card | three connected boxes, an app architecture sketch |
| job | round card, You tab | job posting sheet with a pen |

the interviewer itself is not an image, it is the glowing orb drawn in `SkyBackdrop`, it swells with the voice level.

each high quality 1024 image takes about 40s.
