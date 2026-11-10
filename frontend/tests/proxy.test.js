import assert from 'node:assert/strict'
import { createServer, request } from 'node:http'
import { once } from 'node:events'
import test from 'node:test'
import { preview } from 'vite'
import config from '../vite.config.js'

function post(url, headers, body) {
  return new Promise((resolve, reject) => {
    const req = request(url, { method: 'POST', headers }, async res => {
      let text = ''
      for await (const chunk of res) text += chunk
      resolve({ status: res.statusCode, text })
    })
    req.on('error', reject)
    req.end(JSON.stringify(body))
  })
}

test('login proxy preserves same-origin requests on fallback and LAN hosts', async (t) => {
  const upstream = createServer(async (req, res) => {
    // Model Spring's same-origin check and explicit localhost:5173 allowlist.
    const origin = req.headers.origin
    if (origin && origin !== `http://${req.headers.host}` && origin !== 'http://localhost:5173') {
      res.writeHead(403).end('Invalid CORS request')
      return
    }
    let body = ''
    for await (const chunk of req) body += chunk
    // Reaching authentication should produce 401 for invalid credentials, not 403.
    res.writeHead(401, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ path: req.url, body: JSON.parse(body), message: 'Email or password is incorrect.' }))
  })
  upstream.listen(0, '127.0.0.1')
  await once(upstream, 'listening')
  t.after(() => new Promise(resolve => upstream.close(resolve)))

  const target = `http://127.0.0.1:${upstream.address().port}`
  const proxy = Object.fromEntries(Object.entries(config.preview.proxy).map(([path, options]) => [
    path, typeof options === 'string' ? target : { ...options, target },
  ]))
  const app = await preview({
    configFile: false,
    preview: { ...config.preview, host: '127.0.0.1', port: 0, proxy },
  })
  t.after(() => new Promise(resolve => app.httpServer.close(resolve)))
  const url = `http://127.0.0.1:${app.httpServer.address().port}/api/v1/auth/login`
  const credentials = { email: 'test@example.invalid', password: 'invalid-password' }

  for (const host of ['localhost:5173', 'localhost:5174', '192.168.0.234:5174']) {
    const response = await post(url,
      { Host: host, Origin: `http://${host}`, 'Content-Type': 'application/json' }, credentials)
    assert.equal(response.status, 401, host)
    const result = JSON.parse(response.text)
    assert.equal(result.path, '/api/v1/auth/login')
    assert.deepEqual(result.body, credentials)
  }

  const rejected = await post(url,
    { Origin: 'http://untrusted.example', 'Content-Type': 'application/json' }, credentials)
  assert.equal(rejected.status, 403, 'foreign origins must still be rejected')
})
