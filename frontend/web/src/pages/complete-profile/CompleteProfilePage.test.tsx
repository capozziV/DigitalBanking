import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BffError } from '@/lib/bff-client';
import { fakeSession } from '@/test/fake-session';
import { renderRoutes } from '@/test/render-routes';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());
const bff = vi.fn();
vi.mock('@/lib/bff-client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/lib/bff-client')>()),
  bff: (...args: unknown[]) => bff(...args),
}));

const pathname = (router: ReturnType<typeof renderRoutes>['router']) =>
  router.state.location.pathname;

async function fill(name: string, phone: string) {
  const user = userEvent.setup();
  const nameInput = await screen.findByLabelText('Nome completo');
  if (name) await user.type(nameInput, name);
  if (phone) await user.type(screen.getByLabelText('Celular'), phone);
  return user;
}

const concludeButton = () => screen.getByRole('button', { name: 'Concluir' });

describe('CompleteProfilePage', () => {
  beforeEach(() => {
    fakeSession.reset();
    fakeSession.signedInWithoutCustomer();
    bff.mockReset();
    bff.mockResolvedValue({});
  });

  it('shows Completar cadastro as the heading', async () => {
    renderRoutes('/completar-cadastro');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Completar cadastro' }),
    ).toBeInTheDocument();
  });

  it('phone input masks while typing', async () => {
    renderRoutes('/completar-cadastro');
    await fill('', '11987654321');
    expect(screen.getByLabelText('Celular')).toHaveValue('(11) 98765-4321');
  });

  it('blocks the submit without a valid mobile phone', async () => {
    renderRoutes('/completar-cadastro');
    const user = await fill('Ana Souza', '1187654321');
    expect(concludeButton()).toBeDisabled();
    await user.click(concludeButton());
    expect(bff).not.toHaveBeenCalled();
  });

  it('blocks the submit without a name', async () => {
    renderRoutes('/completar-cadastro');
    await fill('', '11987654321');
    expect(concludeButton()).toBeDisabled();
  });

  it('registers the customer and goes to the app', async () => {
    fakeSession.actions.reload.mockImplementationOnce(async () => {
      fakeSession.customer();
      return undefined;
    });
    const { router } = renderRoutes('/completar-cadastro');
    const user = await fill(' Ana Souza ', '11987654321');
    await user.click(concludeButton());
    expect(bff).toHaveBeenCalledWith('/bff/customers', {
      method: 'POST',
      body: { name: 'Ana Souza', phone: '11987654321' },
    });
    expect(fakeSession.actions.reload).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(pathname(router)).toBe('/inicio'));
  });

  it('leaves the registration when it worked and only the profile load failed', async () => {
    fakeSession.actions.reload.mockImplementationOnce(async () => {
      fakeSession.set({ loadFailed: true });
      return undefined;
    });
    const { router } = renderRoutes('/completar-cadastro');
    const user = await fill('Ana Souza', '11987654321');
    await user.click(concludeButton());
    await waitFor(() => expect(pathname(router)).toBe('/indisponivel'));
    expect(bff).toHaveBeenCalledTimes(1);
  });

  it('shows the BFF message when the customer already exists', async () => {
    bff.mockRejectedValueOnce(
      new BffError(409, { message: 'Já existe uma conta com este e-mail' } as never),
    );
    const { router } = renderRoutes('/completar-cadastro');
    const user = await fill('Ana Souza', '11987654321');
    await user.click(concludeButton());
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Já existe uma conta com este e-mail',
    );
    expect(pathname(router)).toBe('/completar-cadastro');
    expect(fakeSession.actions.reload).not.toHaveBeenCalled();
  });

  it('shows the problem detail when the phone is already used', async () => {
    bff.mockRejectedValueOnce(new BffError(409, { detail: 'Telefone já cadastrado' }));
    renderRoutes('/completar-cadastro');
    const user = await fill('Ana Souza', '11987654321');
    await user.click(concludeButton());
    expect(await screen.findByRole('alert')).toHaveTextContent('Telefone já cadastrado');
  });
});
