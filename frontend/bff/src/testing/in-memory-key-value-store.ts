import type { KeyValueStore } from '../redis/key-value-store.js';

export class InMemoryKeyValueStore implements KeyValueStore {
  private readonly entries = new Map<string, string>();

  async get(key: string): Promise<string | null> {
    return this.entries.get(key) ?? null;
  }

  async set(key: string, value: string): Promise<void> {
    this.entries.set(key, value);
  }

  async setIfAbsent(key: string, value: string): Promise<boolean> {
    if (this.entries.has(key)) return false;
    this.entries.set(key, value);
    return true;
  }

  async delete(key: string): Promise<void> {
    this.entries.delete(key);
  }

  keys(): string[] {
    return [...this.entries.keys()];
  }
}
