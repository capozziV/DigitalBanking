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
    const empty = await screen.findByText('Nenhum lançamento ainda.');
    expect(empty).toHaveClass('text-muted-foreground');
    expect(empty).not.toHaveClass('text-muted');
  });

  it('keeps one group for a day that spans two pages, and the heading is readable', async () => {
    const user = userEvent.setup();
    await open();
    bff.only('/bff/statement').respond(200, {
      entries: [{ ...entry('1', 'CREDIT', 100, 'Ana Souza'), createdAt: '2026-11-20T14:00:00Z' }],
      nextCursor: 'p2',
    });
    await screen.findByText('Ana Souza');
    await user.click(screen.getByRole('button', { name: 'Carregar mais' }));
    await waitFor(() => expect(bff.pending('/bff/statement?cursor=p2')).toHaveLength(1));
    bff.only('/bff/statement?cursor=p2').respond(200, {
      entries: [
        { ...entry('2', 'DEBIT', 200, 'Bruno Lima'), createdAt: '2026-11-20T11:00:00Z' },
        { ...entry('3', 'DEBIT', 300, 'Carla Mendes'), createdAt: '2026-11-19T20:00:00Z' },
      ],
      nextCursor: null,
    });
    await screen.findByText('Bruno Lima');
    const headings = screen.getAllByRole('heading', { level: 2 });
    expect(headings.map((h) => h.textContent)).toEqual(['Hoje', 'Ontem']);
    for (const heading of headings) expect(heading).toHaveClass('text-muted-foreground');
    const today = headings[0].closest('section') as HTMLElement;
    expect(within(today).getAllByRole('listitem')).toHaveLength(2);
  });

  it('asks for the next page with the exact, URL-encoded cursor and no other request', async () => {
    const user = userEvent.setup();
    await open();
    bff.only('/bff/statement').respond(200, {
      entries: [entry('1', 'CREDIT', 100, 'Ana Souza')],
      nextCursor: 'a b/c=',
    });
    await screen.findByText('Ana Souza');
    await user.click(screen.getByRole('button', { name: 'Carregar mais' }));
    await waitFor(() => expect(bff.pending('/bff/statement?cursor=a%20b%2Fc%3D')).toHaveLength(1));
    expect(bff.pending('/bff/statement')).toHaveLength(0);
  });

  it('shows the retry notice when the load fails and tries again', async () => {
    const user = userEvent.setup();
    await open();
    bff.only('/bff/statement').respond(503, {});
    const notice = await screen.findByText(/Não foi possível carregar/);
    expect(notice.closest('p')).toHaveClass('text-muted-foreground');
    expect(notice.closest('p')).not.toHaveClass('text-muted');
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
