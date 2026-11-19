import {
  ForbiddenException,
  Inject,
  Injectable,
  UnauthorizedException,
  createParamDecorator,
  type CanActivate,
  type ExecutionContext,
} from '@nestjs/common';
import type { Request, Response } from 'express';
import { APP_CONFIG, type AppConfig } from '../config/config.js';
import {
  CSRF_COOKIE,
  CSRF_HEADER,
  SESSION_COOKIE,
  clearSessionCookies,
  readCookie,
} from './cookies.js';
import { SessionService, type Session } from './session.service.js';

export type SessionRequest = Request & { bankSession?: Session };

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

/** Requires a live session; every method that is not GET also needs the double-submit CSRF header. */
@Injectable()
export class SessionGuard implements CanActivate {
  constructor(
    private readonly sessions: SessionService,
    @Inject(APP_CONFIG) private readonly config: AppConfig,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const http = context.switchToHttp();
    const request = http.getRequest<SessionRequest>();
    const response = http.getResponse<Response>();

    const session = await this.sessions.resolve(readCookie(request, SESSION_COOKIE));
    if (!session) {
      clearSessionCookies(response, this.config);
      throw new UnauthorizedException({ code: 'session_expired', message: 'Sessão expirada.' });
    }
    if (!SAFE_METHODS.has(request.method)) {
      const header = request.header(CSRF_HEADER);
      if (!header || header !== session.csrfToken || header !== readCookie(request, CSRF_COOKIE)) {
        throw new ForbiddenException({ code: 'csrf', message: 'Token CSRF inválido.' });
      }
    }
    request.bankSession = session;
    return true;
  }
}

export const CurrentSession = createParamDecorator((_data: unknown, context: ExecutionContext) => {
  return context.switchToHttp().getRequest<SessionRequest>().bankSession as Session;
});
