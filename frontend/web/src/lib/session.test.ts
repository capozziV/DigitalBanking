import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Customer } from './api-types';
import { createSessionStore, useSession } from './session';
import { FakeBff, tick } from './test-support';

const ana: Customer = { name: 'Ana', email: 'a@b.com', phone: '+5511987654321' };
const UNREAD = '/bff/notifications/unread-count';

describe('session store', () => {
  let fake: FakeBff;
  let session: ReturnType<typeof createSessionStore>;

  beforeEach(() => {
    fake = new FakeBff().install();
    session = createSessionStore();
  });

  afterEach(() => {
    session.dispose();
    vi.unstubAllGlobals();
  });

  const state = () => session.getSnapshot();

  describe('load', () => {
    it('loads an anonymous session', async () => {
      const promise = session.load();
      fake.only('/bff/auth/session').respond(200, { authenticated: false });
      await promise;
      expect(state().authenticated).toBe(false);
      expect(state().loaded).toBe(true);
    });

    it('loads the profile when authenticated', async () => {
      const promise = session.load();
      fake.only('/bff/auth/session').respond(200, { authenticated: true, email: 'a@b.com' });
      await tick();
      fake.only('/bff/me').respond(200, { customer: null, account: null });
      await promise;
      expect(state().authenticated).toBe(true);
      expect(state().email).toBe('a@b.com');
      expect(state().customer).toBeNull();
      expect(state().loaded).toBe(true);
    });

    it('treats a failed session call as anonymous', async () => {
      const promise = session.load();
      fake.only('/bff/auth/session').respond(404);
      await promise;
      expect(state().authenticated).toBe(false);
    });

    it('BFF down on load sets loadFailed', async () => {
      const promise = session.load();
      fake.only('/bff/auth/session').respond(503, {});
      await promise;
      expect(state().loadFailed).toBe(true);
      expect(state().loaded).toBe(false);
    });

    it('BFF unreachable on load sets loadFailed', async () => {
      const promise = session.load();
      fake.only('/bff/auth/session').fail();
      await promise;
      expect(state().loadFailed).toBe(true);
    });

    it('keeps the session when the BFF is unavailable (503)', async () => {
      const first = session.load();
      fake.only('/bff/auth/session').respond(200, { authenticated: true, email: 'a@b.com' });
      await tick();
      fake.only('/bff/me').respond(200, { customer: ana, account: null });
      await first;

      const promise = session.reload();
      fake.only('/bff/auth/session').respond(503, {});
      await promise;
      expect(state().authenticated).toBe(true);
      expect(state().email).toBe('a@b.com');
      expect(state().loadFailed).toBe(true);
    });

    it('clears the session when the BFF answers 401', async () => {
      const first = session.load();
      fake.only('/bff/auth/session').respond(200, { authenticated: true, email: 'a@b.com' });
      await tick();
      fake.only('/bff/me').respond(200, { customer: ana, account: null });
      await first;

      const promise = session.reload();
      fake.only('/bff/auth/session').respond(401, {});
      await promise;
      expect(state().authenticated).toBe(false);
      expect(state().loadFailed).toBe(false);
      expect(state().loaded).toBe(true);
    });

    it('does not reload a loaded session', async () => {
      const first = session.load();
      fake.only('/bff/auth/session').respond(200, { authenticated: false });
      await first;
      await session.load();
      expect(fake.pending('/bff/auth/session')).toHaveLength(0);
      expect(fake.calls).toHaveLength(1);
    });

    it('shares one call between two loads in flight', async () => {
      const a = session.load();
      const b = session.load();
      fake.only('/bff/auth/session').respond(200, { authenticated: false });
      await Promise.all([a, b]);
      expect(fake.calls).toHaveLength(1);
    });

    it('loadMe stores the customer and the account', async () => {
      const promise = session.loadMe();
      const account = {
        type: 'FREE',
        status: 'ACTIVE',
        balance: 1,
        overdraftLimit: 0,
        availableBalance: 1,
      };
      fake.only('/bff/me').respond(200, { customer: ana, account });
      await promise;
      expect(state().customer).toEqual(ana);
      expect(state().account).toEqual(account);
    });
  });

  describe('sign in, sign up, sign out', () => {
    it('signs in and then loads the session', async () => {
      const promise = session.signIn('a@b.com', '123456');
      const call = fake.only('/bff/auth/sign-in');
      expect(call.method).toBe('POST');
      expect(call.body).toEqual({ email: 'a@b.com', password: '123456' });
      call.respond(200, {});
      await tick();
      fake.only('/bff/auth/session').respond(200, { authenticated: true, email: 'a@b.com' });
      await tick();
      fake.only('/bff/me').respond(200, { customer: ana, account: null });
      await promise;
      expect(state().customer?.name).toBe('Ana');
    });

    it('a wrong password rejects and leaves the session alone', async () => {
      const promise = session.signIn('a@b.com', '123456');
      fake.only('/bff/auth/sign-in').respond(401, { detail: 'Senha incorreta' });
      await expect(promise).rejects.toMatchObject({ status: 401 });
      expect(state().authenticated).toBe(false);
      expect(state().loaded).toBe(false);
    });

    it('signs up and reports whether a code is required', async () => {
      const promise = session.signUp('a@b.com', '123456');
      const call = fake.only('/bff/auth/sign-up');
      expect(call.body).toEqual({ email: 'a@b.com', password: '123456' });
      call.respond(200, { confirmationRequired: true });
      expect(await promise).toEqual({ confirmationRequired: true });
    });

    it('confirms the sign up code', async () => {
      const promise = session.confirm('a@b.com', '987654');
      const call = fake.only('/bff/auth/confirm');
      expect(call.body).toEqual({ email: 'a@b.com', code: '987654' });
      call.respond(200, {});
      await promise;
    });

    const signedIn = async () => {
      const first = session.load();
      fake.only('/bff/auth/session').respond(200, { authenticated: true, email: 'a@b.com' });
      await tick();
      fake.only('/bff/me').respond(200, { customer: ana, account: null });
      await first;
    };

    it('signs out and clears the state', async () => {
      await signedIn();
      const promise = session.signOut();
      expect(fake.only('/bff/auth/sign-out').method).toBe('POST');
      fake.only('/bff/auth/sign-out').respond(200, {});
      await promise;
      expect(state().authenticated).toBe(false);
      expect(state().email).toBeNull();
      expect(state().customer).toBeNull();
      expect(state().loaded).toBe(true);
    });

    it.each([503, 500, 403, 0])(
      'does not declare the sign-out done when the POST fails with %d',
      async (status) => {
        await signedIn();
        const promise = session.signOut();
        const call = fake.only('/bff/auth/sign-out');
        if (status === 0) call.fail();
        else call.respond(status, {});
        await promise;
        expect(state().authenticated).toBe(true);
        expect(state().email).toBe('a@b.com');
        expect(state().signOutFailed).toBe(true);
      },
    );

    it('counts a session that no longer exists (401) as signed out', async () => {
      await signedIn();
      const promise = session.signOut();
      fake.only('/bff/auth/sign-out').respond(401, {});
      await promise;
      expect(state().authenticated).toBe(false);
      expect(state().signOutFailed).toBe(false);
    });

    it('forgets a failed sign-out when the next one works', async () => {
      await signedIn();
      const failing = session.signOut();
      fake.only('/bff/auth/sign-out').respond(503, {});
      await failing;
      const retry = session.signOut();
      expect(state().signOutFailed).toBe(false);
      fake.only('/bff/auth/sign-out').respond(200, {});
      await retry;
      expect(state().authenticated).toBe(false);
    });
  });

  describe('clear', () => {
    it('forgets a failed load when the session is cleared', async () => {
      const promise = session.load();
      fake.only('/bff/auth/session').respond(503, {});
      await promise;
      session.clear();
      expect(state().loadFailed).toBe(false);
      expect(state().loaded).toBe(true);
    });

    it('forgets a failed sign-out when the session is cleared', async () => {
      const promise = session.signOut();
      fake.only('/bff/auth/sign-out').respond(503, {});
      await promise;
      expect(state().signOutFailed).toBe(true);
      session.clear();
      expect(state().signOutFailed).toBe(false);
    });

    it('401 outside /bff/auth clears the session', async () => {
      const first = session.load();
      fake.only('/bff/auth/session').respond(200, { authenticated: true, email: 'a@b.com' });
      await tick();
      fake.only('/bff/me').respond(200, { customer: ana, account: null });
      await first;
      const promise = session.refreshUnread();
      fake.only(UNREAD).respond(401, {});
      await promise;
      expect(state().authenticated).toBe(false);
      expect(state().loaded).toBe(true);
    });

    it('401 on a sign-in does not clear anything', async () => {
      const promise = session.signIn('a@b.com', '000000').catch(() => undefined);
      fake.only('/bff/auth/sign-in').respond(401, {});
      await promise;
      expect(state().loaded).toBe(false);
    });
  });

  describe('unread count', () => {
    it('loads the unread count', async () => {
      const promise = session.refreshUnread();
      fake.only(UNREAD).respond(200, { count: 3 });
      await promise;
      expect(state().unreadCount).toBe(3);
    });

    it('keeps the last unread count when the call fails', async () => {
      const first = session.refreshUnread();
      fake.only(UNREAD).respond(200, { count: 4 });
      await first;
      const promise = session.refreshUnread();
      fake.only(UNREAD).respond(500, {});
      await promise;
      expect(state().unreadCount).toBe(4);
    });

    it('does not overlap unread calls', async () => {
      const first = session.refreshUnread();
      const second = session.refreshUnread();
      fake.only(UNREAD).respond(200, { count: 1 });
      await Promise.all([first, second]);
      expect(state().unreadCount).toBe(1);
    });

    it('does not let a count requested before marking as read overwrite the new one', async () => {
      const stale = session.refreshUnread();
      const fresh = session.refreshUnread({ fresh: true });
      const calls = fake.pending(UNREAD);
      expect(calls).toHaveLength(2);
      calls[1].respond(200, { count: 0 });
      await fresh;
      calls[0].respond(200, { count: 3 });
      await stale;
      expect(state().unreadCount).toBe(0);
    });

    it('keeps one call in flight after a fresh read finishes behind a stale one', async () => {
      const stale = session.refreshUnread();
      const fresh = session.refreshUnread({ fresh: true });
      const calls = fake.pending(UNREAD);
      calls[0].respond(200, { count: 3 });
      await stale;
      const next = session.refreshUnread();
      expect(fake.pending(UNREAD)).toHaveLength(1);
      calls[1].respond(200, { count: 0 });
      await Promise.all([fresh, next]);
      expect(state().unreadCount).toBe(0);
    });

    it('stale unread answer is discarded after clear', async () => {
      const stale = session.refreshUnread();
      const [old] = fake.pending(UNREAD);
      session.clear();
      const next = session.refreshUnread();
      const current = fake.pending(UNREAD)[1];
      old.respond(200, { count: 7 });
      await stale;
      expect(state().unreadCount).toBe(0);
      current.respond(200, { count: 2 });
      await next;
      expect(state().unreadCount).toBe(2);
    });
  });

  describe('unread polling', () => {
    let visibility: DocumentVisibilityState;

    beforeEach(() => {
      visibility = 'visible';
      vi.useFakeTimers();
      vi.spyOn(document, 'visibilityState', 'get').mockImplementation(() => visibility);
    });

    afterEach(() => {
      session.stopUnreadPolling();
      vi.useRealTimers();
      vi.restoreAllMocks();
    });

    const answer = async (count: number) => {
      fake.only(UNREAD).respond(200, { count });
      await vi.advanceTimersByTimeAsync(0);
    };

    it('refreshes every 30 seconds while the tab is visible', async () => {
      session.startUnreadPolling();
      await vi.advanceTimersByTimeAsync(29_999);
      expect(fake.pending(UNREAD)).toHaveLength(0);
      await vi.advanceTimersByTimeAsync(1);
      await answer(2);
      expect(state().unreadCount).toBe(2);
      await vi.advanceTimersByTimeAsync(30_000);
      await answer(3);
      expect(state().unreadCount).toBe(3);
    });

    it('skips the tick while the tab is hidden', async () => {
      session.startUnreadPolling();
      visibility = 'hidden';
      await vi.advanceTimersByTimeAsync(60_000);
      expect(fake.pending(UNREAD)).toHaveLength(0);
    });

    it('refreshes right away when the tab becomes visible again', async () => {
      session.startUnreadPolling();
      document.dispatchEvent(new Event('visibilitychange'));
      await answer(5);
      expect(state().unreadCount).toBe(5);
      visibility = 'hidden';
      document.dispatchEvent(new Event('visibilitychange'));
      expect(fake.pending(UNREAD)).toHaveLength(0);
    });

    it('does not start a second timer when started twice', async () => {
      session.startUnreadPolling();
      session.startUnreadPolling();
      await vi.advanceTimersByTimeAsync(30_000);
      await answer(1);
    });

    it('stops when asked and when the session is cleared', async () => {
      session.startUnreadPolling();
      session.stopUnreadPolling();
      await vi.advanceTimersByTimeAsync(60_000);
      document.dispatchEvent(new Event('visibilitychange'));
      expect(fake.pending(UNREAD)).toHaveLength(0);
      session.startUnreadPolling();
      session.clear();
      await vi.advanceTimersByTimeAsync(60_000);
      document.dispatchEvent(new Event('visibilitychange'));
      expect(fake.pending(UNREAD)).toHaveLength(0);
    });
  });

  describe('subscription', () => {
    it('notifies subscribers on a change and keeps the snapshot while nothing changes', async () => {
      const listener = vi.fn();
      const off = session.subscribe(listener);
      const before = state();
      expect(session.getSnapshot()).toBe(before);
      session.clear();
      expect(listener).toHaveBeenCalledTimes(1);
      expect(state()).not.toBe(before);
      const after = state();
      session.clear();
      expect(state()).toBe(after);
      expect(listener).toHaveBeenCalledTimes(1);
      off();
      const failing = session.signOut();
      fake.only('/bff/auth/sign-out').respond(503, {});
      await failing;
      expect(state().signOutFailed).toBe(true);
      expect(listener).toHaveBeenCalledTimes(1);
    });
  });
});

describe('useSession', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('renders the state of the module session and follows its changes', async () => {
    const fake = new FakeBff().install();
    const { refreshUnread } = await import('./session');
    const { result } = renderHook(() => useSession());
    const before = result.current.unreadCount;
    const promise = act(() => refreshUnread({ fresh: true }));
    fake.only(UNREAD).respond(200, { count: before + 4 });
    await promise;
    expect(result.current.unreadCount).toBe(before + 4);
  });
});
