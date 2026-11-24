import { createServer } from 'node:http';
import type { AddressInfo } from 'node:net';
import { trace } from '@opentelemetry/api';
import { describe, expect, it } from 'vitest';
import { BackendClient } from '../backend/backend-client.js';
import { loadConfig } from '../config/config.js';
import { createLogger } from './logger.js';
import { startTracing } from './otel.js';

describe('tracing with the collector down', () => {
  it('exporter unreachable does not fail the request', async () => {
    const tracing = startTracing({ serviceName: 'bff-test', endpoint: 'http://127.0.0.1:1' });
    const server = createServer((_request, response) => response.end('{}'));
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
    const url = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
    const client = new BackendClient(
      loadConfig({
        IDENTITY_PROVIDER: 'local',
        REDIS_URL: 'redis://127.0.0.1:1',
        ACCOUNTS_URL: url,
      }),
      fetch,
    );
    const logger = createLogger('info', { write() {} });

    const started = Date.now();
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
    expect(Date.now() - started).toBeLessThan(2000);
    await tracing.shutdown().catch(() => undefined);
    server.close();
  });
});
