import { trace } from '@opentelemetry/api';
import { InMemorySpanExporter } from '@opentelemetry/sdk-trace';
import { InMemoryLogRecordExporter } from '@opentelemetry/sdk-logs';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { startTracing, type Tracing } from './otel.js';
import { createLogger } from './logger.js';

describe('logger', () => {
  const logs = new InMemoryLogRecordExporter();
  let tracing: Tracing;

  beforeAll(() => {
    tracing = startTracing({
      serviceName: 'bff-test',
      spanExporter: new InMemorySpanExporter(),
      logExporter: logs,
    });
  });
  afterAll(() => tracing.shutdown());

  function capture() {
    const lines: string[] = [];
    return {
      lines,
      logger: createLogger('info', { write: (line: string) => void lines.push(line) }),
    };
  }

  it('log line has traceId and spanId', async () => {
    const { lines, logger } = capture();
    await trace.getTracer('t').startActiveSpan('s', async (span) => {
      logger.info('inside');
      const line = JSON.parse(lines[0]!);
      expect(line.traceId).toBe(span.spanContext().traceId);
      expect(line.spanId).toBe(span.spanContext().spanId);
      span.end();
    });
    logger.info('outside');
    const outside = JSON.parse(lines[1]!);
    expect(outside.traceId).toBeUndefined();
  });

  it('exported log keeps redaction of password and token', async () => {
    const { logger } = capture();
    logger.info({ password: 'hunter2', user: { token: 'abc.def' }, ok: 'visible' }, 'sign in');
    await tracing.forceFlush();
    const record = logs.getFinishedLogRecords().find((r) => r.body === 'sign in')!;
    const dump = JSON.stringify(record.attributes);
    expect(dump).not.toContain('hunter2');
    expect(dump).not.toContain('abc.def');
    expect(dump).toContain('visible');
  });

  it('LOG_LEVEL=debug emits debug lines to the destination and to OTLP', async () => {
    const lines: string[] = [];
    const logger = createLogger('debug', { write: (line: string) => void lines.push(line) });
    logger.debug('verbose detail');
    await tracing.forceFlush();
    expect(lines.map((line) => JSON.parse(line).msg)).toEqual(['verbose detail']);
    expect(logs.getFinishedLogRecords().some((r) => r.body === 'verbose detail')).toBe(true);
  });

  it('LOG_LEVEL=info drops debug lines', () => {
    const { lines, logger } = capture();
    logger.debug('verbose detail');
    expect(lines).toEqual([]);
  });
});
