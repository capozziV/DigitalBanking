import { trace } from '@opentelemetry/api';
import { ExpressInstrumentation } from '@opentelemetry/instrumentation-express';
import { HttpInstrumentation } from '@opentelemetry/instrumentation-http';
import { IORedisInstrumentation } from '@opentelemetry/instrumentation-ioredis';
import { UndiciInstrumentation } from '@opentelemetry/instrumentation-undici';
import { OTLPLogExporter } from '@opentelemetry/exporter-logs-otlp-http';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { NodeSDK } from '@opentelemetry/sdk-node';
import { BatchLogRecordProcessor, type LogRecordExporter } from '@opentelemetry/sdk-logs';
import { BatchSpanProcessor, type ReadableSpan, type SpanExporter } from '@opentelemetry/sdk-trace';

export const DEFAULT_OTLP_ENDPOINT = 'http://127.0.0.1:4318';

export interface TracingOptions {
  serviceName?: string;
  /** OTLP/HTTP base URL; defaults to `OTEL_EXPORTER_OTLP_ENDPOINT`, then to the local collector. */
  endpoint?: string;
  spanExporter?: SpanExporter;
  logExporter?: LogRecordExporter;
}

export interface Tracing {
  forceFlush(): Promise<void>;
  shutdown(): Promise<void>;
}

/** Attribute keys that can carry a query string, a header, or a command argument: personal data or secrets. */
const URL_KEYS = new Set(['url.full', 'http.url', 'http.target']);
const DROPPED_KEYS = new Set(['url.query', 'db.statement', 'db.query.text']);
const DROPPED_PREFIXES = ['http.request.header.', 'http.response.header.'];

export function sanitizeAttributes(
  attributes: ReadableSpan['attributes'],
): ReadableSpan['attributes'] {
  const clean: ReadableSpan['attributes'] = {};
  for (const [key, value] of Object.entries(attributes)) {
    if (DROPPED_KEYS.has(key) || DROPPED_PREFIXES.some((prefix) => key.startsWith(prefix)))
      continue;
    clean[key] = URL_KEYS.has(key) && typeof value === 'string' ? value.split('?')[0]! : value;
  }
  return clean;
}

/** Strips query strings, headers and command arguments from every span before it leaves the process. */
class SanitizingSpanExporter implements SpanExporter {
  constructor(private readonly delegate: SpanExporter) {}

  export(spans: ReadableSpan[], resultCallback: Parameters<SpanExporter['export']>[1]): void {
    this.delegate.export(
      spans.map((span) =>
        Object.create(span, {
          attributes: { value: sanitizeAttributes(span.attributes), enumerable: true },
        }),
      ),
      resultCallback,
    );
  }

  shutdown(): Promise<void> {
    return this.delegate.shutdown();
  }

  forceFlush(): Promise<void> {
    return this.delegate.forceFlush?.() ?? Promise.resolve();
  }
}

/**
 * Starts OpenTelemetry: 100% sampling (the SDK default), W3C Trace Context, spans and logs exported in batches over
 * OTLP/HTTP through bounded queues, so a collector that is down drops telemetry instead of failing or delaying requests.
 */
export function startTracing(options: TracingOptions = {}): Tracing {
  const endpoint = (
    options.endpoint ??
    process.env['OTEL_EXPORTER_OTLP_ENDPOINT'] ??
    DEFAULT_OTLP_ENDPOINT
  ).replace(/\/$/, '');
  const batch = { scheduledDelayMillis: 1000, exportTimeoutMillis: 5000, maxQueueSize: 2048 };
  const spanProcessor = new BatchSpanProcessor({
    exporter: new SanitizingSpanExporter(
      options.spanExporter ??
        new OTLPTraceExporter({ url: `${endpoint}/v1/traces`, timeoutMillis: 5000 }),
    ),
    ...batch,
  });
  const logProcessor = new BatchLogRecordProcessor({
    exporter:
      options.logExporter ??
      new OTLPLogExporter({ url: `${endpoint}/v1/logs`, timeoutMillis: 5000 }),
    ...batch,
  });
  const sdk = new NodeSDK({
    serviceName: options.serviceName ?? process.env['OTEL_SERVICE_NAME'] ?? 'bff',
    spanProcessors: [spanProcessor],
    logRecordProcessors: [logProcessor],
    instrumentations: [
      new HttpInstrumentation(),
      new ExpressInstrumentation(),
      new UndiciInstrumentation(),
      // Command name only: the arguments are session ids and cached values.
      new IORedisInstrumentation({ dbStatementSerializer: (command) => command }),
    ],
  });
  sdk.start();
  return {
    forceFlush: async () => {
      await spanProcessor.forceFlush();
      await logProcessor.forceFlush();
    },
    shutdown: () => sdk.shutdown(),
  };
}

/** Sets the `transfer.id` attribute on the active span (the request span), when there is one. */
export function annotateTransfer(transferId: string): void {
  trace.getActiveSpan()?.setAttribute('transfer.id', transferId);
}
