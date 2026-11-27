import { BffError } from './bff-client';

export type ProblemDetails = { type?: string; title?: string; detail?: string; status?: number };

/**
 * Message of an error body: Problem Details `detail`, or the BFF's own `message`
 * (`{code, message}`); the fallback when there is none.
 */
export function problemMessage(error: unknown, fallback: string): string {
  if (error instanceof BffError) {
    const body = error.problem as { detail?: unknown; message?: unknown } | null;
    if (body && typeof body === 'object') {
      if (typeof body.detail === 'string' && body.detail) return body.detail;
      if (typeof body.message === 'string' && body.message) return body.message;
    }
  }
  return fallback;
}
