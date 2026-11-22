import { randomUUID } from 'node:crypto';
import { RedisKeyValueStore } from './redis-key-value-store.js';

// Needs a Redis: REDIS_URL, or the one of the compose.
const REDIS_URL = process.env['REDIS_URL'] ?? 'redis://localhost:6379';

describe('RedisKeyValueStore against Redis', () => {
  const store = new RedisKeyValueStore(REDIS_URL);
  const key = `bff:it:${randomUUID()}`;

  afterAll(async () => {
    await store.delete(key);
    await store.onModuleDestroy();
  });

  it('setIfExists renews a live key and never recreates a deleted one', async () => {
    await store.set(key, 'old', 60);
    expect(await store.setIfExists(key, 'new', 60)).toBe(true);
    expect(await store.get(key)).toBe('new');
    await store.delete(key);
    expect(await store.setIfExists(key, 'again', 60)).toBe(false);
    expect(await store.get(key)).toBeNull();
  });
});
