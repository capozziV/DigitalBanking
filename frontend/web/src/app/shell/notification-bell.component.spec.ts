import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { SessionService } from '../core/session.service';
import { tick } from '../testing/dom';
import { NotificationBellComponent } from './notification-bell.component';

@Component({ template: '' })
class BlankComponent {}

const notification = (id: string, readAt: string | null = null) => ({
  id,
  kind: 'TRANSFER_RECEIVED',
  title: `Título ${id}`,
  body: `Texto ${id}`,
  createdAt: '2026-11-19T12:00:00Z',
  readAt,
});

describe('NotificationBellComponent', () => {
  let fixture: ComponentFixture<NotificationBellComponent>;
  let http: HttpTestingController;
  let session: SessionService;
  const el = () => fixture.nativeElement as HTMLElement;
  const bell = () => el().querySelector('button.bell') as HTMLButtonElement;
  const panel = () => el().querySelector('.panel') as HTMLElement | null;

  async function settle(): Promise<void> {
    await tick();
    await fixture.whenStable();
  }

  async function create(): Promise<void> {
    TestBed.configureTestingModule({
      imports: [NotificationBellComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', component: BlankComponent }]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionService);
    fixture = TestBed.createComponent(NotificationBellComponent);
    await fixture.whenStable();
  }

  async function openWith(items: ReturnType<typeof notification>[], nextCursor: string | null) {
    bell().click();
    http.expectOne('/bff/notifications').flush({ items, unreadCount: items.length, nextCursor });
    await settle();
  }

  afterEach(() => http.verify());

  it('shows the unread counter on the bell, as text for screen readers too', async () => {
    await create();
    expect(el().querySelector('.bell-count')).toBeNull();
    session.unreadCount.set(3);
    fixture.detectChanges();
    expect(el().querySelector('.bell-count')?.textContent?.trim()).toBe('3');
    expect(el().querySelector('.bell-badge .visually-hidden')?.textContent).toContain('não lidas');
  });

  it('opens a panel with the notifications and marks them read up to the newest', async () => {
    await create();
    session.unreadCount.set(2);
    await openWith([notification('9'), notification('8')], null);
    expect(bell().getAttribute('aria-expanded')).toBe('true');
    const rows = panel()!.querySelectorAll('li');
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain('Título 9');
    const read = http.expectOne('/bff/notifications/read');
    expect(read.request.method).toBe('POST');
    expect(read.request.body).toEqual({ upTo: '9' });
    read.flush({ marked: 2 });
    await settle();
    http.expectOne('/bff/notifications/unread-count').flush({ count: 0 });
    await settle();
    expect(session.unreadCount()).toBe(0);
    // The ones that were unread stay highlighted until the panel closes.
    expect(panel()!.querySelectorAll('li.unread')).toHaveLength(2);
  });

  it('does not let a count requested before marking as read bring the number back', async () => {
    await create();
    session.unreadCount.set(3);
    await openWith([notification('9'), notification('8')], null);
    void session.refreshUnread();
    http.expectOne('/bff/notifications/read').flush({ marked: 2 });
    await settle();
    const calls = http.match('/bff/notifications/unread-count');
    calls[0].flush({ count: 3 });
    calls[calls.length - 1].flush({ count: 0 });
    await settle();
    expect(session.unreadCount()).toBe(0);
  });

  it('does not mark anything when everything is already read', async () => {
    await create();
    await openWith([notification('9', '2026-11-19T13:00:00Z')], null);
    http.expectNone('/bff/notifications/read');
  });

  it('says so when there are no notifications', async () => {
    await create();
    await openWith([], null);
    expect(panel()!.textContent).toContain('Nenhuma notificação por enquanto.');
    http.expectNone('/bff/notifications/read');
  });

  it('fetches the next page by the cursor when the list is scrolled to the end', async () => {
    await create();
    await openWith([notification('9', 'x')], 'c1');
    const list = el().querySelector('.scroll') as HTMLElement;
    Object.defineProperty(list, 'scrollHeight', { value: 500 });
    Object.defineProperty(list, 'clientHeight', { value: 300 });
    list.scrollTop = 200;
    list.dispatchEvent(new Event('scroll'));
    list.dispatchEvent(new Event('scroll'));
    const next = http.expectOne('/bff/notifications?cursor=c1');
    next.flush({ items: [notification('1', 'x')], unreadCount: 0, nextCursor: null });
    await settle();
    expect(panel()!.querySelectorAll('li')).toHaveLength(2);
    list.dispatchEvent(new Event('scroll'));
    http.expectNone((req) => req.url === '/bff/notifications');
  });

  it('does not fetch more before the end of the list', async () => {
    await create();
    await openWith([notification('9', 'x')], 'c1');
    const list = el().querySelector('.scroll') as HTMLElement;
    Object.defineProperty(list, 'scrollHeight', { value: 1000 });
    Object.defineProperty(list, 'clientHeight', { value: 300 });
    list.scrollTop = 0;
    list.dispatchEvent(new Event('scroll'));
    http.expectNone((req) => req.url.startsWith('/bff/notifications'));
  });

  it('shows the retry notice when the load fails and tries again', async () => {
    await create();
    bell().click();
    http.expectOne('/bff/notifications').flush({}, { status: 503, statusText: 'Unavailable' });
    await settle();
    expect(panel()!.textContent).toContain('Não foi possível carregar. Tente de novo.');
    (panel()!.querySelector('app-load-error button') as HTMLButtonElement).click();
    http.expectOne('/bff/notifications').flush({ items: [], unreadCount: 0, nextCursor: null });
    await settle();
    expect(panel()!.textContent).toContain('Nenhuma notificação');
  });

  it('closes with Escape and returns the focus to the bell', async () => {
    await create();
    document.body.appendChild(el());
    await openWith([notification('9', 'x')], null);
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await fixture.whenStable();
    expect(panel()).toBeNull();
    expect(document.activeElement).toBe(bell());
    el().remove();
  });

  it('closes on a click outside, but not on a click inside', async () => {
    await create();
    document.body.appendChild(el());
    await openWith([notification('9', 'x')], null);
    panel()!.click();
    await fixture.whenStable();
    expect(panel()).not.toBeNull();
    document.body.click();
    await fixture.whenStable();
    expect(panel()).toBeNull();
    el().remove();
  });

  it('closes when the bell is clicked again', async () => {
    await create();
    await openWith([notification('9', 'x')], null);
    bell().click();
    await fixture.whenStable();
    expect(panel()).toBeNull();
  });

  it('closes on navigation', async () => {
    await create();
    await openWith([notification('9', 'x')], null);
    await TestBed.inject(Router).navigateByUrl('/extrato');
    await fixture.whenStable();
    expect(panel()).toBeNull();
  });

  it('reloads from the first page every time it opens', async () => {
    await create();
    await openWith([notification('9', 'x')], null);
    bell().click();
    await fixture.whenStable();
    await openWith([notification('10', 'x'), notification('9', 'x')], null);
    expect(panel()!.querySelectorAll('li')).toHaveLength(2);
  });
});
