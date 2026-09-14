# design system

pulled from the reference clip (a weather app, 18s, 2160x2160 at 60fps). every number here was sampled from the video pixels, not eyeballed. the phone in the clip is a 393x852pt screen, so 1pt maps to 1dp.

the reference uses SF Pro. thats not licensed for android, so we use Inter, which is the closest match.

## layout

one full bleed screen, content pinned to the bottom half, a floating tab bar.

| element | x | y | size |
|---|---|---|---|
| status chip | 16 | 59 | 82 x 42, fully rounded |
| headline (first line cap top) | 24 | 146 | wraps, max ~3 lines |
| big number (cap top) | 24 | 537 | |
| arrow button | right, centre y 585 | 564 | ~56 x 42, fully rounded |
| meta line 1 (cap top) | 24 | 636 | |
| meta line 2 (cap top) | 24 | 664 | |
| tooltip pill | follows playhead | 657 | h 33, fully rounded |
| play button | 24 | 705 | 32 x 32 circle |
| tick scrubber | 76 to 370 | 709 to 734 | |
| tab bar | 22 inset each side | 773 | 349 x 54, radius 27 |
| active tab pill | 4 inset inside bar | 777 | 86 x 46, radius 23 |
| tab bar bottom gap | | | 25 to screen edge |

- side padding for text: **24**
- gap between meta line 1 and 2: cap tops **28** apart (12dp top padding on the caption with Inter)
- caption to tick row: **10**
- the big number sits ~7pt lower than the reference when stacked in a Column, because of Inter font padding. place it by baseline in the real screen
- play button to first tick: **26**

## type

| token | size | weight | line height | notes |
|---|---|---|---|---|
| display | 90 | regular | tight | the big number. digits cap 64pt, stroke ~0.087em |
| headline | 34 | medium | 46 | typed live, wraps |
| meta | 15 | medium | 20 | "Bengaluru · Mostly Cloudy" |
| caption | 12 | regular | 16 | "H 30° · L 22° · 56% Humidity". measured 11 on SF Pro, 12 on Inter matches the width |
| pill | 15 | semibold | 20 | "2 PM", estimated, the pill text was too small to measure cleanly |
| tab label | 10 | semibold | 12 | cap 7.3pt |

separator between meta items is a middle dot with an en space (U+2002) each side, a normal space is too tight

## colour

### day

| token | hex |
|---|---|
| sky top (y 30) | `#6793DF` |
| sky upper (y 250) | `#4989E9` |
| sky mid (y 450) | `#A5CAFA` |
| sky low (y 530) | `#B3D3F8` |
| sky floor (y 700 down) | `#FFFFFF` |
| text primary | `#000000` |
| text meta | `#3E3E3E` |
| text caption | `#848484` |
| tick | `#9A9A9A` |
| tick near playhead | `#292929` |
| playhead | `#E03143` |
| play button | `#292929`, glyph white |
| tooltip pill | `#1F1F1F`, text white |
| arrow button | `#F4FAFD` glass, glyph black |
| chip | `#7CC6FF`, top edge highlight `#A3EDFF`, glyphs `#040A19` |
| tab bar | `#FEFEFE` |
| tab active pill | `#E5E5E5` |
| tab active tint | `#0A64E4` |
| tab label | `#1C1C1C` |

### night

| token | hex |
|---|---|
| sky top | `#383B43` |
| sky upper | `#0E121D` |
| sky mid (clouds) | `#111423` to `#24293A` |
| sky low | `#11131D` |
| sky floor | `#040810` |
| text primary | `#FFFFFF` |
| display number | `#F3EAC8` warm cream |
| text meta | `#A6A8AE` |
| text caption | `#52545B` |
| tick | `#53555E` |
| play button | `#E6E5E9`, glyph black |
| tooltip pill | `#FFFFFF`, text black |
| arrow button | `#131519`, glyph white |
| chip | `#1A1E2C`, glyphs `#E7EAEF` |
| tab bar | `#191A21` |
| tab active pill | `#2E323A` |
| tab active tint | `#168AFF` |
| tab label | `#CFD1D9` |

### transition states

| state | top | upper | mid | low | floor |
|---|---|---|---|---|---|
| dusk flash | `#834C5C` | `#A7372F` | `#ED945D` | `#E68555` | `#BCB8B8` |
| overcast | `#7D8490` | `#747C8A` | `#616A7A` | `#5B6474` | `#4A5360` |

the tab bar follows the sky with a short lag. overcast bar is `#313F53` to `#434C5F`.

## surfaces

- **glass**: chip and arrow button are translucent white over the sky with a 1px lighter top edge. on night they flip to translucent dark
- **sky**: soft blurred clouds plus a sun glow that drifts. the sky fades into a solid floor colour from about y 600, so the bottom content always sits on a calm flat area
- **tab bar**: solid, soft wide shadow, no border

## scrubber

- 45 ticks, pitch **6.2**, tick width **1.5**
- resting tick height **11.6**
- ticks swell near the playhead like a dock: 24.7 at the playhead, then 19.9, 18, 16, 14, back to 11.6 over 4 ticks each side
- playhead is a red tick, the ticks around it go dark
- tooltip pill rides above the playhead, centred on it
- in the reference 1 tick = 30 minutes. for us 1 tick = a slice of the interview timeline

## motion

| what | measured | compose |
|---|---|---|
| playhead travel | 76 to 370 over ~14.5s, ~20pt/s, linear | `tween(easing = LinearEasing)` driven by playback time |
| headline typing | ~60 chars in 0.8s, ~75 chars/s, newest chars lighter then settle | append per delta, each new chunk `fadeIn(tween(180))` |
| headline swap | typed sentence fades out over ~0.4s, short label replaces it | `Crossfade(tween(400))` |
| day to dusk to night | 4.62 to 5.32s, 0.70s | animate sky palette, `tween(700, FastOutSlowInEasing)` |
| night to overcast | 11.64 to 11.94s, 0.30s | `tween(300)` |
| overcast to day | 12.95 to 13.05s, ~0.10 to 0.5s | `tween(400)` |
| play to pause icon | instant at 1.10s | swap, no animation |
| end of playback | play button becomes replay | swap |
| big number | digits change as time moves | per digit `AnimatedContent` slide up |
| tab bar colour | lags the sky ~0.5s | same tween, 500ms delay |

## checking it

`SpecimenTest` renders a static day and night screen from these tokens with Roborazzi into `android/app/build/specimen/`. put it next to a frame from the clip to compare. current renders are in `docs/design/`.

the live app goes further than the specimen: `SkyBackdrop` draws drifting cloud puffs, stars at night and the interviewer orb, icons are nucleo micro-bold drawn by `InkIcon` with a tap motion each. screenshots of every screen are in `docs/design/mock/`.

## mapping to hotseat

| reference | hotseat |
|---|---|
| sky state (day, dusk, night, overcast) | interviewer speaking is day, your answer is night, a pushback flashes dusk then sits overcast |
| sun | the interviewer orb, swells while it talks |
| typed headline | live interviewer transcript |
| big number | elapsed time live, score on the report |
| meta line 1 | role · round |
| meta line 2 | question · pace · fillers |
| scrubber | mic level live, replay timeline after |
| tabs Nearby, Cities, Rankings, More | Practice, Sessions, Progress, profile and settings in a panel from the top right button |
