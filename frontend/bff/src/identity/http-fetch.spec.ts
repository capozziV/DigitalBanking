import { createServer, type Server } from 'node:http';
import { createServer as createNetServer } from 'node:net';
import type { AddressInfo, Socket } from 'node:net';
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

  it('rejects when the server accepts and never answers (timeout)', async () => {
    const sockets: Socket[] = [];
    const hanging = createNetServer((socket) => {
      sockets.push(socket);
    });
    await new Promise<void>((resolve) => hanging.listen(0, '127.0.0.1', resolve));
    const port = (hanging.address() as AddressInfo).port;
    try {
      await expect(httpFetch(`http://127.0.0.1:${port}/`, { timeoutMs: 50 })).rejects.toThrow(
        'timeout',
      );
    } finally {
      sockets.forEach((socket) => socket.destroy());
      await new Promise((resolve) => hanging.close(resolve));
    }
  });

  it('rejects on a network error (closed port)', async () => {
    const probe = createNetServer();
    await new Promise<void>((resolve) => probe.listen(0, '127.0.0.1', resolve));
    const port = (probe.address() as AddressInfo).port;
    await new Promise((resolve) => probe.close(resolve));
    await expect(httpFetch(`http://127.0.0.1:${port}/`)).rejects.toThrow();
  });

  it('returns a null body for 204', async () => {
    server.removeAllListeners('request');
    server.on('request', (_req, res) => {
      res.writeHead(204);
      res.end();
    });
    const port = (server.address() as AddressInfo).port;
    const response = await httpFetch(`http://127.0.0.1:${port}/`);
    expect(response.status).toBe(204);
    expect(response.body).toBeNull();
  });
});
