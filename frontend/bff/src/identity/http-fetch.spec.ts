import { createServer, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { httpFetch } from './http-fetch.js';

describe('httpFetch', () => {
  let server: Server;
  let seen: { host?: string; method?: string; body: string };

  beforeEach(async () => {
    seen = { body: '' };
    server = createServer((req, res) => {
      seen.host = req.headers.host;
      seen.method = req.method;
      req.on('data', (chunk: Buffer) => (seen.body += chunk.toString()));
      req.on('end', () => {
        res.writeHead(200, { 'content-type': 'application/json' });
        res.end('{"ok":true}');
      });
    });
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  });

  afterEach(() => new Promise((resolve) => server.close(resolve)));

  it('honours a custom Host header, which the global fetch drops', async () => {
    const port = (server.address() as AddressInfo).port;
    const response = await httpFetch(`http://127.0.0.1:${port}/token`, {
      method: 'POST',
      headers: { Host: 'localhost:8090', 'Content-Type': 'text/plain' },
      body: 'a=b',
    });
    expect(response.ok).toBe(true);
    expect(await response.json()).toEqual({ ok: true });
    expect(seen).toEqual({ host: 'localhost:8090', method: 'POST', body: 'a=b' });
  });
});
