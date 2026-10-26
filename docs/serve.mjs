// Serves the static export from ./out under the configured basePath.
// `next start` does not work with `output: 'export'`.
import http from 'node:http'

import handler from 'serve-handler'

const basePath = '/essencium-backend'
const port = Number(process.env.PORT ?? 3000)

http
  .createServer((req, res) => {
    const [pathname, query] = req.url.split('?')

    if (pathname !== basePath && !pathname.startsWith(`${basePath}/`)) {
      res.writeHead(302, { Location: `${basePath}/` })
      res.end()
      return
    }

    req.url =
      (pathname.slice(basePath.length) || '/') + (query ? `?${query}` : '')
    return handler(req, res, { public: 'out' })
  })
  .listen(port, () => {
    console.log(`Docs available at http://localhost:${port}${basePath}/`)
  })
