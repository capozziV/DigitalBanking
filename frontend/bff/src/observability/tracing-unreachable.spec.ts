import { createServer } from 'node:http';
import type { AddressInfo } from 'node:net';
import { trace } from '@opentelemetry/api';
import { describe, expect, it, vi } from 'vitest';
import { BackendClient } from '../backend/backend-client.js';
import { loadConfig } from '../config/config.js';
import { HangingLogExporter, HangingSpanExporter } from '../testing/hanging-exporters.js';
import { createLogger } from './logger.js';
import { startTracing } from './otel.js';

// NodeSDK registers global trace/logs providers: each scenario of the collector-down case has its own file
// (vitest isolates files), see tracing-queue.spec.ts.
describe('tracing with the collector down', () => {
  it('exporter unreachable does not fail the request', async () => {
    const spans = new HangingSpanExporter();
    const logs = new HangingLogExporter();
    const tracing = startTracing({
      serviceName: 'bff-test',
      spanExporter: spans,
      logExporter: logs,
    });
    const server = createServer((_request, response) => response.end('{}'));
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
    const client = new BackendClient(
      loadConfig({
        IDENTITY_PROVIDER: 'local',
        REDIS_URL: 'redis://127.0.0.1:1',
        ACCOUNTS_URL: `http://127.0.0.1:${(server.address() as AddressInfo).port}`,
      }),
      fetch,
    );
    const logger = createLogger('info', { write() {} });

    // The requests resolving while every export is still pending proves the export is off the request path.
    const answers = await Promise.all(
      Array.from({ length: 20 }, () =>
        trace.getTracer('t').startActiveSpan('request', async (span) => {
          logger.info('request completed');
          const answer = await client.get('accounts', '/accounts/me', { token: 't' });
          span.end();
          return answer;
        }),
      ),
    );
    expect(answers).toEqual(Array.from({ length: 20 }, () => ({})));

    // ...and that the exporter was in fact driven: the exports are pending, not skipped.
    void tracing.forceFlush();
    await vi.waitFor(() => {
      expect(spans.exported.flat().filter((span) => span.name === 'request')).toHaveLength(20);
      expect(logs.exported.flat().length).toBeGreaterThan(0);
    });

    spans.release();
    logs.release();
    server.close();
  });
});
