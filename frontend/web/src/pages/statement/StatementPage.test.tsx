import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Entry } from '@/lib/api-types';
import { FakeBff } from '@/lib/test-support';
import { renderWithProviders } from '@/test/render';
import { StatementPage } from './StatementPage';

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

describe('StatementPage', () => {
  let bff: FakeBff;
  beforeEach(() => {
    bff = new FakeBff().install();
    // 2026-11-20 12:00 in São Paulo.
    vi.setSystemTime(new Date('2026-11-20T15:00:00Z'));
  });
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  const open = async () => {
    renderWithProviders(<StatementPage />);
    await waitFor(() => expect(bff.pending('/bff/statement')).toHaveLength(1));
  };

  it('shows "Bônus de boas-vindas" as an entry, with the down arrow', async () => {
    await open();
    bff.only('/bff/statement').respond(200, {
      entries: [entry('1', 'CREDIT', 10000, 'Bônus de boas-vindas')],
      nextCursor: null,
    });
    const row = await screen.findByRole('listitem');
    expect(row).toHaveTextContent('Bônus de boas-vindas');
    expect(within(row).getByRole('img', { name: 'Entrada' })).toHaveTextContent('↓');
    expect(within(row).getByTestId('amount')).toHaveAttribute('data-kind', 'CREDIT');
  });

  it('groups entries by São Paulo day ("Hoje", "Ontem", data)', async () => {
    await open();
    bff.only('/bff/statement').respond(200, {
      entries: [
        { ...entry('1', 'CREDIT', 100, 'Ana Souza'), createdAt: '2026-11-20T14:00:00Z' },
        { ...entry('2', 'DEBIT', 200, 'Bruno Lima'), createdAt: '2026-11-19T20:00:00Z' },
        { ...entry('3', 'DEBIT', 300, 'Carla Mendes'), createdAt: '2026-11-10T12:00:00Z' },
      ],
      nextCursor: null,
    });
    expect(await screen.findByRole('heading', { name: 'Hoje' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Ontem' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '10 de novembro de 2026' })).toBeInTheDocument();
  });

  it('"Carregar mais" sends the cursor and appends, hiding the button on the last page', async () => {
    const user = userEvent.setup();
    await open();
    expect(bff.only('/bff/statement').path).toBe('/bff/statement');
    bff.only('/bff/statement').respond(200, {
      entries: [entry('2', 'DEBIT', 500, 'Transferência enviada')],
      nextCursor: 'abc',
    });
    await screen.findByText('Transferência enviada');
    await user.click(screen.getByRole('button', { name: 'Carregar mais' }));
    await waitFor(() => expect(bff.pending('/bff/statement?cursor=abc')).toHaveLength(1));
    bff.only('/bff/statement?cursor=abc').respond(200, {
      entries: [entry('1', 'CREDIT', 10000, 'Bônus de boas-vindas')],
      nextCursor: null,
    });
    await screen.findByText('Bônus de boas-vindas');
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    expect(screen.queryByRole('button', { name: 'Carregar mais' })).not.toBeInTheDocument();
  });

  it('says so when there are no entries', async () => {
    await open();
    bff.only('/bff/statement').respond(200, { entries: [], nextCursor: null });
    expect(await screen.findByText('Nenhum lançamento ainda.')).toBeInTheDocument();
  });

  it('shows the retry notice when the load fails and tries again', async () => {
    const user = userEvent.setup();
    await open();
    bff.only('/bff/statement').respond(503, {});
    expect(await screen.findByText(/Não foi possível carregar/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }));
    await waitFor(() => expect(bff.pending('/bff/statement')).toHaveLength(1));
    bff.only('/bff/statement').respond(200, {
      entries: [entry('1', 'CREDIT', 100, 'x')],
      nextCursor: null,
    });
    expect(await screen.findByRole('listitem')).toBeInTheDocument();
    expect(screen.queryByText(/Não foi possível carregar/)).not.toBeInTheDocument();
  });
});
