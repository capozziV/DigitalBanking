import { pino, type DestinationStream, type Logger } from 'pino';
import { currentCorrelationId } from './correlation.js';

export const LOGGER = Symbol('LOGGER');

export function createLogger(level: string, destination?: DestinationStream): Logger {
  return pino(
    {
      level,
      mixin: () => ({ correlationId: currentCorrelationId() }),
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
    destination,
  );
}
