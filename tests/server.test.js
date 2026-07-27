const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { createServer } = require('../server');

function makeRequest(port, method, path) {
  return new Promise((resolve, reject) => {
    const req = http.request({ hostname: '127.0.0.1', port, path, method }, (res) => {
      let data = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => {
        data += chunk;
      });
      res.on('end', () => {
        resolve({ statusCode: res.statusCode, body: data, headers: res.headers });
      });
    });
    req.on('error', reject);
    req.end();
  });
}

test('serves the landing page and health endpoint', async () => {
  const server = createServer();
  await new Promise((resolve) => server.listen(0, resolve));
  const { port } = server.address();

  try {
    const indexResponse = await makeRequest(port, 'GET', '/');
    assert.equal(indexResponse.statusCode, 200);
    assert.match(indexResponse.body, /AgriNexus/);

    const healthResponse = await makeRequest(port, 'GET', '/api/health');
    assert.equal(healthResponse.statusCode, 200);
    assert.match(healthResponse.body, /AgriNexus backend/);
  } finally {
    await new Promise((resolve, reject) => server.close((error) => (error ? reject(error) : resolve())));
  }
});

test('returns a weather response for farmer locations', async () => {
  const server = createServer();
  await new Promise((resolve) => server.listen(0, resolve));
  const { port } = server.address();

  try {
    const response = await makeRequest(port, 'GET', '/api/weather?location=Limpopo');
    assert.equal(response.statusCode, 200);
    const payload = JSON.parse(response.body);
    assert.ok(Array.isArray(payload));
    assert.ok(payload[0].temperatureC !== undefined);
    assert.ok(payload[0].region);
  } finally {
    await new Promise((resolve, reject) => server.close((error) => (error ? reject(error) : resolve())));
  }
});
