import { Injectable } from '@angular/core';

/** One intention to transfer: the recipient is told by the phone, as the token changes on every lookup. */
interface TransferIntent {
  owner: string | null;
  phone: string;
  amount: number;
}

export interface TransferAttempt {
  readonly slot: string;
  readonly idempotencyKey: string;
  /** The key already existed: the user is back on a transfer whose result was never seen. */
  readonly resumed: boolean;
}

const SLOT_PREFIX = 'transfer-attempt:';

/**
 * Keeps the Idempotency-Key of each transfer whose result is not known yet. It outlives the page,
 * a reload and a new sign-in, so a retry never becomes a second transfer. The slot is a digest of
 * the owner, phone and amount: another user of the tab gets other keys and reads no personal data.
 */
@Injectable({ providedIn: 'root' })
export class TransferAttemptStore {
  /** The same intent gets the same key until it is settled. */
  async open({ owner, phone, amount }: TransferIntent): Promise<TransferAttempt> {
    const slot = SLOT_PREFIX + (await sha256(JSON.stringify([owner, phone, amount])));
    // No await from here on: two clicks must not create two keys.
    // If the storage or the digest fails, this throws: no key, no POST (closed on failure).
    const existing = sessionStorage.getItem(slot);
    const idempotencyKey = existing ?? crypto.randomUUID();
    sessionStorage.setItem(slot, idempotencyKey);
    return { slot, idempotencyKey, resumed: existing !== null };
  }

  /** Call only when the user was shown a final status: the next equal transfer is a new one. */
  settle({ slot, idempotencyKey }: TransferAttempt): void {
    // A late answer of an older attempt must not drop the key of a newer one.
    if (sessionStorage.getItem(slot) === idempotencyKey) sessionStorage.removeItem(slot);
  }
}

async function sha256(text: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('');
}
