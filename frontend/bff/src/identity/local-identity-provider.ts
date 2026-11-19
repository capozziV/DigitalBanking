import { createHash, randomBytes, scryptSync, timingSafeEqual } from 'node:crypto';
import type { KeyValueStore } from '../redis/key-value-store.js';
import { httpFetch } from './http-fetch.js';
import { IdentityError, IdentityProvider, type Tokens } from './identity-provider.js';

const USER_PREFIX = 'bff:local-user:';
const REFRESH_PREFIX = 'bff:local-refresh:';
const REFRESH_TTL_SECONDS = 7 * 24 * 3600;

interface StoredUser {
  sub: string;
  salt: string;
  hash: string;
}

/** The customer id is the token `sub`, so it must be stable per e-mail: a UUID (version 7 layout) from a hash. */
function subFor(email: string): string {
  const bytes = createHash('sha256').update(`local-user:${email}`).digest().subarray(0, 16);
  bytes[6] = (bytes[6]! & 0x0f) | 0x70;
  bytes[8] = (bytes[8]! & 0x3f) | 0x80;
  const hex = bytes.toString('hex');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function hashPassword(password: string, salt: string): string {
  return scryptSync(password, salt, 32).toString('hex');
}

/**
 * Local adapter: the mock-oauth2-server has no sign-up, so users live in Redis (e-mail and password hash) and the
 * token is requested from the mock with the user's stable `sub` as username (see infra/local/auth/token.sh).
 */
export class LocalIdentityProvider extends IdentityProvider {
  constructor(
    private readonly store: KeyValueStore,
    private readonly mockUrl: string,
    /** Host sent to the mock: it builds the token `iss` from it (see MOCK_OAUTH2_HOST in infra/local/auth/token.sh). */
    private readonly mockHost?: string,
    private readonly fetchFn: typeof fetch = httpFetch as typeof fetch,
  ) {
    super();
  }

  async signUp(email: string, password: string): Promise<{ confirmationRequired: boolean }> {
    const normalized = email.toLowerCase();
    const salt = randomBytes(16).toString('hex');
    const user: StoredUser = { sub: subFor(normalized), salt, hash: hashPassword(password, salt) };
    const created = await this.store.setIfAbsent(USER_PREFIX + normalized, JSON.stringify(user));
    if (!created) throw new IdentityError('user_exists');
    return { confirmationRequired: false };
  }

  async confirmSignUp(): Promise<void> {
    // No confirmation code locally: sign-up already activates the user.
  }

  async signIn(email: string, password: string): Promise<Tokens> {
    const raw = await this.store.get(USER_PREFIX + email.toLowerCase());
    if (!raw) throw new IdentityError('invalid_credentials');
    const user = JSON.parse(raw) as StoredUser;
    const expected = Buffer.from(user.hash, 'hex');
    const actual = Buffer.from(hashPassword(password, user.salt), 'hex');
    if (!timingSafeEqual(expected, actual)) throw new IdentityError('invalid_credentials');
    const refreshToken = randomBytes(32).toString('base64url');
    await this.store.set(REFRESH_PREFIX + refreshToken, user.sub, REFRESH_TTL_SECONDS);
    return this.issue(user.sub, refreshToken);
  }

  async refresh(refreshToken: string): Promise<Tokens> {
    const sub = await this.store.get(REFRESH_PREFIX + refreshToken);
    if (!sub) throw new IdentityError('refresh_failed');
    return this.issue(sub, refreshToken);
  }

  private async issue(sub: string, refreshToken: string): Promise<Tokens> {
    let response: Response;
    try {
      response = await this.fetchFn(`${this.mockUrl}/token`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          Authorization: `Basic ${Buffer.from('web:local-secret').toString('base64')}`,
          ...(this.mockHost ? { Host: this.mockHost } : {}),
        },
        body: new URLSearchParams({
          grant_type: 'password',
          username: sub,
          password: 'unused',
        }).toString(),
      });
    } catch {
      throw new IdentityError('unavailable');
    }
    if (!response.ok) throw new IdentityError('unavailable');
    const body = (await response.json()) as { access_token: string; expires_in?: number };
    return {
      accessToken: body.access_token,
      refreshToken,
      expiresInSeconds: body.expires_in ?? 3600,
    };
  }
}
