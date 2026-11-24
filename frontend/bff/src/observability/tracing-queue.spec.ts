import { trace } from '@opentelemetry/api';
import { describe, expect, it, vi } from 'vitest';
import { HangingLogExporter, HangingSpanExporter } from '../testing/hanging-exporters.js';
import { startTracing } from './otel.js';

// Own file: NodeSDK registers global providers, see tracing-unreachable.spec.ts.
describe('tracing queue with the collector down', () => {
  it('a small queue drops the spans that do not fit, without blocking', async () => {
    const spans = new HangingSpanExporter();
    const logs = new HangingLogExporter();
    const tracing = startTracing({
      serviceName: 'bff-test',
      spanExporter: spans,
      logExporter: logs,
      maxQueueSize: 2,
    });

    for (let i = 0; i < 20; i++) trace.getTracer('t').startSpan('request').end();

    void tracing.forceFlush();
    await vi.waitFor(() => expect(spans.exported.length).toBeGreaterThan(0));
    // Every batch fits the queue, and part of the 20 spans never reached the exporter.
    for (const batch of spans.exported) expect(batch.length).toBeLessThanOrEqual(2);
    const exported = spans.exported.flat().filter((span) => span.name === 'request');
    expect(exported.length).toBeLessThan(20);

    spans.release();
    logs.release();
  });
});
