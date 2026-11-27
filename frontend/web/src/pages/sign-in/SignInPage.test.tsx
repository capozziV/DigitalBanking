import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BffError } from '@/lib/bff-client';
import { fakeSession } from '@/test/fake-session';
import { renderRoutes } from '@/test/render-routes';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());

const pathname = (router: ReturnType<typeof renderRoutes>['router']) =>
  router.state.location.pathname;

async function fill(email: string, password: string) {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText('E-mail'), email);
  if (password) await user.type(screen.getByLabelText('Senha (6 dígitos)'), password);
  return user;
}

describe('SignInPage', () => {
  beforeEach(() => {
    fakeSession.reset();
    fakeSession.visitor();
  });

  it('shows Entrar as the page heading with a link to Criar conta', async () => {
    renderRoutes('/entrar');
    expect(await screen.findByRole('heading', { level: 1, name: 'Entrar' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Criar conta' })).toHaveAttribute(
      'href',
      '/criar-conta',
    );
    expect(screen.getByRole('img', { name: 'Cartão Black' })).toBeInTheDocument();
  });

  it.each(['12345', '12a456', ''])('blocks the submit for the password "%s"', async (password) => {
    renderRoutes('/entrar');
    const user = await fill('ana@example.com', password);
    const button = screen.getByRole('button', { name: 'Entrar' });
    expect(button).toBeDisabled();
    await user.click(button);
    expect(fakeSession.actions.signIn).not.toHaveBeenCalled();
  });

  it('caps the password field at six characters', async () => {
    renderRoutes('/entrar');
    await fill('ana@example.com', '1234567');
    expect(screen.getByLabelText('Senha (6 dígitos)')).toHaveValue('123456');
  });

  it('signs in with six digits and goes to the app', async () => {
    fakeSession.actions.signIn.mockImplementationOnce(async () => {
      fakeSession.customer();
      return undefined;
    });
    const { router } = renderRoutes('/entrar');
    const user = await fill('ana@example.com', '123456');
    const button = screen.getByRole('button', { name: 'Entrar' });
    expect(button).toBeEnabled();
    await user.click(button);
    expect(fakeSession.actions.signIn).toHaveBeenCalledWith('ana@example.com', '123456');
    await waitFor(() => expect(pathname(router)).toBe('/inicio'));
  });

  it('wrong password shows the BFF message on the form and stays on /entrar', async () => {
    fakeSession.actions.signIn.mockRejectedValueOnce(new BffError(401, { detail: 'x' }));
    const { router } = renderRoutes('/entrar');
    const user = await fill('ana@example.com', '123456');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha incorretos.');
    expect(pathname(router)).toBe('/entrar');
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeEnabled();
  });

  it('shows the problem detail of any other failure', async () => {
    fakeSession.actions.signIn.mockRejectedValueOnce(
      new BffError(503, { detail: 'Serviço fora do ar' }),
    );
    renderRoutes('/entrar');
    const user = await fill('ana@example.com', '123456');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Serviço fora do ar');
  });

  it('keeps a customer out of Entrar', async () => {
    fakeSession.customer();
    const { router } = renderRoutes('/entrar');
    await waitFor(() => expect(pathname(router)).toBe('/inicio'));
    await act(async () => undefined);
  });
});
