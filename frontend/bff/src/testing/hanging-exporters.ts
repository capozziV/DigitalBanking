import type { ExportResult } from '@opentelemetry/core';
import type { LogRecordExporter, ReadableLogRecord } from '@opentelemetry/sdk-logs';
import type { ReadableSpan, SpanExporter } from '@opentelemetry/sdk-trace';

/** A collector that accepted the connection and never answers: `export` keeps the callback and does not call it. */
export class HangingExporter<T> {
  exported: T[][] = [];
  private released = false;
  private callbacks: ((result: ExportResult) => void)[] = [];

  export(items: T[], callback: (result: ExportResult) => void): void {
    this.exported.push(items);
    if (this.released) callback({ code: 0 });
    else this.callbacks.push(callback);
  }

  /** Lets the pending (and later) exports finish, so the SDK can shut down at the end of a test. */
  release(): void {
    this.released = true;
    for (const callback of this.callbacks.splice(0)) callback({ code: 0 });
  }

  shutdown = async () => {};
  forceFlush = async () => {};
}

export class HangingSpanExporter extends HangingExporter<ReadableSpan> implements SpanExporter {}
export class HangingLogExporter
  extends HangingExporter<ReadableLogRecord>
  implements LogRecordExporter {}
