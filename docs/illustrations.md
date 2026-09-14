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

## onboarding

drawn by an openai model (`gpt-6-astra`) as isometric svg, then checked and flattened by `node tools/onboarding-art.mjs`. the prompt fixes the projection, the 3 unit ink outline and the app palette (three blue values, white and greys, one red accent) and bans gradients, text and css. the raw svgs live in `docs/art/onboarding-*.svg`.

the script folds every group transform into the path data, inherits fill and stroke down from the root, rejects anything it cannot draw, renders a png to tmp/claude/onboarding to look at, and writes `OnboardingArt.kt`. `SvgArtwork` draws them and builds each one back to front when its page settles.

```
cd tools && npm install
node onboarding-art.mjs                # uses the saved svgs
REDRAW=ready node onboarding-art.mjs   # ask the model again for one scene, or REDRAW=all
```

| scene | page |
|---|---|
| welcome | desk with laptop, phone, mic and speech bubbles |
| listen | phone with sound bars and stacked speech bubbles |
| report | clipboard with a bar chart and a check badge |
| ready | mic and padlock on a base, the profile and permission page |
