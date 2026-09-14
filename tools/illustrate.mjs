// generates a transparent glass style illustration with gpt-image and saves it as a webp drawable
// usage: node tools/illustrate.mjs <name> "<subject>"
import { readFileSync, writeFileSync, mkdirSync } from "node:fs"
import { execFileSync } from "node:child_process"

const [name, subject] = process.argv.slice(2)
if (!name || !subject) {
  console.error('usage: node tools/illustrate.mjs <name> "<subject>"')
  process.exit(1)
}

const key = process.env.OPENAI_API_KEY ?? readFileSync("worker/.dev.vars", "utf8").match(/^OPENAI_API_KEY=(.*)$/m)?.[1]
if (!key) throw new Error("no OPENAI_API_KEY in env or worker/.dev.vars")

// keep every illustration in the same family as the nucleo glass icons
const style =
  "Style: premium 3D glass icon illustration, like Apple liquid glass. Frosted milky translucent glass with a soft blue gradient core glowing through (from #4989E9 to #0A64E4), crisp thin white rim highlights on the edges, subtle inner refraction, very soft blurred shadow below. Minimal, clean, centered, lots of empty space around it. No text, no background scenery, isolated object on a fully transparent background."

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

const tmp = "tmp/claude/illustrations"
mkdirSync(tmp, { recursive: true })
const png = `${tmp}/${name}.png`
writeFileSync(png, Buffer.from(body.data[0].b64_json, "base64"))

const out = `android/app/src/main/res/drawable-nodpi/illo_${name}.webp`
execFileSync("sips", ["-Z", "768", png, "--out", `${tmp}/${name}-768.png`], { stdio: "ignore" })
execFileSync("cwebp", ["-quiet", "-q", "90", "-alpha_q", "100", `${tmp}/${name}-768.png`, "-o", out])
console.log("wrote", out, "raw", png)
