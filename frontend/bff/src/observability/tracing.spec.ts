import { createServer, type IncomingHttpHeaders, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { context, trace } from '@opentelemetry/api';
import { InMemorySpanExporter } from '@opentelemetry/sdk-trace';
import { InMemoryLogRecordExporter } from '@opentelemetry/sdk-logs';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { BackendClient } from '../backend/backend-client.js';
import { loadConfig } from '../config/config.js';
import { startTracing, type Tracing } from './otel.js';

describe('tracing', () => {
  const spans = new InMemorySpanExporter();
  const logs = new InMemoryLogRecordExporter();
  let tracing: Tracing;
  let server: Server;
  let baseUrl: string;
  const received: { path: string; headers: IncomingHttpHeaders }[] = [];

  beforeAll(async () => {
    tracing = startTracing({ serviceName: 'bff-test', spanExporter: spans, logExporter: logs });
    server = createServer((request, response) => {
      received.push({ path: request.url ?? '', headers: request.headers });
      response.setHeader('content-type', 'application/json');
      response.end('{}');
    });
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
    baseUrl = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
  });

  afterAll(async () => {
    await tracing.shutdown();
    server.close();
  });

  function clientFor(url: string): BackendClient {
    const config = loadConfig({
      IDENTITY_PROVIDER: 'local',
      REDIS_URL: 'redis://127.0.0.1:1',
      ACCOUNTS_URL: url,
    });
    return new BackendClient(config, fetch);
  }

  it('outgoing backend call carries traceparent of the inbound span', async () => {
    const tracer = trace.getTracer('test');
    let traceId = '';
    await tracer.startActiveSpan('POST /bff/transfers', async (span) => {
      traceId = span.spanContext().traceId;
      await clientFor(baseUrl).get('accounts', '/accounts/me', { token: 't' });
      span.end();
    });
    const header = received.at(-1)!.headers['traceparent'] as string;
    expect(header).toMatch(new RegExp(`^00-${traceId}-[0-9a-f]{16}-01$`));
  });

  it('exports spans without query string or sensitive headers', async () => {
    await trace.getTracer('test').startActiveSpan('root', async (span) => {
      await clientFor(baseUrl).get('accounts', '/customers/lookup?phone=%2B5521912345678', {
        token: 'secret-token',
      });
      span.end();
    });
    await tracing.forceFlush();
    const attributes = spans.getFinishedSpans().flatMap((span) => Object.entries(span.attributes));
    const dump = JSON.stringify(attributes);
    expect(dump).not.toContain('5521912345678');
    expect(dump).not.toContain('secret-token');
    expect(attributes.some(([key]) => key.startsWith('http.request.header.'))).toBe(false);
    expect(attributes.some(([key]) => key === 'url.query')).toBe(false);
  });

  it('exports the active trace with the log record', async () => {
    await trace.getTracer('test').startActiveSpan('logged', async (span) => {
      const { createLogger } = await import('./logger.js');
      createLogger('info', { write() {} }).info('hello');
      expect(context.active()).toBeDefined();
      span.end();
    });
    await tracing.forceFlush();
    expect(logs.getFinishedLogRecords().some((record) => record.body === 'hello')).toBe(true);
  });
});
