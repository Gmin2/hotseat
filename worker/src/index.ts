export default {
  async fetch(req: Request): Promise<Response> {
    const url = new URL(req.url)

    if (url.pathname === "/health") {
      return new Response("ok")
    }

    return new Response("not found", { status: 404 })
  },
}
