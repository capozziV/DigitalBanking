import { SeverityNumber, logs, trace } from '@opentelemetry/api';
import { pino, multistream, type DestinationStream, type Logger } from 'pino';
import { currentCorrelationId } from './correlation.js';

export const LOGGER = Symbol('LOGGER');

const SEVERITIES: Record<number, SeverityNumber> = {
  10: SeverityNumber.TRACE,
  20: SeverityNumber.DEBUG,
  30: SeverityNumber.INFO,
  40: SeverityNumber.WARN,
  50: SeverityNumber.ERROR,
  60: SeverityNumber.FATAL,
};
const ENVELOPE = new Set(['level', 'time', 'msg', 'pid', 'hostname']);

/**
 * Sends each log line to OTLP. It reads the line pino already serialized, so the redaction applies to what is
 * exported too. Without a registered SDK the global logger is a no-op.
 */
const otlpStream: DestinationStream = {
  write(line: string): void {
    try {
      const entry = JSON.parse(line) as Record<string, unknown>;
      const attributes = Object.fromEntries(
        Object.entries(entry).filter(([key, value]) => !ENVELOPE.has(key) && value !== undefined),
      ) as Record<string, string>;
      const level = Number(entry['level']);
      logs.getLogger('bff').emit({
        severityNumber: SEVERITIES[level] ?? SeverityNumber.INFO,
        severityText: pino.levels.labels[level]?.toUpperCase(),
        body: typeof entry['msg'] === 'string' ? entry['msg'] : '',
        attributes,
      });
    } catch {
      // Telemetry never breaks logging.
    }
  },
};

export function createLogger(level: string, destination?: DestinationStream): Logger {
  return pino(
    {
      level,
      mixin: () => {
        const span = trace.getActiveSpan()?.spanContext();
        return {
          correlationId: currentCorrelationId(),
          ...(span ? { traceId: span.traceId, spanId: span.spanId } : {}),
        };
      },
      redact: {
        paths: [
          'password',
          'token',
          'accessToken',
          'refreshToken',
          '*.password',
          '*.token',
          '*.accessToken',
          '*.refreshToken',
          'req.headers.authorization',
          'req.headers.cookie',
        ],
        censor: '[redacted]',
      },
    },
    multistream([{ stream: destination ?? process.stdout }, { stream: otlpStream }]),
  );
}
