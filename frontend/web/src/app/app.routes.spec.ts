import { Location } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router, type Route } from '@angular/router';
import { App } from './app';
import { authInterceptor } from './core/auth.interceptor';
import { setInput, submit, tick } from './testing/dom';
import { AppShellComponent } from './shell/app-shell.component';
import { routes } from './app.routes';

@Component({ template: '' })
class BlankComponent {}

describe('routes', () => {
  it('redirects the old /avisos path to /notificacoes', async () => {
    // Same child routes of the shell, minus guards and lazy pages: only the redirect is under test.
    const shell = routes.find((route) => route.component === AppShellComponent);
    const children: Route[] = (shell?.children ?? []).map((child) =>
      child.redirectTo === undefined ? { path: child.path, component: BlankComponent } : child,
    );
    TestBed.configureTestingModule({ providers: [provideRouter(children)] });
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/avisos');
    expect(router.url).toBe('/notificacoes');
  });

  describe('with the BFF failing', () => {
    let fixture: ComponentFixture<App>;
    let http: HttpTestingController;
    let location: Location;
    const el = () => fixture.nativeElement as HTMLElement;

    async function settle(): Promise<void> {
      for (let turn = 0; turn < 5; turn++) {
        await tick();
        fixture.detectChanges();
      }
    }

    async function open(url: string): Promise<void> {
      TestBed.configureTestingModule({
        imports: [App],
        providers: [
          provideHttpClient(withInterceptors([authInterceptor])),
          provideHttpClientTesting(),
          provideRouter(routes),
        ],
      });
      http = TestBed.inject(HttpTestingController);
      location = TestBed.inject(Location);
      location.go(url);
      fixture = TestBed.createComponent(App);
      TestBed.inject(Router).initialNavigation();
      await settle();
    }

    async function fail(endpoint: string): Promise<void> {
      if (endpoint === '/bff/me') {
        http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'a@b.com' });
        await settle();
      }
      http.expectOne(endpoint).flush({}, { status: 503, statusText: 'Service Unavailable' });
      await settle();
    }

    function expectErrorScreen(url: string): void {
      expect(el().querySelector('h1')?.textContent).toContain('Não foi possível carregar');
      expect(location.path()).toBe(url);
    }

    async function retry(): Promise<void> {
      const button = el().querySelector('button') as HTMLButtonElement;
      expect(button.textContent).toContain('Tentar de novo');
      expect(button.disabled).toBe(false);
      button.click();
      await settle();
    }

    afterEach(() => {
      try {
        http.verify();
      } finally {
        TestBed.resetTestingModule();
      }
    });

    for (const endpoint of ['/bff/auth/session', '/bff/me']) {
      for (const url of ['/extrato', '/entrar', '/completar-cadastro']) {
        it(`shows the error screen on ${url} with ${endpoint} in 503, without a loop`, async () => {
          await open(url);
          await fail(endpoint);
          expectErrorScreen(url);
          http.expectNone('/bff/auth/session');
          await retry();
          await fail(endpoint);
          expectErrorScreen(url);
          http.expectNone('/bff/auth/session');
          await retry();
          http.expectOne('/bff/auth/session').flush({ authenticated: false });
          await settle();
          expect(el().querySelector('h1')?.textContent).toContain('Entrar');
        });
      }
    }

    it('opens the page that was asked when the retry works', async () => {
      await open('/completar-cadastro');
      await fail('/bff/me');
      expectErrorScreen('/completar-cadastro');
      await retry();
      http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'a@b.com' });
      await settle();
      http.expectOne('/bff/me').flush({ customer: null, account: null });
      await settle();
      expect(el().querySelector('h1')?.textContent).toContain('Completar cadastro');
    });

    it('shows the error screen after a right sign-in with the profile failing', async () => {
      await open('/entrar');
      http.expectOne('/bff/auth/session').flush({ authenticated: false });
      await settle();
      setInput(fixture, 'email', 'a@b.com');
      setInput(fixture, 'password', '123456');
      await settle();
      submit(fixture);
      http.expectOne('/bff/auth/sign-in').flush({});
      await settle();
      await fail('/bff/me'); // the load of the sign-in
      await fail('/bff/me'); // the guard of Início tries once more
      expectErrorScreen('/entrar');
    });

    it('retries from the start when the error address itself was opened', async () => {
      await open('/indisponivel');
      await retry();
      http.expectOne('/bff/auth/session').flush({ authenticated: false });
      await settle();
      expect(location.path()).toBe('/entrar');
    });
  });
});
