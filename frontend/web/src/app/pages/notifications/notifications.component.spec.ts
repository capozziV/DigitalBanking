import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SessionService } from '../../core/session.service';
import { tick } from '../../testing/dom';
import { NotificationsComponent } from './notifications.component';

const notification = (id: string, readAt: string | null) => ({
  id,
  kind: 'TRANSFER_RECEIVED',
  title: `Título ${id}`,
  body: `Texto ${id}`,
  data: {},
  createdAt: '2026-11-19T12:00:00Z',
  readAt,
});

describe('NotificationsComponent', () => {
  let fixture: ComponentFixture<NotificationsComponent>;
  let http: HttpTestingController;
  const el = () => fixture.nativeElement as HTMLElement;
  const markAll = () =>
    Array.from(el().querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Marcar como lidas'),
    ) as HTMLButtonElement | undefined;

  async function settle(): Promise<void> {
    await tick();
    await fixture.whenStable();
  }

  async function create(): Promise<void> {
    TestBed.configureTestingModule({
      imports: [NotificationsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(NotificationsComponent);
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  it('lists the notifications with the unread ones in bold', async () => {
    await create();
    http.expectOne('/bff/notifications').flush({
      items: [notification('1', null), notification('2', '2026-11-19T13:00:00Z')],
      unreadCount: 1,
      nextCursor: null,
    });
    await settle();
    const rows = el().querySelectorAll('li');
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain('Título 1');
    expect(rows[0].textContent).toContain('Texto 1');
    expect(rows[0].classList).toContain('unread');
    expect(rows[1].classList).not.toContain('unread');
  });

  it('marks everything as read up to the newest notification and refreshes the counter', async () => {
    await create();
    const session = TestBed.inject(SessionService);
    session.unreadCount.set(1);
    http.expectOne('/bff/notifications').flush({
      items: [notification('1', null), notification('0', '2026-11-19T13:00:00Z')],
      unreadCount: 1,
      nextCursor: null,
    });
    await settle();
    markAll()?.click();
    const read = http.expectOne('/bff/notifications/read');
    expect(read.request.method).toBe('POST');
    expect(read.request.body).toEqual({ upTo: '1' });
    read.flush({ marked: 1 });
    await settle();
    http.expectOne('/bff/notifications').flush({
      items: [notification('1', '2026-11-19T14:00:00Z')],
      unreadCount: 0,
      nextCursor: null,
    });
    await settle();
    http.expectOne('/bff/notifications/unread-count').flush({ count: 0 });
    await settle();
    expect(session.unreadCount()).toBe(0);
    expect(el().querySelector('li')?.classList).not.toContain('unread');
    expect(markAll()).toBeUndefined();
  });

  it('does not let a count requested before marking as read bring the number back', async () => {
    await create();
    const session = TestBed.inject(SessionService);
    session.unreadCount.set(1);
    http.expectOne('/bff/notifications').flush({
      items: [notification('1', null)],
      unreadCount: 1,
      nextCursor: null,
    });
    await settle();
    void session.refreshUnread();
    markAll()?.click();
    http.expectOne('/bff/notifications/read').flush({ marked: 1 });
    await settle();
    http.expectOne('/bff/notifications').flush({
      items: [notification('1', '2026-11-19T14:00:00Z')],
      unreadCount: 0,
      nextCursor: null,
    });
    await settle();
    const calls = http.match('/bff/notifications/unread-count');
    calls[0].flush({ count: 1 });
    calls[calls.length - 1].flush({ count: 0 });
    await settle();
    expect(session.unreadCount()).toBe(0);
  });

  it('says so when there are no notifications', async () => {
    await create();
    http.expectOne('/bff/notifications').flush({ items: [], unreadCount: 0, nextCursor: null });
    await settle();
    expect(el().textContent).toContain('Nenhuma notificação');
  });

  it('shows the retry notice when the load fails', async () => {
    await create();
    http.expectOne('/bff/notifications').flush({}, { status: 503, statusText: 'Unavailable' });
    await settle();
    expect(el().textContent).toContain('Não foi possível carregar. Tente de novo.');
  });
});
