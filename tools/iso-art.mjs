// builds isometric scenes from boxes and top plane ellipses, writes an svg per scene and IsoScenes.kt
// true 30 degree isometric: +x -> (0.866, 0.5), +y -> (-0.866, 0.5), +z -> up
// usage: node tools/iso-art.mjs
import { writeFileSync, mkdirSync } from "node:fs"

const U = 12
const C30 = 0.866025, S30 = 0.5
const ink = "#1C1C1C"

const tones = {
  blue: { top: "#CFE2FC", x: "#7DB0F5", y: "#4989E9" },
  white: { top: "#FFFFFF", x: "#E6EDF7", y: "#CCD8E8" },
  floor: { top: "#EAF2FD", x: "#CFE0F8", y: "#B6CFF3" },
  dark: { top: "#4A4A4A", x: "#2E2E2E", y: "#1C1C1C" },
  red: { top: "#F27A86", x: "#E03143", y: "#B81F31" },
}

const P = (x, y, z) => [C30 * (x - y) * U, S30 * (x + y) * U - z * U]
const f = (n) => +n.toFixed(2)
const poly = (pts) => "M" + pts.map(([a, b]) => `${f(a)},${f(b)}`).join("L") + "Z"

// visible faces of an axis aligned box for this view: top, +x side, +y side
function box(x, y, z, w, d, h, tone, stroke = 1.4) {
  const t = tones[tone]
  return [
    { d: poly([P(x + w, y, z), P(x + w, y + d, z), P(x + w, y + d, z + h), P(x + w, y, z + h)]), fill: t.x, stroke },
    { d: poly([P(x, y + d, z), P(x + w, y + d, z), P(x + w, y + d, z + h), P(x, y + d, z + h)]), fill: t.y, stroke },
    { d: poly([P(x, y, z + h), P(x + w, y, z + h), P(x + w, y + d, z + h), P(x, y + d, z + h)]), fill: t.top, stroke },
  ]
}

// a circle of radius r on the plane z projects to rx = 1.225r, ry = 0.707r, no rotation
function disc(cx, cy, z, r, fill, stroke = 1.2, dash = false) {
  const [sx, sy] = P(cx, cy, z)
  const rx = 1.2247 * r * U, ry = 0.7071 * r * U
  const d = `M${f(sx - rx)},${f(sy)}A${f(rx)},${f(ry)} 0 1 0 ${f(sx + rx)},${f(sy)}A${f(rx)},${f(ry)} 0 1 0 ${f(sx - rx)},${f(sy)}Z`
  return [{ d, fill, stroke, dash }]
}

// a flat polygon lying on the plane z, points given in scene x,y
function flat(points, z, fill, stroke = 1.2) {
  return [{ d: poly(points.map(([x, y]) => P(x, y, z))), fill, stroke }]
}

// parts keep their own anchor (the screen point they scale or float around)
const part = (name, shapes, anchor = [0, 0, 0]) => ({ name, shapes: shapes.flat(), anchor: P(...anchor) })

const scenes = {
  hotseat: [
    part("floor", [box(-4, -4, -0.8, 22, 15, 0.8, "floor", 1.2)]),
    part("ring", [disc(3, 3, 0, 4.4, "none", 1.6, true)]),
    part("chair", [
      box(0.5, 0.5, 0, 0.5, 0.5, 2.8, "dark"),
      box(5, 0.5, 0, 0.5, 0.5, 2.8, "dark"),
      box(0.5, 5, 0, 0.5, 0.5, 2.8, "dark"),
      box(5, 5, 0, 0.5, 0.5, 2.8, "dark"),
      box(0, 0, 2.8, 6, 6, 1, "blue"),
      box(1.2, 0.9, 3.8, 4.6, 4.2, 0.5, "white", 1.2),
      box(1, 0, 3.8, 5, 0.8, 1.6, "blue"),
      box(0, 0, 3.8, 1.2, 6, 4.6, "blue"),
      box(1, 5.2, 3.8, 5, 0.8, 1.6, "blue"),
    ], [3, 3, 0]),
    part("table", [
      box(11.4, 3.4, 0, 2.6, 2.6, 0.4, "white"),
      box(12.2, 4.2, 0.4, 1, 1, 3.6, "white"),
      box(9.5, 1.5, 4, 6.4, 6.4, 0.6, "white"),
    ], [12.7, 4.7, 0]),
    ...[2.4, 3.6, 4.8, 6.0].map((y, i) =>
      part(`bar${i}`, [box(12.3, y, 4.6, 0.8, 0.8, [1.8, 3.6, 2.6, 1.4][i], i === 1 ? "red" : "blue", 1.2)], [12.7, y + 0.4, 4.6]),
    ),
    part("trail", [
      box(10.2, 3.2, 9.4, 0.7, 0.7, 0.7, "white", 1.1),
      box(9.4, 2.6, 10.6, 1, 1, 1, "white", 1.2),
    ], [9.9, 3.1, 9.4]),
    part("bubble", [
      box(6.5, -2.5, 12, 7, 5, 0.8, "white"),
      disc(8.5, 0, 12.8, 0.5, ink, 0),
      disc(10, 0, 12.8, 0.5, ink, 0),
      disc(11.5, 0, 12.8, 0.5, ink, 0),
    ], [10, 0, 12]),
  ],

  sessions: [
    part("floor", [box(-3, -3, -0.8, 18, 14, 0.8, "floor", 1.2)]),
    part("card0", [box(0, 0, 0, 12, 8, 0.8, "white")], [6, 4, 0]),
    part("card1", [box(0.5, 0.5, 0.8, 12, 8, 0.8, "white")], [6.5, 4.5, 0.8]),
    part("card2", [
      box(1, 1, 1.6, 12, 8, 0.8, "blue"),
      disc(3, 3, 2.4, 0.7, tones.red.x, 1.2),
      flat([[3, 6.2], [3, 8.2], [4.7, 7.2]], 2.4, ink, 0),
    ], [7, 5, 1.6]),
    ...[0, 1, 2, 3, 4, 5].map((i) => part(`wave${i}`, [box(6 + i * 1.1, 3.4, 2.4, 0.6, 0.6, [1, 2, 3, 1.6, 2.6, 1.2][i], "white", 1)], [6.3 + i * 1.1, 3.7, 2.4])),
  ],

  progress: [
    part("floor", [box(-2, -3, -0.8, 17, 12, 0.8, "floor", 1.2)]),
    ...[0, 1, 2, 3, 4].map((i) => part(`step${i}`, [box(i * 2.4, 0, 0, 2.4, 6, 1.6 + i * 1.8, i === 4 ? "blue" : "white")], [i * 2.4 + 1.2, 3, 0])),
    part("flag", [
      box(10.5, 1.2, 8.8, 0.35, 0.35, 5.2, "dark", 1),
      box(10.85, 1.2, 12, 2.6, 0.3, 1.8, "red", 1.2),
    ], [10.7, 1.4, 8.8]),
  ],

  you: [
    part("floor", [box(-3, -3, -0.8, 17, 13, 0.8, "floor", 1.2)]),
    part("badge", [
      box(0, 0, 0, 11, 8, 0.8, "white"),
      disc(3.2, 4, 0.8, 2, tones.blue.y, 1.4),
      disc(2.6, 3.4, 0.8, 0.8, tones.blue.top, 1.1),
      box(6, 2.2, 0.8, 4, 0.8, 0.3, "blue", 1),
      box(6, 4, 0.8, 3, 0.8, 0.3, "white", 1),
      box(6, 5.6, 0.8, 3.6, 0.8, 0.3, "white", 1),
    ], [5.5, 4, 0]),
    part("check", [disc(9.4, 1.4, 0.8, 0.9, tones.red.x, 1.2)], [9.4, 1.4, 0.8]),
  ],
}

mkdirSync("docs/art", { recursive: true })
const kotlin = []
for (const [name, parts] of Object.entries(scenes)) {
  // bounds from the move, line and arc end points, the padding covers ellipse bulges
  const xs = [], ys = []
  for (const p of parts) for (const s of p.shapes) for (const m of s.d.matchAll(/[MLA]?(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)/g)) { xs.push(+m[1]); ys.push(+m[2]) }
  const pad = 16
  const minX = Math.min(...xs) - pad, minY = Math.min(...ys) - pad - 12
  const w = Math.max(...xs) + pad - minX, h = Math.max(...ys) + pad - minY
  const svgParts = parts.map((p) =>
    `  <g id="${p.name}">\n` + p.shapes.map((s) =>
      `    <path d="${s.d}" fill="${s.fill}" stroke="${s.dash ? tones.red.x : ink}" stroke-width="${s.stroke}"${s.dash ? ' stroke-dasharray="4 4"' : ""} stroke-linejoin="round" stroke-linecap="round"/>`).join("\n") + "\n  </g>").join("\n")
  writeFileSync(`docs/art/${name}.svg`, `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${f(minX)} ${f(minY)} ${f(w)} ${f(h)}" width="${Math.round(w)}" height="${Math.round(h)}">\n${svgParts}\n</svg>\n`)

  const color = (c) => (c === "none" ? "null" : `0xFF${c.slice(1)}`)
  kotlin.push(`    val ${name[0].toUpperCase() + name.slice(1)} = IsoScene(
        ${f(minX)}f, ${f(minY)}f, ${f(w)}f, ${f(h)}f,
        listOf(
${parts.map((p) => `            IsoPart("${p.name}", ${f(p.anchor[0])}f, ${f(p.anchor[1])}f, listOf(\n${p.shapes.map((s) => `                IsoShape("${s.d}", ${color(s.fill)}, ${s.stroke}f${s.dash ? ", dashed = true" : ""}),`).join("\n")}\n            )),`).join("\n")}
        ),
    )`)
  console.log(name, parts.length, "parts", `${Math.round(w)}x${Math.round(h)}`)
}

writeFileSync("android/app/src/main/java/dev/mintu/hotseat/ui/art/IsoScenes.kt", `package dev.mintu.hotseat.ui.art

// generated by tools/iso-art.mjs, do not edit by hand

object IsoScenes {
${kotlin.join("\n\n")}
}
`)
