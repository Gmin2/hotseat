// onboarding illustrations: an openai model draws each scene as isometric svg, this checks it, renders a png to look at,
// flattens every transform into plain paths and writes OnboardingArt.kt for compose
// usage: node tools/onboarding-art.mjs            (REDRAW=name to ask the model again for one scene)
import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs"
import { parse } from "svg-parser"
import svgpath from "svgpath"
import { Resvg } from "@resvg/resvg-js"

const MODEL = "gpt-6-astra"
const root = new URL("..", import.meta.url).pathname

export const scenes = [
  { name: "welcome", subject: "A small desk with an open laptop, a smartphone standing upright beside it and a round desk microphone on a short stand. Two flat speech bubble slabs float above the desk, one larger than the other." },
  { name: "listen", subject: "A smartphone lying flat with sound wave bars of different heights rising out of its screen, and two flat speech bubble slabs stacked above it, the top one with three small round dots on it." },
  { name: "report", subject: "A clipboard lying flat with a bar chart of four bars standing up from the page, the tallest bar is the red accent, plus a round badge with a check mark beside it." },
  { name: "ready", subject: "A round desk microphone on a stand next to a closed padlock, both on a small square base slab." },
]

const prompt = `You draw isometric product illustrations as SVG. Reply with one complete SVG document and nothing else: no prose, no markdown fence, no explanation.

Canvas: viewBox="0 0 400 400". Keep every part inside it with at least 20 units of empty margin, centred.

Projection: true 30 degree isometric. The two horizontal axes run along (0.866, 0.5) and (-0.866, 0.5); vertical edges stay vertical on screen. Every face lies on one of those three planes. Squares in 3D must come out as rhombi, never as rectangles. A circle on a top face is an ellipse with ry/rx = 0.577.

Line: outline every solid with a uniform 3 unit stroke in #141210. Interior detail lines are 1.5 units. Rounded caps and joins.

Fill: flat only. Blue is the only hue: top faces #CFE2FC, one side #7DB0F5, the other side #4989E9. Neutral parts use white #FFFFFF on top and #E6EDF7 / #CCD8E8 on the sides. Exactly one small accent in red #E03143. No other colours.

Never use: gradients, filters, <text>, <image>, <foreignObject>, <style>, CSS classes, scripts, animation, external references. Use presentation attributes only.

Draw a real object with recognisable parts. Between 30 and 120 elements.`

const key = process.env.OPENAI_API_KEY ?? readFileSync(`${root}worker/.dev.vars`, "utf8").match(/^OPENAI_API_KEY=(.*)$/m)?.[1]

async function draw(subject) {
  const res = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
    body: JSON.stringify({ model: MODEL, input: [{ role: "developer", content: prompt }, { role: "user", content: subject }] }),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(JSON.stringify(body.error ?? body))
  const text = body.output.find((o) => o.type === "message").content.find((c) => c.type === "output_text").text
  const svg = text.slice(text.indexOf("<svg"), text.lastIndexOf("</svg>") + 6)
  if (!svg.startsWith("<svg")) throw new Error("model did not return an svg")
  return svg
}

// ---- flattening ----

const multiply = (a, b) => [
  a[0] * b[0] + a[2] * b[1], a[1] * b[0] + a[3] * b[1],
  a[0] * b[2] + a[2] * b[3], a[1] * b[2] + a[3] * b[3],
  a[0] * b[4] + a[2] * b[5] + a[4], a[1] * b[4] + a[3] * b[5] + a[5],
]
const IDENTITY = [1, 0, 0, 1, 0, 0]

function transform(value = "") {
  let m = IDENTITY
  for (const [, op, args] of value.matchAll(/(\w+)\s*\(([^)]*)\)/g)) {
    const n = args.trim().split(/[\s,]+/).map(Number)
    const rad = (d) => (d * Math.PI) / 180
    let t
    if (op === "matrix") t = n
    else if (op === "translate") t = [1, 0, 0, 1, n[0], n[1] ?? 0]
    else if (op === "scale") t = [n[0], 0, 0, n[1] ?? n[0], 0, 0]
    else if (op === "rotate") {
      const c = Math.cos(rad(n[0])), s = Math.sin(rad(n[0]))
      t = [c, s, -s, c, 0, 0]
      if (n.length === 3) t = multiply(multiply([1, 0, 0, 1, n[1], n[2]], t), [1, 0, 0, 1, -n[1], -n[2]])
    } else if (op === "skewX") t = [1, 0, Math.tan(rad(n[0])), 1, 0, 0]
    else if (op === "skewY") t = [1, Math.tan(rad(n[0])), 0, 1, 0, 0]
    else throw new Error("unknown transform " + op)
    m = multiply(m, t)
  }
  return m
}

const num = (v, d = 0) => (v === undefined ? d : parseFloat(v))

function shapePath(tag, p) {
  switch (tag) {
    case "path": return p.d
    case "rect": {
      const x = num(p.x), y = num(p.y), w = num(p.width), h = num(p.height)
      let rx = num(p.rx, num(p.ry)), ry = num(p.ry, rx)
      rx = Math.min(rx, w / 2); ry = Math.min(ry, h / 2)
      if (!rx && !ry) return `M${x},${y}H${x + w}V${y + h}H${x}Z`
      return `M${x + rx},${y}H${x + w - rx}A${rx},${ry} 0 0 1 ${x + w},${y + ry}V${y + h - ry}A${rx},${ry} 0 0 1 ${x + w - rx},${y + h}H${x + rx}A${rx},${ry} 0 0 1 ${x},${y + h - ry}V${y + ry}A${rx},${ry} 0 0 1 ${x + rx},${y}Z`
    }
    case "circle": case "ellipse": {
      const cx = num(p.cx), cy = num(p.cy), rx = num(p.rx, num(p.r)), ry = num(p.ry, num(p.r))
      return `M${cx - rx},${cy}A${rx},${ry} 0 1 0 ${cx + rx},${cy}A${rx},${ry} 0 1 0 ${cx - rx},${cy}Z`
    }
    case "line": return `M${num(p.x1)},${num(p.y1)}L${num(p.x2)},${num(p.y2)}`
    case "polyline": case "polygon": {
      const n = String(p.points).trim().split(/[\s,]+/).map(Number)
      let d = `M${n[0]},${n[1]}`
      for (let i = 2; i + 1 < n.length; i += 2) d += `L${n[i]},${n[i + 1]}`
      return tag === "polygon" ? d + "Z" : d
    }
    default: return null
  }
}

const NAMED = { white: "#FFFFFF", black: "#000000", red: "#FF0000", none: "none" }

function color(v) {
  if (v === undefined) return undefined
  v = String(v).trim().toLowerCase()
  if (NAMED[v]) return NAMED[v]
  if (/^#[0-9a-f]{3}$/.test(v)) return "#" + [...v.slice(1)].map((c) => c + c).join("").toUpperCase()
  if (/^#[0-9a-f]{6}$/.test(v)) return v.toUpperCase()
  if (v === "currentcolor") return "#141210"
  throw new Error("unsupported colour " + v)
}

const FORBIDDEN = new Set(["text", "image", "foreignObject", "script", "style", "linearGradient", "radialGradient", "filter", "pattern", "mask", "clipPath", "use"])

function flatten(svg) {
  const tree = parse(svg)
  const top = tree.children.find((c) => c.tagName === "svg")
  const box = String(top.properties.viewBox ?? "0 0 400 400").split(/[\s,]+/).map(Number)
  const shapes = []
  const problems = []
  const walk = (node, m, style) => {
    if (node.type !== "element") return
    if (FORBIDDEN.has(node.tagName)) { problems.push(node.tagName); return }
    const p = { ...node.properties }
    for (const [, k, v] of String(p.style ?? "").matchAll(/([\w-]+)\s*:\s*([^;]+)/g)) p[k] = v.trim()
    const here = multiply(m, transform(p.transform))
    const next = {
      fill: p.fill ?? style.fill,
      stroke: p.stroke ?? style.stroke,
      width: p["stroke-width"] ?? style.width,
      opacity: num(p.opacity, 1) * style.opacity,
      fillOpacity: p["fill-opacity"] ?? style.fillOpacity,
      strokeOpacity: p["stroke-opacity"] ?? style.strokeOpacity,
      cap: p["stroke-linecap"] ?? style.cap,
      join: p["stroke-linejoin"] ?? style.join,
    }
    const d = shapePath(node.tagName, p)
    if (d) {
      const scale = Math.sqrt(Math.abs(here[0] * here[3] - here[1] * here[2]))
      const fill = color(next.fill ?? "#000000")
      const stroke = color(next.stroke ?? "none")
      shapes.push({
        d: svgpath(d).matrix(here).round(2).toString(),
        fill: fill === "none" ? null : fill,
        fillAlpha: num(next.fillOpacity, 1) * next.opacity,
        stroke: stroke === "none" ? null : stroke,
        strokeAlpha: num(next.strokeOpacity, 1) * next.opacity,
        width: +(num(next.width, 1) * scale).toFixed(2),
        cap: next.cap ?? "butt",
        join: next.join ?? "miter",
      })
    }
    for (const child of node.children ?? []) walk(child, here, next)
  }
  // the root svg often carries the shared stroke and fill, children inherit it like any group
  const rootStyle = {
    fill: top.properties.fill,
    stroke: top.properties.stroke,
    width: top.properties["stroke-width"],
    opacity: num(top.properties.opacity, 1),
    cap: top.properties["stroke-linecap"],
    join: top.properties["stroke-linejoin"],
  }
  for (const child of top.children) walk(child, IDENTITY, rootStyle)
  return { box, shapes, problems }
}

// ---- run ----

mkdirSync(`${root}docs/art`, { recursive: true })
mkdirSync(`${root}tmp/claude/onboarding`, { recursive: true })

const svgFile = (scene) => `${root}docs/art/onboarding-${scene.name}.svg`
// ask for any missing scenes at the same time
await Promise.all(scenes.map(async (scene) => {
  if (existsSync(svgFile(scene)) && process.env.REDRAW !== scene.name && process.env.REDRAW !== "all") return
  console.log("drawing", scene.name)
  writeFileSync(svgFile(scene), (await draw(scene.subject)) + "\n")
}))

const kotlin = []
for (const scene of scenes) {
  const file = svgFile(scene)
  const svg = readFileSync(file, "utf8")
  const { box, shapes, problems } = flatten(svg)
  writeFileSync(`${root}tmp/claude/onboarding/${scene.name}.png`, new Resvg(svg, { fitTo: { mode: "width", value: 600 }, background: "#FFFFFF" }).render().asPng())
  console.log(scene.name, `${shapes.length} shapes`, problems.length ? `skipped ${problems.join(",")}` : "clean")

  const col = (c, a) => (c ? `0x${Math.round(a * 255).toString(16).padStart(2, "0").toUpperCase()}${c.slice(1)}` : "null")
  const name = scene.name[0].toUpperCase() + scene.name.slice(1)
  kotlin.push(`    val ${name} = SvgArt(
        ${box[0]}f, ${box[1]}f, ${box[2]}f, ${box[3]}f,
        listOf(
${shapes.map((s) => `            SvgShape("${s.d}", ${col(s.fill, s.fillAlpha)}, ${col(s.stroke, s.strokeAlpha)}, ${s.width}f, round = ${s.cap === "round" || s.join === "round"}),`).join("\n")}
        ),
    )`)
}

writeFileSync(`${root}android/app/src/main/java/dev/mintu/hotseat/ui/art/OnboardingArt.kt`, `package dev.mintu.hotseat.ui.art

// generated by tools/onboarding-art.mjs from the svgs in docs/art, do not edit by hand

object OnboardingArt {
${kotlin.join("\n\n")}
}
`)
console.log("wrote OnboardingArt.kt")
