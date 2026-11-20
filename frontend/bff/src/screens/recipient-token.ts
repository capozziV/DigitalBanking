import { createHmac, timingSafeEqual } from 'node:crypto';
import { Inject, Injectable } from '@nestjs/common';
import { APP_CONFIG, type AppConfig } from '../config/config.js';
import { CLOCK } from '../session/session.service.js';

export const RECIPIENT_TOKEN_TTL_MS = 10 * 60_000;

interface Payload {
  /** account that receives the transfer */
  a: string;
  /** session the token was issued to */
  s: string;
  /** expiry, epoch millis */
  e: number;
}

/**
 * The browser never sees the destination account id: it gets a token that carries it, signed with HMAC-SHA256,
 * bound to the session and valid for ten minutes. A token the BFF did not sign is refused.
 */
@Injectable()
export class RecipientTokenService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: Pick<AppConfig, 'recipientTokenSecret'>,
    @Inject(CLOCK) private readonly now: () => number,
  ) {}

  issue(accountId: string, sessionId: string): string {
    const payload: Payload = { a: accountId, s: sessionId, e: this.now() + RECIPIENT_TOKEN_TTL_MS };
    const body = Buffer.from(JSON.stringify(payload)).toString('base64url');
    return `${body}.${this.sign(body)}`;
  }

  /** The account id, or null when the token is malformed, forged, expired or of another session. */
  verify(token: unknown, sessionId: string): string | null {
    if (typeof token !== 'string') return null;
    const parts = token.split('.');
    if (parts.length !== 2) return null;
    const [body, signature] = parts as [string, string];
    const expected = Buffer.from(this.sign(body));
    const given = Buffer.from(signature);
    if (expected.length !== given.length || !timingSafeEqual(expected, given)) return null;
    try {
      const payload = JSON.parse(Buffer.from(body, 'base64url').toString()) as Partial<Payload>;
      if (
        typeof payload.a !== 'string' ||
        payload.s !== sessionId ||
        typeof payload.e !== 'number' ||
        payload.e < this.now()
      ) {
        return null;
      }
      return payload.a;
    } catch {
      return null;
    }
  }

  private sign(body: string): string {
    return createHmac('sha256', this.config.recipientTokenSecret).update(body).digest('base64url');
  }
}
