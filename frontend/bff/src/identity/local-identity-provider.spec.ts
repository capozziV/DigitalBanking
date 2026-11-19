import { IdentityError } from './identity-provider.js';
import { LocalIdentityProvider } from './local-identity-provider.js';
import { InMemoryKeyValueStore } from '../testing/in-memory-key-value-store.js';

function tokenResponse(accessToken: string): Response {
  return new Response(JSON.stringify({ access_token: accessToken, expires_in: 3600 }), {
    status: 200,
    headers: { 'content-type': 'application/json' },
  });
}

describe('LocalIdentityProvider', () => {
  let store: InMemoryKeyValueStore;
  let fetchMock: ReturnType<typeof vi.fn>;
  let provider: LocalIdentityProvider;

  beforeEach(() => {
    store = new InMemoryKeyValueStore();
    fetchMock = vi.fn().mockImplementation(async () => tokenResponse('jwt'));
    provider = new LocalIdentityProvider(
      store,
      'http://mock.test/default',
      fetchMock as unknown as typeof fetch,
    );
  });

  it('accepts sign-up without a code and stores only a hash of the password', async () => {
    await expect(provider.signUp('Ana@Example.test', '123456')).resolves.toEqual({
      confirmationRequired: false,
    });
    const stored = store.keys().map((key) => key);
    expect(stored).toHaveLength(1);
    expect(JSON.stringify([await store.get(stored[0]!)])).not.toContain('123456');
  });

  it('rejects a duplicated e-mail, ignoring case', async () => {
    await provider.signUp('ana@example.test', '123456');
    await expect(provider.signUp('ANA@example.test', '123456')).rejects.toMatchObject({
      code: 'user_exists',
    });
  });

  it('signs in with a stable sub (UUID) as username in the mock request', async () => {
    await provider.signUp('ana@example.test', '123456');
    const first = await provider.signIn('ana@example.test', '123456');
    const second = await provider.signIn('ANA@example.test', '123456');

    expect(first.accessToken).toBe('jwt');
    expect(first.expiresInSeconds).toBe(3600);
    const usernames = fetchMock.mock.calls.map((call) =>
      new URLSearchParams(call[1].body).get('username'),
    );
    expect(usernames[0]).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/,
    );
    expect(usernames[1]).toBe(usernames[0]);
    expect(fetchMock.mock.calls[0]![0]).toBe('http://mock.test/default/token');
    expect(second.refreshToken).not.toBe(first.refreshToken);
  });

  it('rejects wrong password and unknown user the same way', async () => {
    await provider.signUp('ana@example.test', '123456');
    await expect(provider.signIn('ana@example.test', '000000')).rejects.toMatchObject({
      code: 'invalid_credentials',
    });
    await expect(provider.signIn('nobody@example.test', '123456')).rejects.toBeInstanceOf(
      IdentityError,
    );
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('refreshes with the opaque refresh token and issues a new access token for the same sub', async () => {
    await provider.signUp('ana@example.test', '123456');
    const tokens = await provider.signIn('ana@example.test', '123456');
    fetchMock.mockImplementation(async () => tokenResponse('jwt-2'));

    const refreshed = await provider.refresh(tokens.refreshToken);

    expect(refreshed.accessToken).toBe('jwt-2');
    const usernames = fetchMock.mock.calls.map((call) =>
      new URLSearchParams(call[1].body).get('username'),
    );
    expect(usernames[1]).toBe(usernames[0]);
  });

  it('fails the refresh for an unknown refresh token', async () => {
    await expect(provider.refresh('nope')).rejects.toMatchObject({ code: 'refresh_failed' });
  });
});
