import { request as httpRequest } from 'node:http';
import { request as httpsRequest } from 'node:https';

/**
 * Minimal fetch over node:http(s). The global fetch (undici) drops a custom `Host` header, and the local adapter
 * needs it: the mock-oauth2-server builds the token `iss` from the Host it receives.
 */
export function httpFetch(
  url: string,
  init: { method?: string; headers?: Record<string, string>; body?: string } = {},
): Promise<Response> {
  const target = new URL(url);
  const request = target.protocol === 'https:' ? httpsRequest : httpRequest;
  return new Promise((resolve, reject) => {
    const req = request(target, { method: init.method ?? 'GET', headers: init.headers }, (res) => {
      const chunks: Buffer[] = [];
      res.on('data', (chunk: Buffer) => chunks.push(chunk));
      res.on('end', () => {
        const status = res.statusCode ?? 502;
        const body = status === 204 || status === 304 ? null : Buffer.concat(chunks);
        resolve(new Response(body, { status, headers: res.headers as Record<string, string> }));
      });
      res.on('error', reject);
    });
    req.on('error', reject);
    req.end(init.body);
  });
}
