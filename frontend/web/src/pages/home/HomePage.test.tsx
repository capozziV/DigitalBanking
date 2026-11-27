import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AccountSummary, Entry } from '@/lib/api-types';
import { FakeBff } from '@/lib/test-support';
import { fakeSession } from '@/test/fake-session';
import { renderWithProviders } from '@/test/render';
import { HomePage } from './HomePage';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());

const account: AccountSummary = {
  type: 'GOLD',
  status: 'ACTIVE',
  balance: 482015,
  overdraftLimit: 200000,
  availableBalance: 682015,
};

const entry = (
  id: string,
  kind: 'CREDIT' | 'DEBIT',
  amount: number,
  description: string,
): Entry => ({
  id,
  kind,
  amount,
  description,
  balanceAfter: 1000,
  createdAt: '2026-11-19T12:00:00Z',
});

describe('HomePage', () => {
  let bff: FakeBff;
  beforeEach(() => {
    // Reduced motion: the balance shows at once instead of counting up.
    vi.stubGlobal('matchMedia', () => ({
      matches: true,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    }));
    bff = new FakeBff().install();
    fakeSession.reset();
    fakeSession.customer({
      customer: { name: 'Vitor Capozzi', email: 'v@example.com', phone: '11987654321' },
      account,
    });
  });
  afterEach(() => vi.unstubAllGlobals());

  const open = async () => {
    renderWithProviders(<HomePage />);
    await waitFor(() => expect(bff.pending('/bff/home')).toHaveLength(1));
  };

  it('shows the balance, the card of the account type, the daily limit and the latest entries', async () => {
    await open();
    bff.only('/bff/home').respond(200, {
      account,
      dailyLimit: { limit: 500000, used: 120000, available: 380000 },
      latestEntries: [
        entry('1', 'CREDIT', 100000, 'Transferência recebida'),
        entry('2', 'DEBIT', 30000, 'Transferência enviada'),
      ],
    });
    expect(await screen.findByText('Olá, Vitor')).toBeInTheDocument();
    expect(screen.getByTestId('balance')).toHaveTextContent(/^R\$\s6\.820,15$/);
    expect(screen.getByText(/Conta Gold/)).toBeInTheDocument();
    expect(screen.getByText(/cheque especial R\$\s2\.000,00/)).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Cartão Gold' })).toBeInTheDocument();
    expect(screen.getByText(/R\$\s1\.200,00 de R\$\s5\.000,00/)).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    expect(screen.getByRole('link', { name: 'Transferir' })).toHaveAttribute('href', '/transferir');
    expect(screen.getByRole('link', { name: 'Extrato' })).toHaveAttribute('href', '/extrato');
  });

  it('fills the limit bar in proportion to what was used', async () => {
    await open();
    bff.only('/bff/home').respond(200, {
      account,
      dailyLimit: { limit: 500000, used: 125000, available: 375000 },
      latestEntries: [],
    });
    const bar = await screen.findByRole('progressbar', { name: 'Limite diário usado' });
    expect(bar).toHaveAttribute('aria-valuenow', '25');
    expect(screen.getByTestId('limit-fill')).toHaveStyle({ width: '25%' });
  });

  it('home shows "Abrir conta" when account is null, without calling /bff/home, and opens it', async () => {
    const user = userEvent.setup();
    fakeSession.customer({ account: null });
    renderWithProviders(<HomePage />);
    const openButton = await screen.findByRole('button', { name: 'Abrir conta' });
    expect(screen.queryByTestId('balance')).not.toBeInTheDocument();
    expect(bff.pending('/bff/home')).toHaveLength(0);
    await user.click(openButton);
    await waitFor(() => expect(bff.pending('/bff/accounts')).toHaveLength(1));
    const call = bff.only('/bff/accounts');
    expect(call.method).toBe('POST');
    expect(call.body).toEqual({});
    call.respond(200, {});
    await waitFor(() => expect(fakeSession.actions.loadMe).toHaveBeenCalled());
    await waitFor(() => expect(bff.pending('/bff/home')).toHaveLength(1));
    bff.only('/bff/home').respond(200, {
      account: { ...account, balance: 10000, availableBalance: 10000, overdraftLimit: 0 },
      dailyLimit: { limit: 500000, used: 0, available: 500000 },
      latestEntries: [entry('1', 'CREDIT', 10000, 'Bônus de boas-vindas')],
    });
    expect(await screen.findByText('Bônus de boas-vindas')).toBeInTheDocument();
    expect(screen.getByTestId('balance')).toHaveTextContent(/^R\$\s100,00$/);
  });

  it('falls back to "Abrir conta" when /bff/home answers 404', async () => {
    await open();
    bff.only('/bff/home').respond(404, {});
    expect(await screen.findByText('Você ainda não tem conta')).toBeInTheDocument();
    expect(screen.queryByText(/Não foi possível carregar/)).not.toBeInTheDocument();
    expect(
      screen.getByText(
        'Abra uma conta para transferir. O bônus de boas-vindas vale só para a primeira conta.',
      ),
    ).toBeInTheDocument();
  });

  it('shows the message of a failed account opening', async () => {
    const user = userEvent.setup();
    fakeSession.customer({ account: null });
    renderWithProviders(<HomePage />);
    await user.click(await screen.findByRole('button', { name: 'Abrir conta' }));
    await waitFor(() => expect(bff.pending('/bff/accounts')).toHaveLength(1));
    bff.only('/bff/accounts').respond(422, { detail: 'Cliente bloqueado' });
    expect(await screen.findByRole('alert')).toHaveTextContent('Cliente bloqueado');
    expect(screen.getByRole('button', { name: 'Abrir conta' })).toBeEnabled();
  });

  it('shows a discreet notice with a retry when the load fails', async () => {
    const user = userEvent.setup();
    await open();
    bff.only('/bff/home').respond(503, {});
    expect(
      await screen.findByText(/Não foi possível carregar. Tente de novo./),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }));
    await waitFor(() => expect(bff.pending('/bff/home')).toHaveLength(1));
    bff.only('/bff/home').respond(200, { account: null, dailyLimit: null, latestEntries: [] });
    await waitFor(() =>
      expect(screen.queryByText(/Não foi possível carregar/)).not.toBeInTheDocument(),
    );
  });
});
