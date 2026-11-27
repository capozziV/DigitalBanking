import { useSyncExternalStore } from 'react';
import { vi } from 'vitest';
import type { SessionState } from '@/lib/session';

type Listener = () => void;

const visitor: SessionState = {
  loaded: true,
  authenticated: false,
  email: null,
  customer: null,
  account: null,
  unreadCount: 0,
  loadFailed: false,
  signOutFailed: false,
};

let state: SessionState = visitor;
const subscribers = new Set<Listener>();

const set = (partial: Partial<SessionState>): void => {
  state = { ...state, ...partial };
  for (const subscriber of [...subscribers]) subscriber();
};

const actions = {
  load: vi.fn(async () => undefined),
  reload: vi.fn(async () => undefined),
  loadMe: vi.fn(async () => undefined),
  signIn: vi.fn(async () => undefined),
  signUp: vi.fn(async () => ({ confirmationRequired: false })),
  confirm: vi.fn(async () => undefined),
  signOut: vi.fn(async () => undefined),
  clear: vi.fn(() => set({ ...visitor })),
  refreshUnread: vi.fn(async () => undefined),
  startUnreadPolling: vi.fn(),
  stopUnreadPolling: vi.fn(),
};

/** Drives the session store from a test: state and actions (spies). */
export const fakeSession = {
  actions,
  set,
  visitor: (): void => set({ ...visitor }),
  signedInWithoutCustomer: (): void => set({ ...visitor, authenticated: true, email: 'a@b.test' }),
  customer: (partial: Partial<SessionState> = {}): void =>
    set({
      ...visitor,
      authenticated: true,
      email: 'ana@example.test',
      customer: { name: 'Ana Souza', email: 'ana@example.test', phone: '11999990001' },
      account: {
        type: 'BLACK',
        status: 'ACTIVE',
        balance: 1_048_090,
        overdraftLimit: 200_000,
        availableBalance: 1_248_090,
      },
      ...partial,
    }),
  reset: (): void => {
    state = visitor;
    Object.values(actions).forEach((action) => action.mockClear());
  },
};

/** Module replacement for `vi.mock('@/lib/session', ...)`, with the real module's exports. */
export function fakeSessionModule() {
  return {
    ...actions,
    getSnapshot: (): SessionState => state,
    subscribe: (subscriber: Listener): (() => void) => {
      subscribers.add(subscriber);
      return () => subscribers.delete(subscriber);
    },
    useSession: (): SessionState =>
      useSyncExternalStore(
        (subscriber) => {
          subscribers.add(subscriber);
          return () => subscribers.delete(subscriber);
        },
        () => state,
      ),
  };
}
