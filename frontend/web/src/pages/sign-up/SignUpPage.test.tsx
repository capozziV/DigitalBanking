import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BffError } from '@/lib/bff-client';
import { fakeSession } from '@/test/fake-session';
import { renderRoutes } from '@/test/render-routes';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());

const pathname = (router: ReturnType<typeof renderRoutes>['router']) =>
  router.state.location.pathname;

async function fill(password: string, confirmation: string) {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText('E-mail'), 'ana@example.com');
  if (password) await user.type(screen.getByLabelText('Senha (6 dígitos)'), password);
  if (confirmation) await user.type(screen.getByLabelText('Confirme a senha'), confirmation);
  return user;
}

const continueButton = () => screen.getByRole('button', { name: 'Continuar' });

describe('SignUpPage', () => {
  beforeEach(() => {
    fakeSession.reset();
    fakeSession.visitor();
  });

  it('shows Criar conta as the heading with a link to Entrar', async () => {
    renderRoutes('/criar-conta');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Criar conta' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/entrar');
  });

  it.each([
    ['12345', '12345'],
    ['12a456', '12a456'],
  ])('blocks the submit for the password "%s"', async (password, confirmation) => {
    renderRoutes('/criar-conta');
    const user = await fill(password, confirmation);
    expect(continueButton()).toBeDisabled();
    await user.click(continueButton());
    expect(fakeSession.actions.signUp).not.toHaveBeenCalled();
  });

  it('blocks the submit when the confirmation differs', async () => {
    renderRoutes('/criar-conta');
    const user = await fill('123456', '654321');
    expect(continueButton()).toBeDisabled();
    expect(screen.getByText('As senhas não conferem.')).toBeInTheDocument();
    await user.click(continueButton());
    expect(fakeSession.actions.signUp).not.toHaveBeenCalled();
  });

  it('asks for the e-mail code only when the BFF requires it', async () => {
    fakeSession.actions.signUp.mockResolvedValueOnce({ confirmationRequired: true });
    fakeSession.actions.signIn.mockImplementationOnce(async () => {
      fakeSession.signedInWithoutCustomer();
      return undefined;
    });
    const { router } = renderRoutes('/criar-conta');
    const user = await fill('123456', '123456');
    await user.click(continueButton());
    expect(fakeSession.actions.signUp).toHaveBeenCalledWith('ana@example.com', '123456');
    expect(fakeSession.actions.signIn).not.toHaveBeenCalled();

    await user.type(await screen.findByLabelText('Código do e-mail'), '987654');
    await user.click(screen.getByRole('button', { name: 'Confirmar e entrar' }));
    expect(fakeSession.actions.confirm).toHaveBeenCalledWith('ana@example.com', '987654');
    expect(fakeSession.actions.signIn).toHaveBeenCalledWith('ana@example.com', '123456');
    await waitFor(() => expect(pathname(router)).toBe('/completar-cadastro'));
  });

  it('skips the code and signs in when no confirmation is required', async () => {
    fakeSession.actions.signIn.mockImplementationOnce(async () => {
      fakeSession.signedInWithoutCustomer();
      return undefined;
    });
    const { router } = renderRoutes('/criar-conta');
    const user = await fill('123456', '123456');
    await user.click(continueButton());
    expect(fakeSession.actions.signIn).toHaveBeenCalledWith('ana@example.com', '123456');
    expect(fakeSession.actions.confirm).not.toHaveBeenCalled();
    expect(screen.queryByLabelText('Código do e-mail')).not.toBeInTheDocument();
    await waitFor(() => expect(pathname(router)).toBe('/completar-cadastro'));
  });

  it('shows the BFF message when the e-mail is already taken', async () => {
    fakeSession.actions.signUp.mockRejectedValueOnce(
      new BffError(409, { message: 'Já existe uma conta com este e-mail' } as never),
    );
    const { router } = renderRoutes('/criar-conta');
    const user = await fill('123456', '123456');
    await user.click(continueButton());
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Já existe uma conta com este e-mail',
    );
    expect(pathname(router)).toBe('/criar-conta');
  });

  it('shows the problem detail when the e-mail is already taken', async () => {
    fakeSession.actions.signUp.mockRejectedValueOnce(
      new BffError(409, { detail: 'Este e-mail já está cadastrado' }),
    );
    renderRoutes('/criar-conta');
    const user = await fill('123456', '123456');
    await user.click(continueButton());
    expect(await screen.findByRole('alert')).toHaveTextContent('Este e-mail já está cadastrado');
  });

  it('accepts only a 6 digit e-mail code', async () => {
    fakeSession.actions.signUp.mockResolvedValueOnce({ confirmationRequired: true });
    renderRoutes('/criar-conta');
    const user = await fill('123456', '123456');
    await user.click(continueButton());
    const code = await screen.findByLabelText('Código do e-mail');
    const confirmButton = screen.getByRole('button', { name: 'Confirmar e entrar' });
    for (const bad of ['1234', '12345', '1234567', '12345678']) {
      await user.clear(code);
      await user.type(code, bad);
      expect(confirmButton).toBeDisabled();
    }
    await user.clear(code);
    await user.type(code, '123456');
    expect(confirmButton).toBeEnabled();
  });

  it('shows the BFF message when the code is wrong and keeps the code step', async () => {
    fakeSession.actions.signUp.mockResolvedValueOnce({ confirmationRequired: true });
    fakeSession.actions.confirm.mockRejectedValueOnce(
      new BffError(400, { detail: 'Código inválido' }),
    );
    renderRoutes('/criar-conta');
    const user = await fill('123456', '123456');
    await user.click(continueButton());
    await user.type(await screen.findByLabelText('Código do e-mail'), '111111');
    await user.click(screen.getByRole('button', { name: 'Confirmar e entrar' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Código inválido');
    expect(screen.getByLabelText('Código do e-mail')).toBeInTheDocument();
  });
});
