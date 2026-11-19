import type { CookieOptions, Request, Response } from 'express';
import type { AppConfig } from '../config/config.js';
import { SESSION_TTL_SECONDS } from './session.service.js';

export const SESSION_COOKIE = 'bank_session';
export const CSRF_COOKIE = 'bank_csrf';
export const CSRF_HEADER = 'X-CSRF-Token';

export function readCookie(request: Request, name: string): string | undefined {
  const header = request.headers.cookie;
  if (!header) return undefined;
  for (const part of header.split(';')) {
    const separator = part.indexOf('=');
    if (separator > 0 && part.slice(0, separator).trim() === name) {
      try {
        return decodeURIComponent(part.slice(separator + 1).trim());
      } catch {
        return undefined; // malformed value counts as an absent cookie
      }
    }
  }
  return undefined;
}

function options(config: AppConfig, httpOnly: boolean): CookieOptions {
  return {
    httpOnly,
    sameSite: 'strict',
    secure: config.cookieSecure,
    path: '/',
    maxAge: SESSION_TTL_SECONDS * 1000,
  };
}

export function setSessionCookies(
  response: Response,
  config: AppConfig,
  sessionId: string,
  csrfToken: string,
): void {
  response.cookie(SESSION_COOKIE, sessionId, options(config, true));
  response.cookie(CSRF_COOKIE, csrfToken, options(config, false));
}

export function clearSessionCookies(response: Response, config: AppConfig): void {
  const base = { ...options(config, true), maxAge: undefined };
  response.clearCookie(SESSION_COOKIE, base);
  response.clearCookie(CSRF_COOKIE, { ...base, httpOnly: false });
}
