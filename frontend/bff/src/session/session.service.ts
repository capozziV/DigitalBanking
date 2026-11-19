import { randomBytes } from 'node:crypto';
import { Inject, Injectable, ServiceUnavailableException } from '@nestjs/common';
import { IdentityError, IdentityProvider, type Tokens } from '../identity/identity-provider.js';
import { KEY_VALUE_STORE, type KeyValueStore } from '../redis/key-value-store.js';

export const CLOCK = Symbol('CLOCK');
export const SESSION_PREFIX = 'bff:session:';
export const SESSION_TTL_SECONDS = 7 * 24 * 3600;
export const REFRESH_THRESHOLD_MS = 60_000;

export interface Session {
  id: string;
  email: string;
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
  csrfToken: string;
}

type StoredSession = Omit<Session, 'id'>;

@Injectable()
export class SessionService {
  constructor(
    @Inject(KEY_VALUE_STORE) private readonly store: KeyValueStore,
    private readonly identity: IdentityProvider,
    @Inject(CLOCK) private readonly now: () => number,
  ) {}

  async create(email: string, tokens: Tokens): Promise<Session> {
    const session: Session = {
      id: randomBytes(32).toString('base64url'),
      email,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      expiresAt: this.now() + tokens.expiresInSeconds * 1000,
      csrfToken: randomBytes(32).toString('base64url'),
    };
    await this.save(session);
    return session;
  }

  /** Returns the session (refreshing its token when close to expiry) or null when it does not exist or its refresh token was refused; throws 503 when the identity provider is unreachable (session kept). */
  async resolve(id: string | undefined): Promise<Session | null> {
    if (!id) return null;
    const raw = await this.store.get(SESSION_PREFIX + id);
    if (!raw) return null;
    const session: Session = { id, ...(JSON.parse(raw) as StoredSession) };
    if (session.expiresAt - this.now() >= REFRESH_THRESHOLD_MS) return session;
    try {
      const tokens = await this.identity.refresh(session.refreshToken);
      session.accessToken = tokens.accessToken;
      session.refreshToken = tokens.refreshToken;
      session.expiresAt = this.now() + tokens.expiresInSeconds * 1000;
      await this.save(session);
      return session;
    } catch (error) {
      if (error instanceof IdentityError && error.code === 'refresh_failed') {
        await this.destroy(id);
        return null;
      }
      throw new ServiceUnavailableException({
        code: 'unavailable',
        message: 'Serviço indisponível. Tente novamente.',
      });
    }
  }

  async destroy(id: string): Promise<void> {
    await this.store.delete(SESSION_PREFIX + id);
  }

  private async save(session: Session): Promise<void> {
    const { id, ...stored } = session;
    await this.store.set(SESSION_PREFIX + id, JSON.stringify(stored), SESSION_TTL_SECONDS);
  }
}
