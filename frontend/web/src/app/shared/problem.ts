import { HttpErrorResponse } from '@angular/common/http';

/** Message from a Problem Details body (`detail`), or the fallback when there is none. */
export function problemMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as { detail?: unknown } | null;
    if (body && typeof body === 'object' && typeof body.detail === 'string' && body.detail) {
      return body.detail;
    }
  }
  return fallback;
}
