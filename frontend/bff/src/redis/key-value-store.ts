export const KEY_VALUE_STORE = Symbol('KEY_VALUE_STORE');

export interface KeyValueStore {
  get(key: string): Promise<string | null>;
  set(key: string, value: string, ttlSeconds?: number): Promise<void>;
  /** Stores only when the key does not exist yet; returns whether it stored. */
  setIfAbsent(key: string, value: string): Promise<boolean>;
  delete(key: string): Promise<void>;
}
