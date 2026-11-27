import { afterEach, describe, expect, it, vi } from 'vitest';
import { openAttempt, settleAttempt } from './transfer-attempt';

describe('transfer attempt', () => {
  const intent = { owner: 'ana@example.com', phone: '11987654321', amount: 2500 };

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('says whether the key already existed', async () => {
    const first = await openAttempt(intent);
    expect(first.resumed).toBe(false);
    const again = await openAttempt(intent);
    expect(again.resumed).toBe(true);
    expect(again.idempotencyKey).toBe(first.idempotencyKey);
  });

  it('two concurrent opens of the same intent get the same key', async () => {
    // Both digests finish in the same turn, as on a double click: nothing may sit between read and write.
    vi.spyOn(crypto.subtle, 'digest').mockImplementation(() =>
      Promise.resolve(new ArrayBuffer(32)),
    );
    const [a, b] = await Promise.all([openAttempt(intent), openAttempt(intent)]);
    expect(a.idempotencyKey).toBe(b.idempotencyKey);
    expect(a.slot).toBe(b.slot);
    expect([a.resumed, b.resumed].sort()).toEqual([false, true]);
  });

  it('a late settle of an older attempt does not drop the key of a newer one', async () => {
    const older = await openAttempt(intent);
    settleAttempt(older);
    const newer = await openAttempt(intent);
    expect(newer.resumed).toBe(false);
    settleAttempt(older);
    const again = await openAttempt(intent);
    expect(again.idempotencyKey).toBe(newer.idempotencyKey);
    expect(again.resumed).toBe(true);
  });

  it('settle of the current attempt removes the slot', async () => {
    const attempt = await openAttempt(intent);
    settleAttempt(attempt);
    expect(sessionStorage.getItem(attempt.slot)).toBeNull();
  });

  it('uses the transfer-attempt: prefix and a SHA-256 digest of owner, phone and amount', async () => {
    const attempt = await openAttempt(intent);
    const bytes = new TextEncoder().encode(
      JSON.stringify([intent.owner, intent.phone, intent.amount]),
    );
    const digest = await crypto.subtle.digest('SHA-256', bytes);
    const hex = Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, '0')).join('');
    expect(attempt.slot).toBe(`transfer-attempt:${hex}`);
  });

  it('uses a new key for another recipient', async () => {
    const first = await openAttempt(intent);
    const other = await openAttempt({ ...intent, phone: '11912345678' });
    expect(other.idempotencyKey).not.toBe(first.idempotencyKey);
    expect(other.resumed).toBe(false);
  });

  it('uses a new key when the amount changes and finds the first again when it comes back', async () => {
    const first = await openAttempt(intent);
    const other = await openAttempt({ ...intent, amount: 3000 });
    expect(other.idempotencyKey).not.toBe(first.idempotencyKey);
    const back = await openAttempt(intent);
    expect(back.idempotencyKey).toBe(first.idempotencyKey);
    expect(back.resumed).toBe(true);
  });

  it('keeps the key when the same user signs in again', async () => {
    const first = await openAttempt(intent);
    const again = await openAttempt({ ...intent });
    expect(again.idempotencyKey).toBe(first.idempotencyKey);
  });

  it('never gives the key to another user of the tab, and keeps it for its owner', async () => {
    const ana = await openAttempt(intent);
    const bia = await openAttempt({ ...intent, owner: 'bia@example.com' });
    expect(bia.idempotencyKey).not.toBe(ana.idempotencyKey);
    expect(bia.resumed).toBe(false);
    const anaAgain = await openAttempt(intent);
    expect(anaAgain.idempotencyKey).toBe(ana.idempotencyKey);
  });

  it('does not leave the e-mail or the phone readable in the tab storage', async () => {
    await openAttempt(intent);
    const dump = JSON.stringify(
      Object.fromEntries(
        Array.from({ length: sessionStorage.length }, (_, i) => {
          const key = sessionStorage.key(i) as string;
          return [key, sessionStorage.getItem(key)];
        }),
      ),
    );
    expect(dump).not.toContain('ana@example.com');
    expect(dump).not.toContain('11987654321');
  });

  it('storage failure throws on read', async () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked');
    });
    await expect(openAttempt(intent)).rejects.toThrow('blocked');
  });

  it('storage failure throws on write', async () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('full');
    });
    await expect(openAttempt(intent)).rejects.toThrow('full');
  });

  it('storage failure throws when crypto.subtle is missing', async () => {
    vi.stubGlobal('crypto', { randomUUID: () => 'x' });
    await expect(openAttempt(intent)).rejects.toThrow();
    expect(sessionStorage.length).toBe(0);
  });
});
