import { act, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fakeSession } from '@/test/fake-session';
import { renderRoutes } from '@/test/render-routes';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());

const pathname = (router: ReturnType<typeof renderRoutes>['router']) =>
  router.state.location.pathname;

describe('guards', () => {
  beforeEach(() => fakeSession.reset());

  it('sends a visitor on a customer screen to Entrar', async () => {
    fakeSession.visitor();
    const { router } = renderRoutes('/inicio');
    await waitFor(() => expect(pathname(router)).toBe('/entrar'));
    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
  });

  it('sends a signed-in user without customer to Completar cadastro', async () => {
    fakeSession.signedInWithoutCustomer();
    const { router } = renderRoutes('/inicio');
    await waitFor(() => expect(pathname(router)).toBe('/completar-cadastro'));
  });

  it('lets a customer through to the screen', async () => {
    fakeSession.customer();
    renderRoutes('/extrato');
    expect(await screen.findByRole('heading', { name: 'Extrato' })).toBeInTheDocument();
  });

  it('sends a customer on Entrar or Criar conta into the app', async () => {
    fakeSession.customer();
    const { router } = renderRoutes('/entrar');
    await waitFor(() => expect(pathname(router)).toBe('/inicio'));
    await act(() => router.navigate('/criar-conta'));
    await waitFor(() => expect(pathname(router)).toBe('/inicio'));
  });

  it('lets a visitor see Entrar and Criar conta', async () => {
    fakeSession.visitor();
    const { router } = renderRoutes('/criar-conta');
    expect(await screen.findByRole('heading', { name: 'Criar conta' })).toBeInTheDocument();
    expect(pathname(router)).toBe('/criar-conta');
  });

  it('keeps Completar cadastro for signed-in users without customer', async () => {
    fakeSession.signedInWithoutCustomer();
    renderRoutes('/completar-cadastro');
    expect(await screen.findByRole('heading', { name: 'Completar cadastro' })).toBeInTheDocument();
  });

  it('sends a customer on Completar cadastro to Início', async () => {
    fakeSession.customer();
    const { router } = renderRoutes('/completar-cadastro');
    await waitFor(() => expect(pathname(router)).toBe('/inicio'));
  });

  it('sends a visitor on Completar cadastro to Entrar', async () => {
    fakeSession.visitor();
    const { router } = renderRoutes('/completar-cadastro');
    await waitFor(() => expect(pathname(router)).toBe('/entrar'));
  });

  it('loads the session first and decides only once it is loaded', async () => {
    fakeSession.set({ loaded: false });
    const { router } = renderRoutes('/inicio');
    await waitFor(() => expect(fakeSession.actions.load).toHaveBeenCalled());
    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    expect(pathname(router)).toBe('/inicio');
    act(() => fakeSession.customer());
    expect(await screen.findByRole('heading', { name: 'Início' })).toBeInTheDocument();
  });

  it('keeps a customer screen mounted while the session reloads', async () => {
    fakeSession.customer();
    renderRoutes('/extrato');
    const heading = await screen.findByRole('heading', { name: 'Extrato' });
    const shell = screen.getByRole('contentinfo');

    act(() => fakeSession.set({ loaded: false }));

    expect(heading).toBeInTheDocument();
    expect(shell).toBeInTheDocument();
    act(() => fakeSession.customer());
    expect(screen.getByRole('heading', { name: 'Extrato' })).toBe(heading);
  });

  it('keeps Entrar mounted while signing in, then goes into the app', async () => {
    fakeSession.visitor();
    const { router } = renderRoutes('/entrar');
    const heading = await screen.findByRole('heading', { name: 'Entrar' });

    // signIn: reload() marks the session unloaded and fills it in before it is loaded again.
    act(() => fakeSession.set({ loaded: false, authenticated: true, email: 'ana@example.test' }));
    expect(heading).toBeInTheDocument();
    expect(pathname(router)).toBe('/entrar');

    act(() => fakeSession.customer());
    await waitFor(() => expect(pathname(router)).toBe('/inicio'));
  });

  it.each(['/inicio', '/entrar', '/completar-cadastro'])(
    'sends a failed session load on %s to Indisponível, remembering the address',
    async (path) => {
      fakeSession.set({ loadFailed: true });
      const { router } = renderRoutes(path);
      await waitFor(() => expect(pathname(router)).toBe('/indisponivel'));
      expect(router.state.location.state).toEqual({ from: path });
    },
  );

  it('sends a lost session during a screen to Entrar exactly once', async () => {
    fakeSession.customer();
    const { router } = renderRoutes('/extrato');
    await screen.findByRole('heading', { name: 'Extrato' });
    const visits: string[] = [];
    router.subscribe((state) => visits.push(`${state.location.pathname}#${state.location.key}`));

    // Two 401s in a row: the real store clears the session on each one.
    act(() => {
      fakeSession.actions.clear();
      fakeSession.actions.clear();
    });

    await waitFor(() => expect(pathname(router)).toBe('/entrar'));
    expect(new Set(visits.filter((visit) => visit.startsWith('/entrar#'))).size).toBe(1);
    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
  });
});
