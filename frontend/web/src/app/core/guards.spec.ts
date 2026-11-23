import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  RedirectCommand,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { authGuard, guestGuard, profileGuard, unregisteredGuard } from './guards';

describe('guards', () => {
  let http: HttpTestingController;
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const tick = () => new Promise((resolve) => setTimeout(resolve));

  const run = (guard: typeof authGuard) =>
    TestBed.runInInjectionContext(() => guard(route, state)) as Promise<boolean | UrlTree>;

  const urlOf = (result: boolean | UrlTree) =>
    result instanceof UrlTree ? result.toString() : result;

  const guards = { authGuard, profileGuard, unregisteredGuard, guestGuard };
  for (const [name, guard] of Object.entries(guards)) {
    for (const failing of ['/bff/auth/session', '/bff/me']) {
      it(`${name} sends a 503 of ${failing} to the error screen`, async () => {
        const result = run(guard);
        const session = http.expectOne('/bff/auth/session');
        if (failing === '/bff/me') {
          session.flush({ authenticated: true, email: 'a@b.com' });
          await tick();
        }
        (failing === '/bff/me' ? http.expectOne(failing) : session).flush(
          {},
          { status: 503, statusText: 'Service Unavailable' },
        );
        const redirect = (await result) as unknown as RedirectCommand;
        expect(redirect).toBeInstanceOf(RedirectCommand);
        expect(redirect.redirectTo.toString()).toBe('/indisponivel');
        // The address stays, so that a reload of the tab tries the same page again.
        expect(redirect.navigationBehaviorOptions?.skipLocationChange).toBe(true);
      });
    }
  }

  it('authGuard sends an anonymous visitor to Entrar', async () => {
    const result = run(authGuard);
    http.expectOne('/bff/auth/session').flush({ authenticated: false });
    expect(urlOf(await result)).toBe('/entrar');
  });

  it('authGuard lets an authenticated visitor through', async () => {
    const result = run(authGuard);
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'a@b.com' });
    await tick();
    http.expectOne('/bff/me').flush({ customer: null, account: null });
    expect(await result).toBe(true);
  });

  it('profileGuard sends a user without customer to Completar cadastro', async () => {
    const result = run(profileGuard);
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'a@b.com' });
    await tick();
    http.expectOne('/bff/me').flush({ customer: null, account: null });
    expect(urlOf(await result)).toBe('/completar-cadastro');
  });

  it('profileGuard lets a registered customer through', async () => {
    const result = run(profileGuard);
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'a@b.com' });
    await tick();
    http.expectOne('/bff/me').flush({
      customer: { name: 'Ana', email: 'a@b.com', phone: '+5511987654321' },
      account: null,
    });
    expect(await result).toBe(true);
  });

  it('unregisteredGuard sends a registered customer to Início', async () => {
    const result = run(unregisteredGuard);
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'a@b.com' });
    await tick();
    http.expectOne('/bff/me').flush({
      customer: { name: 'Ana', email: 'a@b.com', phone: '+5511987654321' },
      account: null,
    });
    expect(urlOf(await result)).toBe('/inicio');
  });

  it('unregisteredGuard lets a user without customer through', async () => {
    const result = run(unregisteredGuard);
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'a@b.com' });
    await tick();
    http.expectOne('/bff/me').flush({ customer: null, account: null });
    expect(await result).toBe(true);
  });

  it('unregisteredGuard sends an anonymous visitor to Entrar', async () => {
    const result = run(unregisteredGuard);
    http.expectOne('/bff/auth/session').flush({ authenticated: false });
    expect(urlOf(await result)).toBe('/entrar');
  });

  it('profileGuard sends an anonymous visitor to Entrar', async () => {
    const result = run(profileGuard);
    http.expectOne('/bff/auth/session').flush({ authenticated: false });
    expect(urlOf(await result)).toBe('/entrar');
  });

  it('guestGuard sends an authenticated visitor into the app', async () => {
    const result = run(guestGuard);
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'a@b.com' });
    await tick();
    http.expectOne('/bff/me').flush({ customer: null, account: null });
    expect(urlOf(await result)).toBe('/inicio');
  });

  it('guestGuard lets an anonymous visitor see Entrar', async () => {
    const result = run(guestGuard);
    http.expectOne('/bff/auth/session').flush({ authenticated: false });
    expect(await result).toBe(true);
  });
});
