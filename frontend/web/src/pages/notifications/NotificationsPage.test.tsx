import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationsPage } from '@/pages/notifications/NotificationsPage';
import { FakeBff } from '@/lib/test-support';
import { fakeSession } from '@/test/fake-session';
import { renderWithProviders } from '@/test/render';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());

const notification = (id: string, readAt: string | null, createdAt = '2026-11-19T15:00:00Z') => ({
  id,
  kind: 'TRANSFER_RECEIVED',
  title: `Título ${id}`,
  body: `Texto ${id} de Maria Lima`,
  createdAt,
  readAt,
});

const page = (items: ReturnType<typeof notification>[]) => ({
  items,
  unreadCount: items.filter((item) => !item.readAt).length,
  nextCursor: null,
});

describe('NotificationsPage', () => {
  let fake: FakeBff;

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-11-19T18:00:00Z'));
    fakeSession.reset();
    fakeSession.customer();
    fake = new FakeBff().install();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  const open = async (items: ReturnType<typeof notification>[]) => {
    renderWithProviders(<NotificationsPage />);
    await waitFor(() => expect(fake.pending('/bff/notifications')).toHaveLength(1));
    fake.only('/bff/notifications').respond(200, page(items));
  };

  it('lists the notifications with the unread ones marked and the counterpart name in the text', async () => {
    await open([notification('1', null), notification('2', '2026-11-19T16:00:00Z')]);
    const rows = await screen.findAllByRole('listitem');
    expect(rows).toHaveLength(2);
    expect(rows[0]).toHaveTextContent('Título 1');
    expect(rows[0]).toHaveTextContent('Texto 1 de Maria Lima');
    expect(rows[0]).toHaveAttribute('data-unread', 'true');
    expect(within(rows[0]).getByText(/não lida/)).toBeInTheDocument();
    expect(rows[1]).not.toHaveAttribute('data-unread');
  });

  it('groups the notifications by day, in the São Paulo time zone', async () => {
    await open([
      notification('3', null, '2026-11-19T15:00:00Z'),
      // 01:00 UTC of the 19th is 22:00 of the 18th in São Paulo.
      notification('2', null, '2026-11-19T01:00:00Z'),
      notification('1', null, '2026-11-10T12:00:00Z'),
    ]);
    const groups = await screen.findAllByRole('heading', { level: 2 });
    expect(groups.map((group) => group.textContent)).toEqual(['Hoje', 'Ontem', '10 de novembro']);
    const today = groups[0].closest('section') as HTMLElement;
    expect(within(today).getAllByRole('listitem')).toHaveLength(1);
    expect(today).toHaveTextContent('12:00');
  });

  it('marks everything as read up to the newest notification and refreshes the counter', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    await open([notification('1', null), notification('0', '2026-11-19T13:00:00Z')]);
    await user.click(await screen.findByRole('button', { name: 'Marcar como lidas' }));
    const read = fake.only('/bff/notifications/read');
    expect(read.method).toBe('POST');
    expect(read.body).toEqual({ upTo: '1' });
    read.respond(200, { marked: 1 });
    await waitFor(() => expect(fake.pending('/bff/notifications')).toHaveLength(1));
    fake.only('/bff/notifications').respond(200, page([notification('1', '2026-11-19T14:00:00Z')]));
    await waitFor(() =>
      expect(fakeSession.actions.refreshUnread).toHaveBeenCalledWith({ fresh: true }),
    );
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Marcar como lidas' })).not.toBeInTheDocument(),
    );
    expect(screen.getByRole('listitem')).not.toHaveAttribute('data-unread');
  });

  it('shows the error in place when marking as read fails', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    await open([notification('1', null)]);
    await user.click(await screen.findByRole('button', { name: 'Marcar como lidas' }));
    fake.only('/bff/notifications/read').respond(500, { detail: 'Falhou no servidor.' });
    expect(await screen.findByRole('alert')).toHaveTextContent('Falhou no servidor.');
    expect(fakeSession.actions.refreshUnread).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Marcar como lidas' })).toBeEnabled();
  });

  it('does not offer to mark as read when everything is read', async () => {
    await open([notification('1', '2026-11-19T13:00:00Z')]);
    await screen.findByRole('listitem');
    expect(screen.queryByRole('button', { name: 'Marcar como lidas' })).not.toBeInTheDocument();
  });

  it('shows the designed empty state when there are no notifications', async () => {
    await open([]);
    expect(await screen.findByText('Nenhuma notificação por enquanto.')).toBeInTheDocument();
    expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Marcar como lidas' })).not.toBeInTheDocument();
  });

  it('shows the retry notice when the load fails and tries again', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    renderWithProviders(<NotificationsPage />);
    await waitFor(() => expect(fake.pending('/bff/notifications')).toHaveLength(1));
    fake.only('/bff/notifications').respond(503, {});
    expect(await screen.findByText(/Não foi possível carregar\. Tente de novo\./)).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }));
    await waitFor(() => expect(fake.pending('/bff/notifications')).toHaveLength(1));
    fake.only('/bff/notifications').respond(200, page([]));
    expect(await screen.findByText('Nenhuma notificação por enquanto.')).toBeInTheDocument();
  });
});
