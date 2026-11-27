import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fakeSession } from '@/test/fake-session';
import { renderRoutes } from '@/test/render-routes';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());

const pathname = (router: ReturnType<typeof renderRoutes>['router']) =>
  router.state.location.pathname;

describe('UnavailablePage', () => {
  beforeEach(() => {
    fakeSession.reset();
    fakeSession.customer({ loadFailed: true });
  });

  it('says the service is unavailable', async () => {
    renderRoutes('/indisponivel');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Não foi possível carregar' }),
    ).toBeInTheDocument();
    expect(screen.getByText(/O serviço está indisponível no momento/)).toBeInTheDocument();
  });

  it('Unavailable "Tentar de novo" calls session.reload and leaves when it succeeds', async () => {
    fakeSession.actions.reload.mockImplementationOnce(async () => {
      fakeSession.set({ loadFailed: false });
      return undefined;
    });
    const { router } = renderRoutes('/inicio');
    await waitFor(() => expect(pathname(router)).toBe('/indisponivel'));
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Tentar de novo' }));
    expect(fakeSession.actions.reload).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(pathname(router)).toBe('/inicio'));
  });

  it('returns to the page it came from, not the start, after a successful retry', async () => {
    fakeSession.actions.reload.mockImplementationOnce(async () => {
      fakeSession.set({ loadFailed: false });
      return undefined;
    });
    const { router } = renderRoutes('/extrato');
    await waitFor(() => expect(pathname(router)).toBe('/indisponivel'));
    expect(router.state.location.state).toEqual({ from: '/extrato' });
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Tentar de novo' }));
    await waitFor(() => expect(pathname(router)).toBe('/extrato'));
    expect(pathname(router)).not.toBe('/inicio');
  });

  it('stays on the screen while the reload still fails', async () => {
    fakeSession.actions.reload.mockImplementationOnce(async () => undefined);
    const visited: string[] = [];
    const { router } = renderRoutes('/indisponivel', (path) => visited.push(path));
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Tentar de novo' }));
    expect(fakeSession.actions.reload).toHaveBeenCalledTimes(1);
    await act(async () => undefined);
    expect(visited).toEqual([]);
    expect(pathname(router)).toBe('/indisponivel');
    expect(screen.getByRole('button', { name: 'Tentar de novo' })).toBeEnabled();
  });

  it('goes to the start when there is no address to return to', async () => {
    fakeSession.actions.reload.mockImplementationOnce(async () => {
      fakeSession.set({ loadFailed: false });
      return undefined;
    });
    const { router } = renderRoutes('/indisponivel');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Tentar de novo' }));
    await waitFor(() => expect(pathname(router)).toBe('/inicio'));
  });
});
