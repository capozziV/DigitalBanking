import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { Entry } from '@/lib/api-types';
import { EntryList } from './EntryList';

const credit: Entry = {
  id: '1',
  kind: 'CREDIT',
  amount: 10000,
  description: 'Bônus de boas-vindas',
  balanceAfter: 10000,
  createdAt: '2026-11-19T12:00:00Z',
};
const debit: Entry = {
  id: '2',
  kind: 'DEBIT',
  amount: 2500,
  description: 'Transferência enviada',
  balanceAfter: 7500,
  createdAt: '2026-11-19T13:00:00Z',
};

describe('EntryList', () => {
  it('shows the welcome bonus as an entry (down arrow, bold amount)', () => {
    render(<EntryList entries={[credit, debit]} />);
    const row = screen.getAllByRole('listitem')[0];
    expect(row).toHaveTextContent('Bônus de boas-vindas');
    expect(within(row).getByRole('img', { name: 'Entrada' })).toHaveTextContent('↓');
    const amount = within(row).getByTestId('amount');
    expect(amount).toHaveAttribute('data-kind', 'CREDIT');
    expect(amount).toHaveTextContent(/^\+\sR\$\s100,00$/);
  });

  it('shows a debit as an exit (up arrow, muted amount)', () => {
    render(<EntryList entries={[credit, debit]} />);
    const row = screen.getAllByRole('listitem')[1];
    expect(within(row).getByRole('img', { name: 'Saída' })).toHaveTextContent('↑');
    const amount = within(row).getByTestId('amount');
    expect(amount).toHaveAttribute('data-kind', 'DEBIT');
    expect(amount).toHaveTextContent(/^−\sR\$\s25,00$/);
  });

  it('falls back to the entry kind when the description is null', () => {
    render(<EntryList entries={[{ ...debit, description: null }]} />);
    expect(screen.getByRole('listitem')).toHaveTextContent('Débito');
  });

  it('shows the time in São Paulo and, in the statement, the balance after the entry', () => {
    render(<EntryList entries={[credit]} detail="statement" />);
    // 12:00 UTC is 09:00 in São Paulo.
    expect(screen.getByRole('listitem')).toHaveTextContent(/09:00 · saldo R\$\s100,00/);
  });

  it('shows the date and time on the home list', () => {
    render(<EntryList entries={[credit]} detail="home" />);
    expect(screen.getByRole('listitem')).toHaveTextContent('19/11/2026 às 09:00');
  });
});
