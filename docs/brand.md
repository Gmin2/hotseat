# brand

the mark is a rounded lowercase **h** that is also a chair seen from the side: the tall stroke is the backrest, the arch is the seat and front leg. the red dot above the seat is someone about to take the hot seat, and doubles as the live recording light.

`node tools/brand.mjs` generates everything from one set of numbers on a 24 grid:

| file | use |
|---|---|
| `docs/brand/mark.svg` | ink mark for light backgrounds |
| `docs/brand/mark-white.svg` | white mark for the sky or dark backgrounds |
| `docs/brand/icon.svg` | app tile, sky gradient with the white mark |
| `docs/brand/favicon.svg` | same tile at 32px for the web |
| `res/drawable/ic_launcher_*` + `mipmap-anydpi-v26/ic_launcher*` | adaptive launcher icon with a monochrome layer for themed icons |
| `ui/brand/Brand.kt` | the path and dot for compose |

where it shows up in the app:
- launcher icon
- system splash (`Theme.Hotseat.Starting`), no fade out
- `Intro`: starts from the splash mark, the dot hops and lands, the name types, then the screen lifts away
- status chip: the dot breathes during a live interview, tapping replays the draw in

colours: sky `#6793DF` to `#4989E9`, white mark, red `#E03143`.
