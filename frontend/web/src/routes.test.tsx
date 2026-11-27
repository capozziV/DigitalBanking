import { screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fakeSession } from '@/test/fake-session';
import { renderRoutes } from '@/test/render-routes';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());

describe('routes', () => {
  beforeEach(() => {
    fakeSession.reset();
    fakeSession.customer();
  });

  it('redirects the old /avisos to /notificacoes', async () => {
    const { router } = renderRoutes('/avisos');
    await waitFor(() => expect(router.state.location.pathname).toBe('/notificacoes'));
    expect(await screen.findByRole('heading', { name: 'Notificações' })).toBeInTheDocument();
  });

  it('sends an unknown address to /, which opens Início', async () => {
    const visited: string[] = [];
    const { router } = renderRoutes('/qualquer', (pathname) => visited.push(pathname));
    await waitFor(() => expect(router.state.location.pathname).toBe('/inicio'));
    expect(visited[0]).toBe('/');
  });

  it.each([
    ['/inicio', 'Início'],
    ['/extrato', 'Extrato'],
    ['/transferir', 'Transferir'],
    ['/notificacoes', 'Notificações'],
    ['/minha-conta', 'Minha conta'],
  ])('opens %s with the title "%s · capzzPay"', async (path, title) => {
    renderRoutes(path);
    expect(await screen.findByRole('heading', { name: title })).toBeInTheDocument();
    await waitFor(() => expect(document.title).toBe(`${title} · capzzPay`));
  });

  it.each([
    ['/entrar', 'Entrar'],
    ['/criar-conta', 'Criar conta'],
  ])('titles the visitor screen %s', async (path, title) => {
    fakeSession.visitor();
    renderRoutes(path);
    await waitFor(() => expect(document.title).toBe(`${title} · capzzPay`));
  });

  it('titles Completar cadastro and Indisponível', async () => {
    fakeSession.signedInWithoutCustomer();
    const { router } = renderRoutes('/completar-cadastro');
    await waitFor(() => expect(document.title).toBe('Completar cadastro · capzzPay'));
    await router.navigate('/indisponivel');
    await waitFor(() => expect(document.title).toBe('Indisponível · capzzPay'));
  });
});
