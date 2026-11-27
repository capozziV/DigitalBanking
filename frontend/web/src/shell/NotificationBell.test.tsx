import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationBell } from '@/shell/NotificationBell';
import { FakeBff } from '@/lib/test-support';
import { fakeSession } from '@/test/fake-session';
import { renderWithProviders } from '@/test/render';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());

const notification = (id: string, readAt: string | null = null) => ({
  id,
  kind: 'TRANSFER_RECEIVED',
  title: `Título ${id}`,
  body: `Texto ${id}`,
  createdAt: '2026-11-19T15:00:00Z',
  readAt,
});

const page = (items: ReturnType<typeof notification>[], nextCursor: string | null = null) => ({
  items,
  unreadCount: items.length,
  nextCursor,
});

describe('NotificationBell', () => {
  let fake: FakeBff;

  beforeEach(() => {
    fakeSession.reset();
    fakeSession.customer();
    fake = new FakeBff().install();
  });

  afterEach(() => vi.unstubAllGlobals());

  const bell = () => screen.getByRole('button', { name: /^Notificações/ });
  const panel = () => screen.getByRole('dialog', { name: 'Notificações' });

  async function openWith(items: ReturnType<typeof notification>[], nextCursor: string | null) {
    const user = userEvent.setup();
    renderWithProviders(<NotificationBell />);
    await user.click(bell());
    await waitFor(() => expect(fake.pending('/bff/notifications')).toHaveLength(1));
    fake.only('/bff/notifications').respond(200, page(items, nextCursor));
    return user;
  }

  it('shows the unread counter on the bell, as text for screen readers too', () => {
    fakeSession.customer({ unreadCount: 3 });
    renderWithProviders(<NotificationBell />);
    expect(bell()).toHaveTextContent('3');
    expect(bell()).toHaveAccessibleName('Notificações, 3 não lidas');
  });

  it('opens a panel with the notifications and marks them read up to the newest', async () => {
    fakeSession.customer({ unreadCount: 2 });
    await openWith([notification('9'), notification('8')], null);
    expect(await within(panel()).findAllByRole('listitem')).toHaveLength(2);
    expect(bell()).toHaveAttribute('aria-expanded', 'true');
    await waitFor(() => expect(fake.pending('/bff/notifications/read')).toHaveLength(1));
    const read = fake.only('/bff/notifications/read');
    expect(read.method).toBe('POST');
    expect(read.body).toEqual({ upTo: '9' });
    read.respond(200, { marked: 2 });
    await waitFor(() =>
      expect(fakeSession.actions.refreshUnread).toHaveBeenCalledWith({ fresh: true }),
    );
    // The ones that were unread stay highlighted until the panel closes.
    expect(
      within(panel())
        .getAllByRole('listitem')
        .filter((row) => row.hasAttribute('data-unread')),
    ).toHaveLength(2);
  });

  it('does not mark anything when everything is already read', async () => {
    await openWith([notification('9', '2026-11-19T13:00:00Z')], null);
    await within(panel()).findAllByRole('listitem');
    expect(fake.calls.filter((call) => call.path === '/bff/notifications/read')).toHaveLength(0);
  });

  it('says so when there are no notifications', async () => {
    await openWith([], null);
    expect(await within(panel()).findByText('Nenhuma notificação por enquanto.')).toBeVisible();
    expect(fake.calls.filter((call) => call.path === '/bff/notifications/read')).toHaveLength(0);
  });

  function scrollList(scrollHeight: number, scrollTop: number): HTMLElement {
    const list = screen.getByTestId('notification-scroll');
    Object.defineProperty(list, 'scrollHeight', { value: scrollHeight, configurable: true });
    Object.defineProperty(list, 'clientHeight', { value: 300, configurable: true });
    list.scrollTop = scrollTop;
    return list;
  }

  it('fetches the next page by the cursor when the list is scrolled to the end', async () => {
    await openWith([notification('9', 'x')], 'c1');
    await within(panel()).findAllByRole('listitem');
    const list = scrollList(500, 200);
    fireEvent.scroll(list);
    fireEvent.scroll(list);
    await waitFor(() => expect(fake.pending('/bff/notifications?cursor=c1')).toHaveLength(1));
    fake.only('/bff/notifications?cursor=c1').respond(200, page([notification('1', 'x')]));
    await waitFor(() => expect(within(panel()).getAllByRole('listitem')).toHaveLength(2));
    fireEvent.scroll(list);
    expect(fake.calls.filter((call) => call.path.startsWith('/bff/notifications?'))).toHaveLength(
      1,
    );
  });

  it('does not fetch more before the end of the list', async () => {
    await openWith([notification('9', 'x')], 'c1');
    await within(panel()).findAllByRole('listitem');
    fireEvent.scroll(scrollList(1000, 0));
    expect(fake.calls.filter((call) => call.path.startsWith('/bff/notifications?'))).toHaveLength(
      0,
    );
  });

  it('shows the retry notice when the load fails and tries again', async () => {
    const user = userEvent.setup();
    renderWithProviders(<NotificationBell />);
    await user.click(bell());
    await waitFor(() => expect(fake.pending('/bff/notifications')).toHaveLength(1));
    fake.only('/bff/notifications').respond(503, {});
    expect(await within(panel()).findByText(/Não foi possível carregar\./)).toBeVisible();
    await user.click(within(panel()).getByRole('button', { name: 'Tentar de novo' }));
    await waitFor(() => expect(fake.pending('/bff/notifications')).toHaveLength(1));
    fake.only('/bff/notifications').respond(200, page([]));
    expect(await within(panel()).findByText('Nenhuma notificação por enquanto.')).toBeVisible();
  });

  it('closes with Escape and returns the focus to the bell', async () => {
    const user = await openWith([notification('9', 'x')], null);
    await within(panel()).findAllByRole('listitem');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(bell()).toHaveFocus();
  });

  it('reloads from the first page every time it opens', async () => {
    const user = await openWith([notification('9', 'x')], null);
    await within(panel()).findAllByRole('listitem');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await user.click(bell());
    await waitFor(() => expect(fake.pending('/bff/notifications')).toHaveLength(1));
    fake
      .only('/bff/notifications')
      .respond(200, page([notification('10', 'x'), notification('9', 'x')]));
    await waitFor(() => expect(within(panel()).getAllByRole('listitem')).toHaveLength(2));
  });

  it('links to the full screen', async () => {
    await openWith([notification('9', 'x')], null);
    expect(within(panel()).getByRole('link', { name: 'Ver todas' })).toHaveAttribute(
      'href',
      '/notificacoes',
    );
  });
});
