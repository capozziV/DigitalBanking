import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { SessionService } from './session.service';

describe('SessionService', () => {
  let http: HttpTestingController;
  let service: SessionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(SessionService);
  });

  afterEach(() => http.verify());

  const tick = () => new Promise((resolve) => setTimeout(resolve));

  it('loads an anonymous session', async () => {
    const promise = service.ensureLoaded();
    http.expectOne('/bff/auth/session').flush({ authenticated: false });
    await promise;
    expect(service.authenticated()).toBe(false);
    expect(service.loaded()).toBe(true);
  });

  it('loads the profile when authenticated', async () => {
    const promise = service.ensureLoaded();
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'a@b.com' });
    await tick();
    http.expectOne('/bff/me').flush({ customer: null, account: null });
    await promise;
    expect(service.authenticated()).toBe(true);
    expect(service.email()).toBe('a@b.com');
    expect(service.customer()).toBeNull();
    expect(service.profileLoaded()).toBe(true);
  });

  it('treats a failed session call as anonymous', async () => {
    const promise = service.ensureLoaded();
    http.expectOne('/bff/auth/session').flush('<html>', { status: 404, statusText: 'Not Found' });
    await promise;
    expect(service.authenticated()).toBe(false);
  });

  it('keeps the session when the BFF is unavailable (503)', async () => {
    service.authenticated.set(true);
    service.email.set('a@b.com');
    const promise = service.reload();
    http
      .expectOne('/bff/auth/session')
      .flush({}, { status: 503, statusText: 'Service Unavailable' });
    await promise;
    expect(service.authenticated()).toBe(true);
    expect(service.email()).toBe('a@b.com');
    expect(service.loadFailed()).toBe(true);
  });

  it('clears the session when the BFF answers 401', async () => {
    service.authenticated.set(true);
    const promise = service.reload();
    http.expectOne('/bff/auth/session').flush({}, { status: 401, statusText: 'Unauthorized' });
    await promise;
    expect(service.authenticated()).toBe(false);
    expect(service.loadFailed()).toBe(false);
  });

  it('does not reload a loaded session', async () => {
    const first = service.ensureLoaded();
    http.expectOne('/bff/auth/session').flush({ authenticated: false });
    await first;
    await service.ensureLoaded();
    http.expectNone('/bff/auth/session');
  });

  it('signs in and then loads the session', async () => {
    const promise = service.signIn('a@b.com', '123456');
    const req = http.expectOne('/bff/auth/sign-in');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@b.com', password: '123456' });
    req.flush({});
    await tick();
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'a@b.com' });
    await tick();
    http.expectOne('/bff/me').flush({
      customer: { name: 'Ana', email: 'a@b.com', phone: '+5511987654321' },
      account: null,
    });
    await promise;
    expect(service.customer()?.name).toBe('Ana');
  });

  it('signs up and reports whether a code is required', async () => {
    const promise = service.signUp('a@b.com', '123456');
    http.expectOne('/bff/auth/sign-up').flush({ confirmationRequired: true });
    expect(await promise).toEqual({ confirmationRequired: true });
  });

  it('confirms the sign up code', async () => {
    const promise = service.confirm('a@b.com', '987654');
    const req = http.expectOne('/bff/auth/confirm');
    expect(req.request.body).toEqual({ email: 'a@b.com', code: '987654' });
    req.flush({});
    await promise;
  });

  it('signs out, clears the state and goes to Entrar', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    service.authenticated.set(true);
    service.email.set('a@b.com');
    service.customer.set({ name: 'Ana', email: 'a@b.com', phone: '+5511987654321' });
    const promise = service.signOut();
    http.expectOne('/bff/auth/sign-out').flush({});
    await promise;
    expect(service.authenticated()).toBe(false);
    expect(service.email()).toBeNull();
    expect(service.customer()).toBeNull();
    expect(service.loaded()).toBe(true);
    expect(navigate).toHaveBeenCalledWith('/entrar');
  });

  for (const status of [503, 500, 403, 0]) {
    it(`does not declare the sign-out done when the POST fails with ${status}`, async () => {
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
      service.authenticated.set(true);
      service.email.set('a@b.com');
      const promise = service.signOut();
      const req = http.expectOne('/bff/auth/sign-out');
      if (status === 0) req.error(new ProgressEvent('error'));
      else req.flush({}, { status, statusText: 'Error' });
      await promise;
      expect(service.authenticated()).toBe(true);
      expect(service.email()).toBe('a@b.com');
      expect(service.signOutFailed()).toBe(true);
      expect(navigate).not.toHaveBeenCalled();
    });
  }

  it('counts a session that no longer exists (401) as signed out', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    service.authenticated.set(true);
    const promise = service.signOut();
    http.expectOne('/bff/auth/sign-out').flush({}, { status: 401, statusText: 'Unauthorized' });
    await promise;
    expect(service.authenticated()).toBe(false);
    expect(service.signOutFailed()).toBe(false);
    expect(navigate).toHaveBeenCalledWith('/entrar');
  });

  it('forgets a failed load when the session is cleared', () => {
    service.loadFailed.set(true);
    service.clear();
    expect(service.loadFailed()).toBe(false);
  });

  it('forgets a failed sign-out when the session is cleared', () => {
    service.signOutFailed.set(true);
    service.clear();
    expect(service.signOutFailed()).toBe(false);
  });

  it('loads the unread count', async () => {
    const promise = service.refreshUnread();
    http.expectOne('/bff/notifications/unread-count').flush({ count: 3 });
    await promise;
    expect(service.unreadCount()).toBe(3);
  });

  it('keeps the last unread count when the call fails', async () => {
    service.unreadCount.set(4);
    const promise = service.refreshUnread();
    http.expectOne('/bff/notifications/unread-count').flush('x', { status: 500, statusText: 'x' });
    await promise;
    expect(service.unreadCount()).toBe(4);
  });

  it('does not overlap unread calls', async () => {
    const first = service.refreshUnread();
    const second = service.refreshUnread();
    http.expectOne('/bff/notifications/unread-count').flush({ count: 1 });
    await Promise.all([first, second]);
    expect(service.unreadCount()).toBe(1);
  });

  it('does not let a count requested before marking as read overwrite the new one', async () => {
    const stale = service.refreshUnread();
    const fresh = service.refreshUnread({ fresh: true });
    const calls = http.match('/bff/notifications/unread-count');
    expect(calls).toHaveLength(2);
    calls[1].flush({ count: 0 });
    await fresh;
    calls[0].flush({ count: 3 });
    await stale;
    expect(service.unreadCount()).toBe(0);
  });

  it('keeps one call in flight after a fresh read finishes behind a stale one', async () => {
    const stale = service.refreshUnread();
    const fresh = service.refreshUnread({ fresh: true });
    const calls = http.match('/bff/notifications/unread-count');
    calls[0].flush({ count: 3 });
    await stale;
    const next = service.refreshUnread();
    expect(http.match('/bff/notifications/unread-count')).toHaveLength(0);
    calls[1].flush({ count: 0 });
    await Promise.all([fresh, next]);
    expect(service.unreadCount()).toBe(0);
  });

  it('ignores a count that arrives after clear and does not reuse that call for the next user', async () => {
    const stale = service.refreshUnread();
    const old = http.expectOne('/bff/notifications/unread-count');
    service.clear();
    const next = service.refreshUnread();
    const current = http.expectOne('/bff/notifications/unread-count');
    old.flush({ count: 7 });
    await stale;
    expect(service.unreadCount()).toBe(0);
    current.flush({ count: 2 });
    await next;
    expect(service.unreadCount()).toBe(2);
  });

  describe('unread polling', () => {
    let visibility: DocumentVisibilityState;

    beforeEach(() => {
      visibility = 'visible';
      vi.useFakeTimers();
      vi.spyOn(document, 'visibilityState', 'get').mockImplementation(() => visibility);
    });

    afterEach(() => {
      service.stopUnreadPolling();
      vi.useRealTimers();
      vi.restoreAllMocks();
    });

    const flushCount = async (count: number) => {
      http.expectOne('/bff/notifications/unread-count').flush({ count });
      await vi.advanceTimersByTimeAsync(0);
    };

    it('refreshes every 30 seconds while the tab is visible', async () => {
      service.startUnreadPolling();
      await vi.advanceTimersByTimeAsync(29_999);
      http.expectNone('/bff/notifications/unread-count');
      await vi.advanceTimersByTimeAsync(1);
      await flushCount(2);
      expect(service.unreadCount()).toBe(2);
      await vi.advanceTimersByTimeAsync(30_000);
      await flushCount(3);
      expect(service.unreadCount()).toBe(3);
    });

    it('skips the tick while the tab is hidden', async () => {
      service.startUnreadPolling();
      visibility = 'hidden';
      await vi.advanceTimersByTimeAsync(60_000);
      http.expectNone('/bff/notifications/unread-count');
    });

    it('refreshes right away when the tab becomes visible again', async () => {
      service.startUnreadPolling();
      document.dispatchEvent(new Event('visibilitychange'));
      await flushCount(5);
      expect(service.unreadCount()).toBe(5);
      visibility = 'hidden';
      document.dispatchEvent(new Event('visibilitychange'));
      http.expectNone('/bff/notifications/unread-count');
    });

    it('does not start a second timer when started twice', async () => {
      service.startUnreadPolling();
      service.startUnreadPolling();
      await vi.advanceTimersByTimeAsync(30_000);
      await flushCount(1);
    });

    it('stops when asked and when the session is cleared', async () => {
      service.startUnreadPolling();
      service.stopUnreadPolling();
      await vi.advanceTimersByTimeAsync(60_000);
      document.dispatchEvent(new Event('visibilitychange'));
      http.expectNone('/bff/notifications/unread-count');
      service.startUnreadPolling();
      service.clear();
      await vi.advanceTimersByTimeAsync(60_000);
      document.dispatchEvent(new Event('visibilitychange'));
      http.expectNone('/bff/notifications/unread-count');
    });
  });
});
