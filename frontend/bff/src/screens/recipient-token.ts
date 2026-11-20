import { randomBytes } from 'node:crypto';
import { Inject, Injectable } from '@nestjs/common';
import { KEY_VALUE_STORE, type KeyValueStore } from '../redis/key-value-store.js';

export const RECIPIENT_TOKEN_TTL_SECONDS = 600;
const PREFIX = 'bff:recipient:';

interface Stored {
  accountId: string;
  recipientName: string;
  sessionId: string;
}

/**
 * The browser never sees the destination account id: it gets an opaque random token, and the BFF keeps
 * the account behind it in Redis for ten minutes, bound to the session. The token is not consumed on use,
 * so a repeated request with the same Idempotency-Key still works.
 */
@Injectable()
export class RecipientTokenService {
  constructor(@Inject(KEY_VALUE_STORE) private readonly store: KeyValueStore) {}

  async issue(accountId: string, recipientName: string, sessionId: string): Promise<string> {
    const token = randomBytes(32).toString('base64url');
    const stored: Stored = { accountId, recipientName, sessionId };
    await this.store.set(PREFIX + token, JSON.stringify(stored), RECIPIENT_TOKEN_TTL_SECONDS);
    return token;
  }

  /** The account id, or null when the token is unknown, expired or of another session. */
  async resolve(token: unknown, sessionId: string): Promise<string | null> {
    if (typeof token !== 'string' || token === '') return null;
    const raw = await this.store.get(PREFIX + token);
    if (raw === null) return null;
    try {
      const stored = JSON.parse(raw) as Partial<Stored>;
      if (typeof stored.accountId !== 'string' || stored.sessionId !== sessionId) return null;
      return stored.accountId;
    } catch {
      return null;
    }
  }
}
