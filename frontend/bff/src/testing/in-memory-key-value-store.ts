import type { KeyValueStore } from '../redis/key-value-store.js';

interface Entry {
  value: string;
  /** epoch millis, or null when the entry never expires */
  expiresAt: number | null;
}

export class InMemoryKeyValueStore implements KeyValueStore {
  private readonly entries = new Map<string, Entry>();

  constructor(private readonly now: () => number = () => Date.now()) {}

  async get(key: string): Promise<string | null> {
    return this.live(key)?.value ?? null;
  }

  async set(key: string, value: string, ttlSeconds?: number): Promise<void> {
    this.entries.set(key, {
      value,
      expiresAt: ttlSeconds === undefined ? null : this.now() + ttlSeconds * 1000,
    });
  }

  async setIfAbsent(key: string, value: string): Promise<boolean> {
    if (this.live(key)) return false;
    this.entries.set(key, { value, expiresAt: null });
    return true;
  }

  async delete(key: string): Promise<void> {
    this.entries.delete(key);
  }

  keys(): string[] {
    return [...this.entries.keys()].filter((key) => this.live(key));
  }

  private live(key: string): Entry | undefined {
    const entry = this.entries.get(key);
    if (entry?.expiresAt != null && entry.expiresAt <= this.now()) {
      this.entries.delete(key);
      return undefined;
    }
    return entry;
  }
}
