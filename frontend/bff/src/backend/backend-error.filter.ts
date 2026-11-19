import { Catch, type ArgumentsHost, type ExceptionFilter } from '@nestjs/common';
import type { Response } from 'express';
import { BackendProblemError, BackendUnavailableError } from './backend-client.js';

@Catch(BackendProblemError, BackendUnavailableError)
export class BackendErrorFilter implements ExceptionFilter {
  catch(error: BackendProblemError | BackendUnavailableError, host: ArgumentsHost): void {
    const response = host.switchToHttp().getResponse<Response>();
    if (error instanceof BackendProblemError) {
      response.status(error.status).json(error.problem);
      return;
    }
    response
      .status(503)
      .json({ code: 'unavailable', message: 'Serviço indisponível. Tente novamente.' });
  }
}
