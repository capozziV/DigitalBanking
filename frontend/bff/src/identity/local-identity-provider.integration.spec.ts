import { randomUUID } from 'node:crypto';
import { RedisKeyValueStore } from '../redis/redis-key-value-store.js';
import { LocalIdentityProvider } from './local-identity-provider.js';

// Needs the compose services: `docker compose -f infra/local/docker-compose.yml up -d --wait redis mock-oauth2-server`.
const REDIS_URL = process.env['REDIS_URL'] ?? 'redis://localhost:6379';
const MOCK_URL = process.env['MOCK_OAUTH2_URL'] ?? 'http://localhost:8090/default';

function claims(jwt: string): Record<string, unknown> {
  return JSON.parse(Buffer.from(jwt.split('.')[1]!, 'base64url').toString());
}

describe('LocalIdentityProvider against mock-oauth2-server and Redis', () => {
  const store = new RedisKeyValueStore(REDIS_URL);
  const provider = new LocalIdentityProvider(store, MOCK_URL);
  const email = `it-${randomUUID()}@example.test`;

  afterAll(async () => {
    await store.onModuleDestroy();
  });

  it('signs up, signs in and refreshes with a stable sub', async () => {
    await expect(provider.signUp(email, '123456')).resolves.toEqual({
      confirmationRequired: false,
    });
    await expect(provider.signUp(email, '123456')).rejects.toMatchObject({ code: 'user_exists' });

    const first = await provider.signIn(email, '123456');
    const second = await provider.signIn(email.toUpperCase(), '123456');
    const refreshed = await provider.refresh(first.refreshToken);

    const sub = claims(first.accessToken)['sub'];
    expect(sub).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    expect(claims(second.accessToken)['sub']).toBe(sub);
    expect(claims(refreshed.accessToken)['sub']).toBe(sub);
    expect(first.expiresInSeconds).toBeGreaterThan(60);
    await expect(provider.signIn(email, '000000')).rejects.toMatchObject({
      code: 'invalid_credentials',
    });
  });
});
