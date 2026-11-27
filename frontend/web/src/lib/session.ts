import { useSyncExternalStore } from 'react';
import type { AccountSummary, Customer, Me } from './api-types';
import { BffError, bff, onSessionLost } from './bff-client';

export type SessionState = {
  loaded: boolean;
  authenticated: boolean;
  email: string | null;
  customer: Customer | null;
  account: AccountSummary | null;
  unreadCount: number;
  /** The last load failed for a reason other than a lost session (BFF down, network). */
  loadFailed: boolean;
  /** The last sign-out was not confirmed by the BFF: the session cookie may still work. */
  signOutFailed: boolean;
};

interface SessionDTO {
  authenticated: boolean;
  email?: string | null;
}

const UNREAD_POLL_MS = 30_000;

const INITIAL: SessionState = {
  loaded: false,
  authenticated: false,
  email: null,
  customer: null,
  account: null,
  unreadCount: 0,
  loadFailed: false,
  signOutFailed: false,
};

const isSessionGone = (error: unknown): boolean =>
  error instanceof BffError && error.status === 401;

/**
 * Who is signed in. Tokens never reach the browser: only the BFF cookie does. Plain store (no
 * React): `subscribe` and `getSnapshot` fit `useSyncExternalStore`.
 */
export function createSessionStore() {
  let state: SessionState = INITIAL;
  const listeners = new Set<() => void>();

  let loading: Promise<void> | null = null;
  let refreshing: Promise<void> | null = null;
  /** Bumped to discard the answer of any unread call started before it. */
  let unreadGeneration = 0;
  let pollTimer: ReturnType<typeof setInterval> | null = null;

  const set = (patch: Partial<SessionState>): void => {
    const changed = (Object.keys(patch) as (keyof SessionState)[]).some(
      (key) => patch[key] !== state[key],
    );
    if (!changed) return;
    state = { ...state, ...patch };
    for (const listener of [...listeners]) listener();
  };

  const getSnapshot = (): SessionState => state;

  const subscribe = (listener: () => void): (() => void) => {
    listeners.add(listener);
    return () => listeners.delete(listener);
  };

  const discardUnreadInFlight = (): void => {
    unreadGeneration++;
    refreshing = null;
  };

  const pollIfVisible = (): void => {
    if (document.visibilityState === 'visible') void refreshUnread();
  };

  const startUnreadPolling = (): void => {
    if (pollTimer !== null) return;
    pollTimer = setInterval(pollIfVisible, UNREAD_POLL_MS);
    document.addEventListener('visibilitychange', pollIfVisible);
  };

  const stopUnreadPolling = (): void => {
    if (pollTimer === null) return;
    clearInterval(pollTimer);
    pollTimer = null;
    document.removeEventListener('visibilitychange', pollIfVisible);
  };

  /** Forgets the local state, as when the BFF says the session is gone. */
  const clear = (): void => {
    stopUnreadPolling();
    discardUnreadInFlight();
    set({ ...INITIAL, loaded: true });
  };

  const loadMe = async (): Promise<void> => {
    const me = await bff<Me>('/bff/me');
    set({ customer: me.customer, account: me.account });
  };

  const fetchSession = async (): Promise<void> => {
    try {
      const session = await bff<SessionDTO>('/bff/auth/session');
      if (!session.authenticated) {
        clear();
        return;
      }
      set({ authenticated: true, email: session.email ?? null });
      await loadMe();
      set({ loaded: true, loadFailed: false });
    } catch (error) {
      if (isSessionGone(error)) {
        clear();
        return;
      }
      // Transient failure (503, network): keep the state, so it does not look like a sign out.
      set({ loadFailed: true });
    }
  };

  /** Loads the session (and the profile when signed in) once; calls in flight are shared. */
  const load = (): Promise<void> => {
    if (state.loaded) return Promise.resolve();
    loading ??= fetchSession().finally(() => {
      loading = null;
    });
    return loading;
  };

  /** Forces a fresh load of the session and profile. */
  const reload = (): Promise<void> => {
    set({ loaded: false });
    return load();
  };

  const signIn = async (email: string, password: string): Promise<void> => {
    await bff('/bff/auth/sign-in', { method: 'POST', body: { email, password } });
    await reload();
  };

  const signUp = (email: string, password: string): Promise<{ confirmationRequired: boolean }> =>
    bff('/bff/auth/sign-up', { method: 'POST', body: { email, password } });

  const confirm = async (email: string, code: string): Promise<void> => {
    await bff('/bff/auth/confirm', { method: 'POST', body: { email, code } });
  };

  /** The cookie is HttpOnly, so only the BFF can end the session: a failure is not a sign-out. */
  const signOut = async (): Promise<void> => {
    set({ signOutFailed: false });
    try {
      await bff('/bff/auth/sign-out', { method: 'POST', body: {} });
    } catch (error) {
      if (!isSessionGone(error)) {
        set({ signOutFailed: true });
        return;
      }
    }
    clear();
  };

  const fetchUnread = async (generation: number): Promise<void> => {
    try {
      const result = await bff<{ count: number }>('/bff/notifications/unread-count');
      if (generation === unreadGeneration) set({ unreadCount: result.count });
    } catch {
      // Transient failure: keep the last number.
    }
  };

  /**
   * Reads the unread count; a call already in flight is reused, a failure keeps the last number.
   * With `fresh` (after marking as read) a call started earlier is discarded and a new one is made.
   */
  function refreshUnread(options?: { fresh?: boolean }): Promise<void> {
    if (options?.fresh) discardUnreadInFlight();
    if (refreshing === null) {
      const call: Promise<void> = fetchUnread(unreadGeneration).finally(() => {
        if (refreshing === call) refreshing = null;
      });
      refreshing = call;
    }
    return refreshing;
  }

  // The BFF says the session is gone (401 outside /bff/auth/): forget it here too.
  const offSessionLost = onSessionLost(clear);
  const dispose = (): void => {
    offSessionLost();
    stopUnreadPolling();
  };

  return {
    getSnapshot,
    subscribe,
    load,
    reload,
    loadMe,
    signIn,
    signUp,
    confirm,
    signOut,
    clear,
    refreshUnread,
    startUnreadPolling,
    stopUnreadPolling,
    dispose,
  };
}

const session = createSessionStore();

export const {
  getSnapshot,
  subscribe,
  load,
  reload,
  loadMe,
  signIn,
  signUp,
  confirm,
  signOut,
  clear,
  refreshUnread,
  startUnreadPolling,
  stopUnreadPolling,
} = session;

/** The only React in `src/lib`: the session as a hook. */
export function useSession(): SessionState {
  return useSyncExternalStore(subscribe, getSnapshot);
}
