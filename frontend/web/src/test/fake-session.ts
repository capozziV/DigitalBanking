import { useSyncExternalStore } from 'react';
import { vi } from 'vitest';
import type { SessionState } from '@/shell/session-stub';

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
const sessionLost = new Set<Listener>();
const unavailable = new Set<Listener>();

const set = (partial: Partial<SessionState>): void => {
  state = { ...state, ...partial };
  for (const subscriber of [...subscribers]) subscriber();
};

const actions = {
  load: vi.fn(async () => undefined),
  reload: vi.fn(async () => undefined),
  signOut: vi.fn(async () => undefined),
  clear: vi.fn(() => set({ ...visitor })),
  startUnreadPolling: vi.fn(),
  stopUnreadPolling: vi.fn(),
};

/** Drives the session contract from a test: state, actions (spies) and BFF events. */
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
  emitSessionLost: (): void => [...sessionLost].forEach((listener) => listener()),
  emitUnavailable: (): void => [...unavailable].forEach((listener) => listener()),
  reset: (): void => {
    state = visitor;
    sessionLost.clear();
    unavailable.clear();
    Object.values(actions).forEach((action) => action.mockClear());
  },
};

/** Module replacement for `vi.mock('@/shell/session-stub', ...)`. */
export function fakeSessionModule() {
  return {
    useSession: (): SessionState =>
      useSyncExternalStore(
        (subscriber) => {
          subscribers.add(subscriber);
          return () => subscribers.delete(subscriber);
        },
        () => state,
      ),
    session: actions,
    onSessionLost: (listener: Listener) => {
      sessionLost.add(listener);
      return () => sessionLost.delete(listener);
    },
    onUnavailable: (listener: Listener) => {
      unavailable.add(listener);
      return () => unavailable.delete(listener);
    },
  };
}
