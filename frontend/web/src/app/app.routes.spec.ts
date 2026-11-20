import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, type Route } from '@angular/router';
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
});
