// the hotseat mascot: a round blue bird wearing a headset
// one geometry, written out as web svgs, the android launcher and splash drawables, and MascotArt.kt for compose
// usage: node tools/mascot.mjs
import { writeFileSync, mkdirSync, rmSync, existsSync } from "node:fs"

const C = {
  blue: "#3B7BEF",
  ink: "#141210",
  eye: "#0F0C07",
  red: "#E03143",
  white: "#FFFFFF",
  beak: "#FFB23E",
  paper: "#E6EBFF",
  skyTop: "#6793DF",
  skyBottom: "#4989E9",
}

const f = (n) => +n.toFixed(2)
const rad = (deg) => (deg * Math.PI) / 180

// an ellipse as two arcs, optionally turned by deg around its own centre
function ellipse(cx, cy, rx, ry, deg = 0) {
  const dx = rx * Math.cos(rad(deg)), dy = rx * Math.sin(rad(deg))
  const a = `${f(cx - dx)},${f(cy - dy)}`, b = `${f(cx + dx)},${f(cy + dy)}`
  return `M${a}A${rx},${ry} ${f(deg)} 1 0 ${b}A${rx},${ry} ${f(deg)} 1 0 ${a}Z`
}

function rect(x, y, w, h, r) {
  return `M${x + r},${y}H${x + w - r}A${r},${r} 0 0 1 ${x + w},${y + r}V${y + h - r}A${r},${r} 0 0 1 ${x + w - r},${y + h}H${x + r}A${r},${r} 0 0 1 ${x},${y + h - r}V${y + r}A${r},${r} 0 0 1 ${x + r},${y}Z`
}

function rotateAround([x, y], [px, py], deg) {
  const c = Math.cos(rad(deg)), s = Math.sin(rad(deg))
  return [px + (x - px) * c - (y - py) * s, py + (x - px) * s + (y - py) * c]
}

// wings hang from a shoulder and tilt out a little, the flap turns them further around the shoulder
function wing(side) {
  const sx = side < 0 ? 146 : 366
  const tilt = side * -14
  const [cx, cy] = rotateAround([sx - side * 8, 316], [sx, 316], tilt)
  return ellipse(cx, cy, 36, 62, tilt)
}

export const pivots = { wingL: [146, 262], wingR: [366, 262] }
export const eyes = { left: 222, right: 290, y: 250, w: 20, h: 24, catchlight: 7 }
export const sticker = { cx: 256, cy: 266, rx: 204, ry: 206, glowX: 256, glowY: 248, glowR: 212 }

// back to front. "eyes" is where the eyes go, they are drawn by code so they can blink and look around
export const shapes = [
  { layer: "wingL", d: wing(-1), fill: C.blue, stroke: C.ink, width: 12 },
  { layer: "wingR", d: wing(1), fill: C.blue, stroke: C.ink, width: 12 },
  { layer: "feet", d: "M232,404V430M280,404V430M220,430H244M268,430H292", stroke: C.ink, width: 10 },
  { layer: "body", d: ellipse(256, 282, 136, 128), fill: C.blue, stroke: C.ink, width: 12 },
  { layer: "face", d: ellipse(256, 276, 96, 86), fill: C.white },
  { layer: "eyes" },
  { layer: "cheeks", d: ellipse(196, 298, 15, 9) + ellipse(316, 298, 15, 9), fill: C.red, alpha: 0.5 },
  { layer: "beak", d: "M234,284Q256,274 278,284Q266,314 256,316Q246,314 234,284Z", fill: C.beak, stroke: C.ink, width: 8 },
  { layer: "band", d: "M120,262C120,100 392,100 392,262", stroke: C.ink, width: 18 },
  { layer: "cups", d: rect(96, 226, 44, 82, 20) + rect(372, 226, 44, 82, 20), fill: C.ink },
  { layer: "arm", d: "M394,300C398,352 360,372 318,366", stroke: C.ink, width: 10 },
  { layer: "mic", d: ellipse(310, 366, 16, 16), fill: C.red, stroke: C.ink, width: 8 },
]

function eyePaths(open) {
  if (open < 0.15) {
    const lid = (cx) => `M${cx - eyes.w},${eyes.y}Q${cx},${eyes.y + 14} ${cx + eyes.w},${eyes.y}`
    return [{ d: lid(eyes.left) + lid(eyes.right), stroke: C.eye, width: 8 }]
  }
  const ball = (cx) => ellipse(cx, eyes.y, eyes.w, eyes.h)
  const light = (cx) => ellipse(cx - 7, eyes.y - eyes.h * 0.4, eyes.catchlight, eyes.catchlight)
  return [
    { d: ball(eyes.left) + ball(eyes.right), fill: C.eye },
    { d: light(eyes.left) + light(eyes.right), fill: C.white },
  ]
}

const flat = (open) => shapes.flatMap((s) => (s.layer === "eyes" ? eyePaths(open) : [s]))

// ---- svg ----

function svgShape(s, mono = false) {
  const fill = s.fill ? (mono ? "#000" : s.fill) : "none"
  const stroke = s.stroke ? ` stroke="${mono ? "#000" : s.stroke}" stroke-width="${s.width}" stroke-linecap="round" stroke-linejoin="round"` : ""
  const alpha = s.alpha ? ` fill-opacity="${s.alpha}"` : ""
  return `  <path d="${s.d}" fill="${fill}"${alpha}${stroke}/>`
}

const stickerSvg = `  <defs><radialGradient id="paper" cx="${sticker.glowX}" cy="${sticker.glowY}" r="${sticker.glowR}" gradientUnits="userSpaceOnUse"><stop offset="0.3" stop-color="#fff"/><stop offset="1" stop-color="${C.paper}"/></radialGradient></defs>
  <path d="${ellipse(sticker.cx, sticker.cy, sticker.rx, sticker.ry)}" fill="url(#paper)"/>`

const doc = (w, h, body, box = "0 0 512 512") => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${box}" width="${w}" height="${h}">\n${body}\n</svg>\n`

// ---- android vector drawables ----

const hex = (c, alpha = 1) => "#" + Math.round(alpha * 255).toString(16).padStart(2, "0").toUpperCase() + c.slice(1).toUpperCase()

function vdShape(s, mono = false) {
  const attrs = [`android:pathData="${s.d}"`]
  if (s.fill) attrs.push(`android:fillColor="${mono ? "#FF000000" : hex(s.fill, s.alpha ?? 1)}"`)
  if (s.stroke) attrs.push(`android:strokeColor="${mono ? "#FF000000" : hex(s.stroke)}"`, `android:strokeWidth="${s.width}"`, `android:strokeLineCap="round"`, `android:strokeLineJoin="round"`)
  return `        <path ${attrs.join(" ")} />`
}

// fits a 512 box region into the 108 unit adaptive canvas: centre (cx, cy) lands in the middle at scale k
const group = (k, cx, cy, inner) =>
  `    <group android:scaleX="${f(k)}" android:scaleY="${f(k)}" android:translateX="${f(54 - cx * k)}" android:translateY="${f(54 - cy * k)}">\n${inner}\n    </group>`

const vector = (inner, gradients = false) => `<vector xmlns:android="http://schemas.android.com/apk/res/android"${gradients ? '\n    xmlns:aapt="http://schemas.android.com/aapt"' : ""}
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
${inner}
</vector>
`

const stickerVd = `        <path android:pathData="${ellipse(sticker.cx, sticker.cy, sticker.rx, sticker.ry)}">
            <aapt:attr name="android:fillColor">
                <gradient android:type="radial" android:centerX="${sticker.glowX}" android:centerY="${sticker.glowY}" android:gradientRadius="${sticker.glowR}">
                    <item android:offset="0.3" android:color="#FFFFFFFF" />
                    <item android:offset="1" android:color="${hex(C.paper)}" />
                </gradient>
            </aapt:attr>
        </path>`

// launcher: the bird fills the 66 unit safe circle, splash: the sticker fits inside the 72 unit circle android keeps
export const launcher = { k: 0.2, cx: 256, cy: 283 }
export const splash = { k: 0.17, cx: sticker.cx, cy: sticker.cy }

// ---- write ----

const awake = flat(1)
const asleep = flat(0)

mkdirSync("docs/brand", { recursive: true })
for (const old of ["mark.svg", "mark-white.svg"]) if (existsSync(`docs/brand/${old}`)) rmSync(`docs/brand/${old}`)
writeFileSync("docs/brand/mascot.svg", doc(512, 512, stickerSvg + "\n" + awake.map((s) => svgShape(s)).join("\n")))
writeFileSync("docs/brand/mascot-asleep.svg", doc(512, 512, stickerSvg + "\n" + asleep.map((s) => svgShape(s)).join("\n")))
writeFileSync("docs/brand/mascot-bare.svg", doc(512, 512, awake.map((s) => svgShape(s)).join("\n")))
const tile = (size) => doc(size, size, `  <defs><radialGradient id="paper" cx="54" cy="50" r="64" gradientUnits="userSpaceOnUse"><stop offset="0.3" stop-color="#fff"/><stop offset="1" stop-color="${C.paper}"/></radialGradient></defs>
  <rect width="108" height="108" rx="24" fill="url(#paper)"/>
  <g transform="translate(${f(54 - launcher.cx * launcher.k)} ${f(54 - launcher.cy * launcher.k)}) scale(${launcher.k})">
${awake.map((s) => svgShape(s)).join("\n")}
  </g>`, "0 0 108 108")
writeFileSync("docs/brand/icon.svg", tile(512))
writeFileSync("docs/brand/favicon.svg", tile(32))

const res = "android/app/src/main/res"
mkdirSync(`${res}/drawable`, { recursive: true })
mkdirSync(`${res}/mipmap-anydpi-v26`, { recursive: true })
writeFileSync(`${res}/drawable/ic_launcher_foreground.xml`, vector(group(launcher.k, launcher.cx, launcher.cy, awake.map((s) => vdShape(s)).join("\n"))))
writeFileSync(`${res}/drawable/ic_splash.xml`, vector(group(splash.k, splash.cx, splash.cy, stickerVd + "\n" + asleep.map((s) => vdShape(s)).join("\n")), true))

// the themed icon is a silhouette: the face is cut out of the body so the eyes and beak still read
const monoShapes = awake
  .filter((s) => !["face", "cheeks"].includes(s.layer) && s.fill !== C.white)
  .map((s) => (s.layer === "body" ? { ...s, d: shapes.find((x) => x.layer === "body").d + shapes.find((x) => x.layer === "face").d, stroke: null, evenOdd: true } : s))
writeFileSync(`${res}/drawable/ic_launcher_monochrome.xml`, vector(group(launcher.k, launcher.cx, launcher.cy, monoShapes.map((s) => vdShape(s, true).replace(" />", s.evenOdd ? ' android:fillType="evenOdd" />' : " />")).join("\n"))))
writeFileSync(`${res}/drawable/ic_launcher_background.xml`, vector(`    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient android:type="radial" android:centerX="54" android:centerY="50" android:gradientRadius="64">
                <item android:offset="0.3" android:color="#FFFFFFFF" />
                <item android:offset="1" android:color="${hex(C.paper)}" />
            </gradient>
        </aapt:attr>
    </path>`, true))

const color = (c, a = 1) => (c ? `0x${hex(c, a).slice(1)}` : "null")
writeFileSync("android/app/src/main/java/dev/mintu/hotseat/ui/brand/MascotArt.kt", `package dev.mintu.hotseat.ui.brand

// generated by tools/mascot.mjs, do not edit by hand

object MascotArt {
    const val BOX = 512f

    val shapes = listOf(
${shapes.map((s) => s.layer === "eyes"
    ? `        MascotShape("eyes", "", null, null, 0f),`
    : `        MascotShape("${s.layer}", "${s.d}", ${color(s.fill, s.alpha)}, ${color(s.stroke)}, ${s.width ?? 0}f),`).join("\n")}
    )

    const val WING_L_X = ${pivots.wingL[0]}f
    const val WING_L_Y = ${pivots.wingL[1]}f
    const val WING_R_X = ${pivots.wingR[0]}f
    const val WING_R_Y = ${pivots.wingR[1]}f

    const val EYE_L = ${eyes.left}f
    const val EYE_R = ${eyes.right}f
    const val EYE_Y = ${eyes.y}f
    const val EYE_W = ${eyes.w}f
    const val EYE_H = ${eyes.h}f
    const val CATCHLIGHT = ${eyes.catchlight}f
    const val EYE_INK = ${color(C.eye)}

    const val MIC_X = 310f
    const val MIC_Y = 366f

    const val STICKER_CX = ${sticker.cx}f
    const val STICKER_CY = ${sticker.cy}f
    const val STICKER_RX = ${sticker.rx}f
    const val STICKER_RY = ${sticker.ry}f
    const val STICKER_GLOW_X = ${sticker.glowX}f
    const val STICKER_GLOW_Y = ${sticker.glowY}f
    const val STICKER_GLOW_R = ${sticker.glowR}f
    const val PAPER = ${color(C.paper)}

    // the splash drawable is 108 units shown at 288dp, this is how big the 512 box ends up on screen
    const val SPLASH_BOX_DP = ${f(512 * splash.k * 288 / 108)}f
    const val SKY_TOP = ${color(C.skyTop)}
    const val SKY_BOTTOM = ${color(C.skyBottom)}
}
`)
console.log("wrote docs/brand, launcher and splash drawables, MascotArt.kt")
