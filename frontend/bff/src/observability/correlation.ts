import { AsyncLocalStorage } from 'node:async_hooks';
import { randomUUID } from 'node:crypto';
import type { NextFunction, Request, Response } from 'express';
import type { Logger } from 'pino';

export const CORRELATION_HEADER = 'X-Correlation-ID';

const storage = new AsyncLocalStorage<{ correlationId: string }>();

export function currentCorrelationId(): string | undefined {
  return storage.getStore()?.correlationId;
}

export function runWithCorrelationId<T>(correlationId: string, work: () => T): T {
  return storage.run({ correlationId }, work);
}

const VALID_ID = /^[A-Za-z0-9._-]{1,128}$/;

export function correlationMiddleware(logger: Logger) {
  return (request: Request, response: Response, next: NextFunction): void => {
    const incoming = request.header(CORRELATION_HEADER);
    const correlationId = incoming && VALID_ID.test(incoming) ? incoming : randomUUID();
    response.setHeader(CORRELATION_HEADER, correlationId);
    const startedAt = Date.now();
    runWithCorrelationId(correlationId, () => {
      response.on('finish', () => {
        // Path only: query strings may carry personal data.
        runWithCorrelationId(correlationId, () =>
          logger.info(
            {
              method: request.method,
              path: request.path,
              status: response.statusCode,
              durationMs: Date.now() - startedAt,
            },
            'request completed',
          ),
        );
      });
      next();
    });
  };
}
