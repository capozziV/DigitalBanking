import { RecipientTokenService } from './recipient-token.js';

const ACCOUNT = '0b6f3f0e-1c53-4f6d-9a77-2f0a9a3f1a10';

describe('RecipientTokenService', () => {
  let now: number;
  const service = (secret = 'a-secret-with-enough-length') =>
    new RecipientTokenService(
      { recipientTokenSecret: secret } as ConstructorParameters<typeof RecipientTokenService>[0],
      () => now,
    );

  beforeEach(() => {
    now = 1_000_000;
  });

  it('round-trips the account for the same session', () => {
    const token = service().issue(ACCOUNT, 'session-1');
    expect(service().verify(token, 'session-1')).toBe(ACCOUNT);
  });

  it('does not show the account id in clear text', () => {
    expect(service().issue(ACCOUNT, 'session-1')).not.toContain(ACCOUNT);
  });

  it('refuses a token whose payload was changed', () => {
    const [payload, signature] = service().issue(ACCOUNT, 'session-1').split('.');
    const forged = Buffer.from(
      JSON.stringify({ ...JSON.parse(Buffer.from(payload!, 'base64url').toString()), a: 'other' }),
    ).toString('base64url');
    expect(service().verify(`${forged}.${signature}`, 'session-1')).toBeNull();
  });

  it('refuses a token whose signature was changed', () => {
    const [payload, signature] = service().issue(ACCOUNT, 'session-1').split('.');
    const flipped = (signature![0] === 'A' ? 'B' : 'A') + signature!.slice(1);
    expect(service().verify(`${payload}.${flipped}`, 'session-1')).toBeNull();
  });

  it('refuses a token signed with another key', () => {
    const token = service('another-secret-with-enough-length').issue(ACCOUNT, 'session-1');
    expect(service().verify(token, 'session-1')).toBeNull();
  });

  it('refuses a token issued to another session', () => {
    const token = service().issue(ACCOUNT, 'session-1');
    expect(service().verify(token, 'session-2')).toBeNull();
  });

  it('refuses an expired token', () => {
    const token = service().issue(ACCOUNT, 'session-1');
    now += 10 * 60_000 + 1;
    expect(service().verify(token, 'session-1')).toBeNull();
  });

  it.each(['', 'abc', 'a.b.c', '.', 'x.y'])('refuses the garbage %j', (token) => {
    expect(service().verify(token, 'session-1')).toBeNull();
  });
});
