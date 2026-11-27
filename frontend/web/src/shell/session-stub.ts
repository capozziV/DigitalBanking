/**
 * Temporary stand-in for the session contract that Task 2 delivers in `src/lib/session.ts`,
 * `src/lib/bff-client.ts` and `src/lib/api-types.ts`. Same names and shapes; deleted at merge,
 * when the imports move to `@/lib/...`.
 */
import { useSyncExternalStore } from 'react';

export type AccountType = 'FREE' | 'STANDARD' | 'GOLD' | 'BLACK' | 'DIAMOND';

export interface Customer {
  name: string;
  email: string;
  phone: string;
}

export interface AccountSummary {
  type: string;
  status: string;
  balance: number;
  overdraftLimit: number;
  availableBalance: number;
}

export interface SessionState {
  loaded: boolean;
  authenticated: boolean;
  email: string | null;
  customer: Customer | null;
  account: AccountSummary | null;
  unreadCount: number;
  loadFailed: boolean;
  signOutFailed: boolean;
}

const TYPE_LABELS: Record<string, string> = {
  FREE: 'Free',
  STANDARD: 'Standard',
  GOLD: 'Gold',
  BLACK: 'Black',
  DIAMOND: 'Diamond',
};

export const accountTypeLabel = (type: string): string => TYPE_LABELS[type] ?? type;

type Listener = () => void;

const signedOut: SessionState = {
  loaded: true,
  authenticated: false,
  email: null,
  customer: null,
  account: null,
  unreadCount: 0,
  loadFailed: false,
  signOutFailed: false,
};

let state: SessionState = signedOut;
const subscribers = new Set<Listener>();
const sessionLostListeners = new Set<Listener>();
const unavailableListeners = new Set<Listener>();

const setState = (next: SessionState): void => {
  state = next;
  for (const subscriber of [...subscribers]) subscriber();
};

const subscribe = (subscriber: Listener): (() => void) => {
  subscribers.add(subscriber);
  return () => subscribers.delete(subscriber);
};

export function useSession(): SessionState {
  return useSyncExternalStore(subscribe, () => state);
}

export const session = {
  load: async (): Promise<void> => undefined,
  reload: async (): Promise<void> => undefined,
  signOut: async (): Promise<void> => setState(signedOut),
  clear: (): void => setState(signedOut),
  startUnreadPolling: (): void => undefined,
  stopUnreadPolling: (): void => undefined,
};

export function onSessionLost(listener: Listener): () => void {
  sessionLostListeners.add(listener);
  return () => sessionLostListeners.delete(listener);
}

export function onUnavailable(listener: Listener): () => void {
  unavailableListeners.add(listener);
  return () => unavailableListeners.delete(listener);
}
