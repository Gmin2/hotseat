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

## isometric scenes

computed, not generated. `node tools/iso-art.mjs` builds each scene from boxes, flat polygons and top plane ellipses in true 30 degree isometric (ellipse ry/rx 0.577), writes `docs/art/<scene>.svg` and `IsoScenes.kt`. `IsoArt` draws them in compose part by part, so each named part can float or scale. scaling a part vertically around its anchor is exact in isometric, it only stretches height.

style: one hue at three values keyed to face orientation (top lightest), near black outlines, a red accent.

| scene | tab | what it is | motion |
|---|---|---|---|
| hotseat | Practice | chair on a red hot seat ring, voice bars on a table trailing up into a speech bubble | bars jump with the interviewer voice, bubble floats, chair squashes on tap |
| sessions | Sessions | stack of recorded session cards, play mark, record dot, waveform | cards drop in, waveform breathes |
| progress | Progress | rising steps with a red flag on top | steps grow one after another, flag sways |
| you | You | profile badge with avatar and a red verified dot | badge floats, dot pops |

each high quality 1024 image takes about 40s.
