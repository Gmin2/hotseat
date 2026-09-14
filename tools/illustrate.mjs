// generates a transparent glass style illustration with gpt-image and saves it as a webp drawable
// usage: node tools/illustrate.mjs <name> "<subject>"
import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs"
import { execFileSync } from "node:child_process"

const [name, subject] = process.argv.slice(2)
if (!name || !subject) {
  console.error('usage: node tools/illustrate.mjs <name> "<subject>"')
  process.exit(1)
}

const key = process.env.OPENAI_API_KEY ?? readFileSync("worker/.dev.vars", "utf8").match(/^OPENAI_API_KEY=(.*)$/m)?.[1]
if (!key) throw new Error("no OPENAI_API_KEY in env or worker/.dev.vars")

// keep every illustration in the same flat family as the app: sky blues, white, ink black, one red accent
const style =
  "Style: flat minimal vector illustration for a clean iOS style app, simple rounded geometric shapes, solid flat fills only, palette limited to sky blue #4989E9, pale blue #A5CAFA, white #FFFFFF, near black #1C1C1C for small details and one tiny red #E03143 accent. No gradients, no 3D, no glass, no shadows, no outlines except thin near black details. Friendly and calm, centered, generous empty space. No text or letters anywhere, isolated on a fully transparent background."

const tmp = "tmp/claude/illustrations"
const png = `${tmp}/${name}.png`
mkdirSync(tmp, { recursive: true })

// REUSE=1 skips the api and only redoes the trim and export from the last raw png
if (!(process.env.REUSE && existsSync(png))) {
const res = await fetch("https://api.openai.com/v1/images/generations", {
  method: "POST",
  headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
  body: JSON.stringify({
    model: "gpt-image-2.5-sunburst",
    prompt: `${subject}. ${style}`,
    size: "1024x1024",
    quality: "high",
    background: "transparent",
    output_format: "png",
  }),
})
const body = await res.json()
if (!res.ok) throw new Error(JSON.stringify(body.error ?? body))

writeFileSync(png, Buffer.from(body.data[0].b64_json, "base64"))
}

// trim the transparent margin so the art fills its slot, keep a little air and a square canvas
const size = 1024
const alpha = execFileSync("ffmpeg", ["-v", "error", "-i", png, "-vf", "alphaextract", "-f", "rawvideo", "-pix_fmt", "gray", "-"], { maxBuffer: size * size + 16 })
let x0 = size, y0 = size, x1 = 0, y1 = 0
for (let y = 0; y < size; y++) {
  for (let x = 0; x < size; x++) {
    if (alpha[y * size + x] > 16) {
      if (x < x0) x0 = x
      if (x > x1) x1 = x
      if (y < y0) y0 = y
      if (y > y1) y1 = y
    }
  }
}
const side = Math.min(size, Math.round(Math.max(x1 - x0, y1 - y0) * 1.08))
const cx = Math.round((x0 + x1) / 2), cy = Math.round((y0 + y1) / 2)
const left = Math.max(0, Math.min(size - side, cx - side / 2)), top = Math.max(0, Math.min(size - side, cy - side / 2))
execFileSync("ffmpeg", ["-v", "error", "-y", "-i", png, "-vf", `crop=${side}:${side}:${left}:${top},scale=768:768`, `${tmp}/${name}-768.png`])

const out = `android/app/src/main/res/drawable-nodpi/illo_${name}.webp`
mkdirSync("android/app/src/main/res/drawable-nodpi", { recursive: true })
execFileSync("cwebp", ["-quiet", "-q", "90", "-alpha_q", "100", `${tmp}/${name}-768.png`, "-o", out])
console.log("wrote", out, "raw", png)
