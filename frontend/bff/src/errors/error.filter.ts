import {
  Catch,
  HttpException,
  Inject,
  type ArgumentsHost,
  type ExceptionFilter,
} from '@nestjs/common';
import type { Response } from 'express';
import type { Logger } from 'pino';
import { BackendProblemError, BackendUnavailableError } from '../backend/backend-client.js';
import { LOGGER } from '../observability/logger.js';
import { GENERIC_ERROR, PROBLEM_MESSAGES, UNAVAILABLE } from './error-messages.js';

/**
 * Everything that fails ends here, in Portuguese: a refusal of a service as `{type, status, detail}` with the text
 * of [PROBLEM_MESSAGES], an error of the BFF as `{code, message}`.
 */
@Catch()
export class ErrorFilter implements ExceptionFilter {
  constructor(@Inject(LOGGER) private readonly logger: Logger) {}

  catch(error: unknown, host: ArgumentsHost): void {
    const response = host.switchToHttp().getResponse<Response>();
    if (error instanceof BackendProblemError) {
      response.status(error.status).json(problemBody(error));
      return;
    }
    if (error instanceof BackendUnavailableError) {
      response.status(503).json({ code: 'unavailable', message: UNAVAILABLE });
      return;
    }
    if (error instanceof HttpException) {
      const body = error.getResponse();
      // Thrown by the BFF, which always gives a code and a text in Portuguese.
      if (typeof body === 'object' && 'code' in body) {
        response.status(error.getStatus()).json(body);
        return;
      }
    }
    const status = statusOf(error);
    if (status >= 500) {
      // Only the stack: the properties of an error may carry what it was handling (Redis puts the command there).
      this.logger.error(
        { error: error instanceof Error ? error.stack : typeof error },
        'unexpected error',
      );
      response.status(status).json({ code: 'internal_error', message: GENERIC_ERROR });
    } else if (status === 404) {
      response.status(status).json({ code: 'not_found', message: 'Página não encontrada.' });
    } else {
      response.status(status).json({ code: 'invalid_request', message: 'Requisição inválida.' });
    }
  }
}

function problemBody({ status, problem }: BackendProblemError) {
  const message = PROBLEM_MESSAGES.get(problem.type ?? '');
  if (message === undefined)
    return { status, detail: status === 503 ? UNAVAILABLE : GENERIC_ERROR };
  return { type: problem.type, status, detail: message };
}

/** The status of an error of Nest or of the body parser (`status` 4xx); anything else is a 500. */
function statusOf(error: unknown): number {
  if (error instanceof HttpException) return error.getStatus();
  const status = (error as { status?: unknown } | null)?.status;
  return typeof status === 'number' && status >= 400 && status < 500 ? status : 500;
}
