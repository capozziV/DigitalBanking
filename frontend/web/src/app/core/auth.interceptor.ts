import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { Injector, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessionService } from './session.service';

const CSRF_COOKIE = 'bank_csrf';

function readCookie(document: Document, name: string): string | null {
  for (const part of document.cookie.split(';')) {
    const [key, ...value] = part.trim().split('=');
    if (key === name) return decodeURIComponent(value.join('='));
  }
  return null;
}

/**
 * Adds the CSRF header (double submit with the `bank_csrf` cookie) to non-GET calls to the BFF and
 * sends the user back to Entrar when the BFF says the session is gone (401).
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const document = inject(DOCUMENT);
  const injector = inject(Injector);
  const router = inject(Router);

  const toBff = request.url.startsWith('/bff/');
  let outgoing = request;
  if (toBff && request.method !== 'GET') {
    const token = readCookie(document, CSRF_COOKIE);
    if (token) outgoing = request.clone({ setHeaders: { 'X-CSRF-Token': token } });
  }

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      // Auth calls report their own 401 (wrong password, no session yet) on the form itself.
      const authCall = request.url.startsWith('/bff/auth/');
      if (toBff && !authCall && error instanceof HttpErrorResponse && error.status === 401) {
        injector.get(SessionService).clear();
        void router.navigateByUrl('/entrar');
      }
      return throwError(() => error);
    }),
  );
};
