import { InMemoryKeyValueStore } from '../testing/in-memory-key-value-store.js';
import { RECIPIENT_TOKEN_TTL_SECONDS, RecipientTokenService } from './recipient-token.js';

const ACCOUNT = '0b6f3f0e-1c53-4f6d-9a77-2f0a9a3f1a10';

describe('RecipientTokenService', () => {
  let now: number;
  let store: InMemoryKeyValueStore;
  let service: RecipientTokenService;

  beforeEach(() => {
    now = 1_000_000;
    store = new InMemoryKeyValueStore(() => now);
    service = new RecipientTokenService(store);
  });

  it('round-trips the account for the same session', async () => {
    const token = await service.issue(ACCOUNT, 'Bruno Lima', 'session-1');
    expect(await service.resolve(token, 'session-1')).toBe(ACCOUNT);
  });

  it('is opaque: random, and does not carry the account id', async () => {
    const first = await service.issue(ACCOUNT, 'Bruno Lima', 'session-1');
    const second = await service.issue(ACCOUNT, 'Bruno Lima', 'session-1');
    expect(first).not.toBe(second);
    expect(first).not.toContain(ACCOUNT);
    expect(Buffer.from(first, 'base64url').toString()).not.toContain(ACCOUNT);
  });

  it('keeps the entry under bff:recipient:{token}', async () => {
    const token = await service.issue(ACCOUNT, 'Bruno Lima', 'session-1');
    expect(store.keys()).toEqual([`bff:recipient:${token}`]);
  });

  it('refuses an unknown token', async () => {
    expect(await service.resolve('does-not-exist', 'session-1')).toBeNull();
  });

  it('refuses a token issued to another session', async () => {
    const token = await service.issue(ACCOUNT, 'Bruno Lima', 'session-1');
    expect(await service.resolve(token, 'session-2')).toBeNull();
  });

  it('refuses an expired token', async () => {
    const token = await service.issue(ACCOUNT, 'Bruno Lima', 'session-1');
    now += RECIPIENT_TOKEN_TTL_SECONDS * 1000 + 1;
    expect(await service.resolve(token, 'session-1')).toBeNull();
  });

  it('does not consume the token: resolves again', async () => {
    const token = await service.issue(ACCOUNT, 'Bruno Lima', 'session-1');
    await service.resolve(token, 'session-1');
    expect(await service.resolve(token, 'session-1')).toBe(ACCOUNT);
  });

  it.each([undefined, null, 42, '', {}])('refuses the garbage %j', async (token) => {
    expect(await service.resolve(token, 'session-1')).toBeNull();
  });
});
