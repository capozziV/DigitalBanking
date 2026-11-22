import type { INestApplication } from '@nestjs/common';
import { jsonOnlyMiddleware } from './http/json-only.js';
import { correlationMiddleware } from './observability/correlation.js';
import { LOGGER } from './observability/logger.js';
import type { Logger } from 'pino';

export const GLOBAL_PREFIX = 'bff';

export function configureApp(app: INestApplication): void {
  app.setGlobalPrefix(GLOBAL_PREFIX);
  app.use(correlationMiddleware(app.get<Logger>(LOGGER)));
  // Before the body parsers, which Nest registers on init.
  app.use(jsonOnlyMiddleware);
}
