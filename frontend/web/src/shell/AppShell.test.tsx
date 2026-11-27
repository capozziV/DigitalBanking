import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AppShell } from '@/shell/AppShell';
import { fakeSession } from '@/test/fake-session';
import { renderWithProviders } from '@/test/render';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());

const LINKS: [string, string][] = [
  ['Início', '/inicio'],
  ['Extrato', '/extrato'],
  ['Transferir', '/transferir'],
  ['Notificações', '/notificacoes'],
  ['Minha conta', '/minha-conta'],
];

describe('AppShell', () => {
  beforeEach(() => {
    fakeSession.reset();
    fakeSession.customer();
  });

  it('shows the unread count on the bell', () => {
    fakeSession.customer({ unreadCount: 3 });
    renderWithProviders(<AppShell />, { route: '/inicio' });
    const bell = screen.getAllByRole('button', { name: /^Notificações/ })[0];
    expect(bell).toHaveTextContent('3');
    expect(bell).toHaveAccessibleName('Notificações, 3 não lidas');
  });

  it('hides the count when everything is read and caps it at 99+', () => {
    const { unmount } = renderWithProviders(<AppShell />, { route: '/inicio' });
    expect(screen.getAllByRole('button', { name: 'Notificações' })[0]).not.toHaveTextContent(/\d/);
    unmount();
    fakeSession.customer({ unreadCount: 120 });
    renderWithProviders(<AppShell />, { route: '/inicio' });
    expect(screen.getAllByRole('button', { name: /^Notificações/ })[0]).toHaveTextContent('99+');
  });

  it.each([['Principal'], ['Abas']])(
    'lists the five destinations in the %s navigation, the count only on the bell',
    (label) => {
      fakeSession.customer({ unreadCount: 3 });
      renderWithProviders(<AppShell />, { route: '/inicio' });
      const nav = screen.getByRole('navigation', { name: label });
      for (const [name, href] of LINKS) {
        expect(within(nav).getByRole('link', { name })).toHaveAttribute('href', href);
      }
      expect(nav).not.toHaveTextContent('3');
    },
  );

  it('marks the current destination', () => {
    renderWithProviders(<AppShell />, { route: '/extrato' });
    const nav = screen.getByRole('navigation', { name: 'Principal' });
    expect(within(nav).getByRole('link', { name: 'Extrato' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('polls the unread count while mounted', () => {
    const { unmount } = renderWithProviders(<AppShell />, { route: '/inicio' });
    expect(fakeSession.actions.startUnreadPolling).toHaveBeenCalledTimes(1);
    unmount();
    expect(fakeSession.actions.stopUnreadPolling).toHaveBeenCalledTimes(1);
  });

  it('reads the unread count right away, then polls it', () => {
    renderWithProviders(<AppShell />, { route: '/inicio' });
    expect(fakeSession.actions.refreshUnread).toHaveBeenCalledTimes(1);
    expect(fakeSession.actions.refreshUnread.mock.invocationCallOrder[0]).toBeLessThan(
      fakeSession.actions.startUnreadPolling.mock.invocationCallOrder[0],
    );
  });

  it('shows the footer', () => {
    renderWithProviders(<AppShell />, { route: '/inicio' });
    expect(screen.getByRole('contentinfo')).toHaveTextContent('© 2026 capzz.labs');
  });

  it('signs out from the side navigation', async () => {
    renderWithProviders(<AppShell />, { route: '/inicio' });
    await userEvent.click(screen.getByRole('button', { name: 'Sair' }));
    expect(fakeSession.actions.signOut).toHaveBeenCalledTimes(1);
  });

  it('warns when the sign-out was not confirmed and lets the user try again', async () => {
    fakeSession.customer({ signOutFailed: true });
    renderWithProviders(<AppShell />, { route: '/inicio' });
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Não foi possível confirmar a saída');
    await userEvent.click(within(alert).getByRole('button', { name: 'Tentar de novo' }));
    expect(fakeSession.actions.signOut).toHaveBeenCalledTimes(1);
  });

  it('opens the bell panel with a link to all notifications', async () => {
    fakeSession.customer({ unreadCount: 2 });
    renderWithProviders(<AppShell />, { route: '/inicio' });
    await userEvent.click(screen.getAllByRole('button', { name: /^Notificações/ })[0]);
    const panel = await screen.findByRole('dialog', { name: 'Notificações' });
    expect(within(panel).getByRole('link', { name: 'Ver todas' })).toHaveAttribute(
      'href',
      '/notificacoes',
    );
  });
});
