import type { NextFunction, Request, Response } from 'express';
import { SAFE_METHODS } from './safe-methods.js';

/**
 * Login CSRF: a form of another site can post urlencoded, multipart or text/plain, never JSON (that needs a CORS
 * preflight, which the BFF does not answer). So a write takes JSON only, on the public routes too. A write with
 * neither body nor type still passes: the header is read here because `request.is` ignores it without a body.
 */
export function jsonOnlyMiddleware(request: Request, response: Response, next: NextFunction): void {
  if (SAFE_METHODS.has(request.method) || isJsonOrEmpty(request)) {
    next();
    return;
  }
  response.status(415).json({ code: 'unsupported_media_type', message: 'Envie os dados em JSON.' });
}

function isJsonOrEmpty({ headers }: Request): boolean {
  const type = headers['content-type'];
  if (type !== undefined) return type.split(';')[0]?.trim().toLowerCase() === 'application/json';
  return headers['transfer-encoding'] === undefined && Number(headers['content-length'] ?? 0) === 0;
}
