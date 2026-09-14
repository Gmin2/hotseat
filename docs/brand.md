# brand

the mascot is a round blue bird wearing a headset: your interviewer. drawn in a sticker style (thick ink outline, blue body, white face, blush cheeks, a red mic tip that doubles as the live light), based on the owl in the rewinder reference.

`node tools/mascot.mjs` generates everything from one geometry in a 512 box:

| file | use |
|---|---|
| `docs/brand/mascot.svg` | the bird on its sticker |
| `docs/brand/mascot-asleep.svg` | eyes shut, what the splash shows |
| `docs/brand/mascot-bare.svg` | no sticker, for light backgrounds |
| `docs/brand/icon.svg`, `favicon.svg` | app tile and web favicon |
| `res/drawable/ic_launcher_*`, `mipmap-anydpi-v26/ic_launcher*` | adaptive launcher icon, with a silhouette layer for themed icons |
| `res/drawable/ic_splash.xml` | the sleeping bird on its sticker for the system splash |
| `ui/brand/MascotArt.kt` | the paths, pivots and eye positions for compose |

in the app, `Mascot` draws it from that data so the eyes, wings, mic and hop can move:
- **launch**: the system splash shows the bird asleep, `Intro` takes over in the exact same spot and plays the owl's beats. half blink at 600ms, shut at 830, wakes with a hop and two flaps at 1050, glances left at 1450 and right at 1900, settles at 2350, the name types in and the screen lifts away at 3400
- **chip**: `IdleMascot` blinks on its own every few seconds, tapping makes it hop and flap, during a live interview its mic glows
- **chat**: the same bird is the interviewer's avatar, its mic glows while the interviewer is speaking

colours: blue `#3B7BEF`, ink `#141210`, red `#E03143`, beak `#FFB23E`, paper `#E6EBFF`, splash sky `#5A8EE4`.
