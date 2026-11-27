import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { FakeBff } from '@/lib/test-support';
import { fakeSession } from '@/test/fake-session';
import { renderWithProviders } from '@/test/render';
import { AccountPage } from './AccountPage';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());

const goldAccount = {
  type: 'GOLD',
  status: 'ACTIVE',
  balance: 0,
  overdraftLimit: 200000,
  availableBalance: 200000,
};

describe('AccountPage', () => {
  let bff: FakeBff;
  beforeEach(() => {
    vi.stubGlobal('matchMedia', () => ({
      matches: true,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    }));
    bff = new FakeBff().install();
    fakeSession.reset();
    fakeSession.customer({
      customer: { name: 'Ana Souza', email: 'ana@example.com', phone: '+5511987654321' },
      account: goldAccount,
    });
  });
  afterEach(() => vi.unstubAllGlobals());

  const render = () => renderWithProviders(<AccountPage />);

  it('signs out from Minha conta', async () => {
    const user = userEvent.setup();
    render();
    await user.click(screen.getByRole('button', { name: 'Sair' }));
    expect(fakeSession.actions.signOut).toHaveBeenCalledTimes(1);
  });

  it('shows the customer, the account data and the card of the account type', () => {
    render();
    expect(screen.getByText('Ana Souza')).toBeInTheDocument();
    expect(screen.getByText('ana@example.com')).toBeInTheDocument();
    expect(screen.getByText('(11) 98765-4321')).toBeInTheDocument();
    expect(screen.getByText('Tipo').closest('div')).toHaveTextContent('Gold');
    expect(screen.getByText(/^R\$\s2\.000,00$/)).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Cartão Gold' })).toBeInTheDocument();
  });

  it('shows the daily limit from /bff/home, in one request', async () => {
    render();
    await waitFor(() => expect(bff.pending('/bff/home')).toHaveLength(1));
    const row = screen.getByText('Limite diário').closest('div') as HTMLElement;
    expect(row).toHaveTextContent('—');
    bff.only('/bff/home').respond(200, {
      account: goldAccount,
      dailyLimit: { limit: 500000, used: 120000, available: 380000 },
      latestEntries: [],
    });
    await waitFor(() => expect(row).toHaveTextContent(/R\$\s5\.000,00/));
    expect(bff.calls.filter((c) => c.path === '/bff/home')).toHaveLength(1);
  });

  it('keeps the page working when the daily limit cannot be loaded', async () => {
    render();
    await waitFor(() => expect(bff.pending('/bff/home')).toHaveLength(1));
    bff.only('/bff/home').respond(503, {});
    await waitFor(() => expect(bff.pending('/bff/home')).toHaveLength(0));
    const row = screen.getByText('Limite diário').closest('div') as HTMLElement;
    expect(row).toHaveTextContent('—');
    expect(screen.getByText('Ana Souza')).toBeInTheDocument();
  });

  it('does not ask for the daily limit without an account', () => {
    fakeSession.customer({ account: null });
    render();
    expect(bff.calls.filter((c) => c.path === '/bff/home')).toHaveLength(0);
    expect(screen.queryByText('Limite diário')).not.toBeInTheDocument();
  });

  it('masks the phone while typing, sends only digits and reloads the profile', async () => {
    const user = userEvent.setup();
    render();
    const field = screen.getByLabelText('Novo celular');
    await user.type(field, '21998765432');
    expect(field).toHaveValue('(21) 99876-5432');
    await user.click(screen.getByRole('button', { name: 'Trocar celular' }));
    await waitFor(() => expect(bff.pending('/bff/me/phone')).toHaveLength(1));
    const call = bff.only('/bff/me/phone');
    expect(call.method).toBe('PUT');
    expect(call.body).toEqual({ phone: '21998765432' });
    call.respond(200, {});
    await waitFor(() => expect(fakeSession.actions.loadMe).toHaveBeenCalled());
    expect(await screen.findByRole('status')).toHaveTextContent('Telefone atualizado.');
    expect(field).toHaveValue('');
  });

  it('blocks a phone that is not a mobile number', async () => {
    const user = userEvent.setup();
    render();
    await user.type(screen.getByLabelText('Novo celular'), '1187654321');
    expect(screen.getByRole('button', { name: 'Trocar celular' })).toBeDisabled();
    expect(
      screen.getByText('Informe um celular com DDD, como (11) 98765-4321.'),
    ).toBeInTheDocument();
    expect(bff.calls.filter((c) => c.path !== '/bff/home')).toHaveLength(0);
  });

  it('shows the message when the phone is already used', async () => {
    const user = userEvent.setup();
    render();
    await user.type(screen.getByLabelText('Novo celular'), '21998765432');
    await user.click(screen.getByRole('button', { name: 'Trocar celular' }));
    await waitFor(() => expect(bff.pending('/bff/me/phone')).toHaveLength(1));
    bff.only('/bff/me/phone').respond(409, { detail: 'Telefone já cadastrado' });
    expect(await screen.findByRole('alert')).toHaveTextContent('Telefone já cadastrado');
  });

  it('close account asks for confirmation in a dialog, and cancel closes it without calling', async () => {
    const user = userEvent.setup();
    render();
    await user.click(screen.getByRole('button', { name: 'Encerrar conta' }));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('Tem certeza?');
    expect(bff.calls.filter((c) => c.path !== '/bff/home')).toHaveLength(0);
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(bff.calls.filter((c) => c.path !== '/bff/home')).toHaveLength(0);
    expect(screen.getByRole('button', { name: 'Encerrar conta' })).toBeInTheDocument();
  });

  it('closes the account after the confirmation and reloads the profile', async () => {
    const user = userEvent.setup();
    render();
    await user.click(screen.getByRole('button', { name: 'Encerrar conta' }));
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Sim, encerrar' }));
    await waitFor(() => expect(bff.pending('/bff/accounts/close')).toHaveLength(1));
    const call = bff.only('/bff/accounts/close');
    expect(call.method).toBe('POST');
    expect(call.body).toEqual({});
    call.respond(200, {});
    await waitFor(() => expect(fakeSession.actions.loadMe).toHaveBeenCalled());
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('shows why the account could not be closed', async () => {
    const user = userEvent.setup();
    render();
    await user.click(screen.getByRole('button', { name: 'Encerrar conta' }));
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Sim, encerrar' }),
    );
    await waitFor(() => expect(bff.pending('/bff/accounts/close')).toHaveLength(1));
    bff
      .only('/bff/accounts/close')
      .respond(409, { detail: 'A conta precisa estar com saldo zero' });
    expect(await screen.findByRole('alert')).toHaveTextContent('saldo zero');
  });

  it('has no close button when there is no account', () => {
    fakeSession.customer({ account: null });
    render();
    expect(
      screen.getByText('Você não tem conta aberta. Abra uma na tela Início.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Encerrar conta' })).not.toBeInTheDocument();
  });
});
