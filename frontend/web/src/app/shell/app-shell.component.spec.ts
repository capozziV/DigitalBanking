import { Component } from '@angular/core';
import { BreakpointObserver } from '@angular/cdk/layout';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { SessionService } from '../core/session.service';
import { tick } from '../testing/dom';
import { AppShellComponent } from './app-shell.component';

@Component({ template: '' })
class BlankComponent {}

describe('AppShellComponent', () => {
  let fixture: ComponentFixture<AppShellComponent>;
  let http: HttpTestingController;
  let wide: BehaviorSubject<{ matches: boolean; breakpoints: Record<string, boolean> }>;
  let queries: string[];

  const el = () => fixture.nativeElement as HTMLElement;

  async function create(matches: boolean) {
    wide = new BehaviorSubject({ matches, breakpoints: {} });
    queries = [];
    TestBed.configureTestingModule({
      imports: [AppShellComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', component: BlankComponent }]),
        {
          provide: BreakpointObserver,
          useValue: {
            observe: (query: string) => {
              queries.push(query);
              return wide.asObservable();
            },
          },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AppShellComponent);
    await fixture.whenStable();
    http.expectOne('/bff/notifications/unread-count').flush({ count: 2 });
    await tick();
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  it('shows the footer', async () => {
    await create(true);
    expect(el().querySelector('footer')?.textContent).toContain('© 2026 capzz.labs');
  });

  it('shows a discreet notice with a retry button only when the session load failed', async () => {
    await create(true);
    expect(el().textContent).not.toContain('Não foi possível carregar');
    const session = TestBed.inject(SessionService);
    session.loadFailed.set(true);
    fixture.detectChanges();
    expect(el().textContent).toContain('Não foi possível carregar. Tente de novo.');
    const reload = vi.spyOn(session, 'reload').mockResolvedValue();
    (el().querySelector('app-load-error button') as HTMLButtonElement).click();
    expect(reload).toHaveBeenCalled();
  });

  it('lists the five menu items', async () => {
    await create(true);
    const labels = Array.from(el().querySelectorAll('nav a')).map((a) => a.textContent ?? '');
    expect(labels).toHaveLength(5);
    for (const label of ['Início', 'Extrato', 'Transferir', 'Notificações', 'Minha conta']) {
      expect(labels.some((text) => text.includes(label))).toBe(true);
    }
  });

  it('shows the unread counter next to Notificações', async () => {
    await create(true);
    expect(el().querySelector('.badge .count')?.textContent?.trim()).toBe('2');
  });

  it('uses the 960 px breakpoint', async () => {
    await create(true);
    expect(queries).toEqual(['(min-width: 960px)']);
  });

  it('keeps the menu fixed and hides the hamburger at 960 px or wider', async () => {
    await create(true);
    expect(el().querySelector('.menu')?.classList).toContain('fixed');
    expect(el().querySelector('button.hamburger')).toBeNull();
  });

  it('collapses the menu below 960 px and opens it with the hamburger', async () => {
    await create(false);
    const menu = el().querySelector('.menu') as HTMLElement;
    expect(menu.classList).not.toContain('fixed');
    expect(menu.classList).not.toContain('open');
    (el().querySelector('button.hamburger') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(menu.classList).toContain('open');
    (el().querySelector('.backdrop') as HTMLElement).click();
    await fixture.whenStable();
    expect(menu.classList).not.toContain('open');
  });

  it('moves the focus to the first item when the menu opens and back to the hamburger on Escape', async () => {
    await create(false);
    document.body.appendChild(el());
    const hamburger = el().querySelector('button.hamburger') as HTMLButtonElement;
    hamburger.click();
    await fixture.whenStable();
    await tick();
    expect(document.activeElement).toBe(el().querySelector('nav a'));
    el()
      .querySelector('.menu')
      ?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await fixture.whenStable();
    expect(el().querySelector('.menu')?.classList).not.toContain('open');
    expect(document.activeElement).toBe(hamburger);
    el().remove();
  });

  it('reads the unread counter to screen readers as text', async () => {
    await create(true);
    const badge = el().querySelector('.badge') as HTMLElement;
    expect(badge.hasAttribute('aria-label')).toBe(false);
    expect(badge.querySelector('.visually-hidden')?.textContent).toContain('não lidas');
  });

  it('closes the collapsed menu after choosing an item', async () => {
    await create(false);
    (el().querySelector('button.hamburger') as HTMLButtonElement).click();
    await fixture.whenStable();
    (el().querySelector('nav a') as HTMLAnchorElement).click();
    await fixture.whenStable();
    expect(el().querySelector('.menu')?.classList).not.toContain('open');
  });

  it('shows Sair as a visible menu item with an icon, below 960 px too', async () => {
    for (const matches of [true, false]) {
      TestBed.resetTestingModule();
      await create(matches);
      const button = el().querySelector('button.sign-out') as HTMLButtonElement;
      expect(button.textContent).toContain('Sair');
      expect(button.querySelector('.glyph')).not.toBeNull();
      expect(el().querySelector('aside.menu')?.contains(button)).toBe(true);
    }
  });

  it('signs out from the menu: calls the BFF, clears the session and goes to Entrar', async () => {
    await create(true);
    const session = TestBed.inject(SessionService);
    session.authenticated.set(true);
    session.customer.set({ name: 'Ana', email: 'a@b.com', phone: '+5511987654321' });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    (el().querySelector('button.sign-out') as HTMLButtonElement).click();
    http.expectOne('/bff/auth/sign-out').flush({});
    await tick();
    expect(session.authenticated()).toBe(false);
    expect(session.customer()).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/entrar');
  });
});
