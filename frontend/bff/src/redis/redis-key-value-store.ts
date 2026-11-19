import { Redis } from 'ioredis';
import type { OnModuleDestroy } from '@nestjs/common';
import type { KeyValueStore } from './key-value-store.js';

export class RedisKeyValueStore implements KeyValueStore, OnModuleDestroy {
  private readonly redis: Redis;

  constructor(url: string) {
    this.redis = new Redis(url, { lazyConnect: true, maxRetriesPerRequest: 2 });
    // Without a listener, a Redis outage would crash the process with an unhandled 'error' event.
    this.redis.on('error', () => undefined);
  }

  get(key: string): Promise<string | null> {
    return this.redis.get(key);
  }

  async set(key: string, value: string, ttlSeconds?: number): Promise<void> {
    if (ttlSeconds === undefined) {
      await this.redis.set(key, value);
    } else {
      await this.redis.set(key, value, 'EX', ttlSeconds);
    }
  }

  async setIfAbsent(key: string, value: string): Promise<boolean> {
    return (await this.redis.set(key, value, 'NX')) === 'OK';
  }

  async delete(key: string): Promise<void> {
    await this.redis.del(key);
  }

  async onModuleDestroy(): Promise<void> {
    this.redis.disconnect();
  }
}
