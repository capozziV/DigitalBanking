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
    const promise = service.signOut();
    http.expectOne('/bff/auth/sign-out').flush({});
    await promise;
    expect(service.authenticated()).toBe(false);
    expect(service.loaded()).toBe(true);
    expect(navigate).toHaveBeenCalledWith('/entrar');
  });

  it('loads the unread count', async () => {
    const promise = service.refreshUnread();
    http.expectOne('/bff/notifications/unread-count').flush({ count: 3 });
    await promise;
    expect(service.unreadCount()).toBe(3);
  });

  it('keeps the unread count at zero when the call fails', async () => {
    const promise = service.refreshUnread();
    http.expectOne('/bff/notifications/unread-count').flush('x', { status: 500, statusText: 'x' });
    await promise;
    expect(service.unreadCount()).toBe(0);
  });
});
