import {
  BadRequestException,
  Body,
  ConflictException,
  Controller,
  ForbiddenException,
  Get,
  HttpCode,
  Inject,
  Post,
  Req,
  Res,
  ServiceUnavailableException,
  UnauthorizedException,
  UseGuards,
} from '@nestjs/common';
import type { Request, Response } from 'express';
import type { Logger } from 'pino';
import { APP_CONFIG, type AppConfig } from '../config/config.js';
import { IdentityError, IdentityProvider } from '../identity/identity-provider.js';
import { LOGGER } from '../observability/logger.js';
import {
  SESSION_COOKIE,
  readCookie,
  setSessionCookies,
  clearSessionCookies,
} from '../session/cookies.js';
import { CurrentSession, SessionGuard } from '../session/session.guard.js';
import { SessionService, type Session } from '../session/session.service.js';
import { isValidPassword } from './password.js';

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const CODE = /^\d{6}$/;

interface Credentials {
  email: string;
  password: string;
}

function readEmail(body: Record<string, unknown> | undefined): string {
  const email = body?.['email'];
  if (typeof email !== 'string' || email.length > 254 || !EMAIL.test(email)) {
    throw new BadRequestException({ code: 'invalid_email', message: 'E-mail inválido.' });
  }
  return email.trim().toLowerCase();
}

function readCredentials(body: Record<string, unknown> | undefined): Credentials {
  const email = readEmail(body);
  const password = body?.['password'];
  // Validated here, before the identity provider is called (ADR 0024).
  if (!isValidPassword(password)) {
    throw new BadRequestException({
      code: 'invalid_password',
      message: 'A senha deve ter exatamente 6 dígitos.',
    });
  }
  return { email, password };
}

@Controller('auth')
export class AuthController {
  constructor(
    private readonly identity: IdentityProvider,
    private readonly sessions: SessionService,
    @Inject(APP_CONFIG) private readonly config: AppConfig,
    @Inject(LOGGER) private readonly logger: Logger,
  ) {}

  @Post('sign-up')
  async signUp(@Body() body: Record<string, unknown>): Promise<{ confirmationRequired: boolean }> {
    const { email, password } = readCredentials(body);
    return this.call(() => this.identity.signUp(email, password), 'sign-up');
  }

  @Post('confirm')
  @HttpCode(204)
  async confirm(@Body() body: Record<string, unknown>): Promise<void> {
    const email = readEmail(body);
    const code = body?.['code'];
    if (typeof code !== 'string' || !CODE.test(code)) {
      throw new BadRequestException({ code: 'invalid_code', message: 'Código inválido.' });
    }
    await this.call(() => this.identity.confirmSignUp(email, code), 'confirm');
  }

  @Post('sign-in')
  @HttpCode(200)
  async signIn(
    @Body() body: Record<string, unknown>,
    @Res({ passthrough: true }) response: Response,
  ): Promise<{ authenticated: true; email: string }> {
    const { email, password } = readCredentials(body);
    const tokens = await this.call(() => this.identity.signIn(email, password), 'sign-in');
    const session = await this.sessions.create(email, tokens);
    setSessionCookies(response, this.config, session.id, session.csrfToken);
    this.logger.info('session created');
    return { authenticated: true, email };
  }

  @Post('sign-out')
  @HttpCode(204)
  @UseGuards(SessionGuard)
  async signOut(
    @CurrentSession() session: Session,
    @Res({ passthrough: true }) response: Response,
  ): Promise<void> {
    await this.sessions.destroy(session.id);
    clearSessionCookies(response, this.config);
  }

  @Get('session')
  async session(
    @Req() request: Request,
    @Res({ passthrough: true }) response: Response,
  ): Promise<{ authenticated: boolean; email: string | null }> {
    const session = await this.sessions.resolve(readCookie(request, SESSION_COOKIE));
    if (!session) {
      if (readCookie(request, SESSION_COOKIE)) clearSessionCookies(response, this.config);
      return { authenticated: false, email: null };
    }
    return { authenticated: true, email: session.email };
  }

  private async call<T>(work: () => Promise<T>, operation: string): Promise<T> {
    try {
      return await work();
    } catch (error) {
      if (!(error instanceof IdentityError)) throw error;
      this.logger.warn({ operation, reason: error.code }, 'identity request failed');
      switch (error.code) {
        case 'user_exists':
          throw new ConflictException({
            code: error.code,
            message: 'Já existe uma conta com este e-mail.',
          });
        case 'invalid_code':
          throw new BadRequestException({
            code: error.code,
            message: 'Código inválido ou expirado.',
          });
        case 'invalid_credentials':
        case 'refresh_failed':
          throw new UnauthorizedException({
            code: 'invalid_credentials',
            message: 'E-mail ou senha incorretos.',
          });
        case 'not_confirmed':
          throw new ForbiddenException({
            code: error.code,
            message: 'Confirme seu e-mail para entrar.',
          });
        default:
          throw new ServiceUnavailableException({
            code: 'unavailable',
            message: 'Serviço de identidade indisponível.',
          });
      }
    }
  }
}
