import { Redis } from 'ioredis';
import { RedisKeyValueStore } from './redis-key-value-store.js';

describe('RedisKeyValueStore.setIfExists', () => {
  afterEach(() => vi.restoreAllMocks());

  it.each([
    ['OK', true],
    [null, false],
  ])('is a single SET XX with the TTL: Redis answers %s, stored %s', async (answer, stored) => {
    const set = vi.spyOn(Redis.prototype, 'set').mockResolvedValue(answer as never);
    const store = new RedisKeyValueStore('redis://127.0.0.1:1');
    expect(await store.setIfExists('key', 'value', 60)).toBe(stored);
    expect(set.mock.calls).toEqual([['key', 'value', 'EX', 60, 'XX']]);
    await store.onModuleDestroy();
  });
});
