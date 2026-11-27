import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { EntryIcon } from './EntryIcon';

describe('EntryIcon', () => {
  it('shows a filled circle with a down arrow for a credit', () => {
    render(<EntryIcon kind="CREDIT" />);
    const icon = screen.getByRole('img', { name: 'Entrada' });
    expect(icon).toHaveTextContent('↓');
    expect(icon).toHaveAttribute('data-style', 'filled');
  });

  it('shows a hollow circle with an up arrow for a debit', () => {
    render(<EntryIcon kind="DEBIT" />);
    const icon = screen.getByRole('img', { name: 'Saída' });
    expect(icon).toHaveTextContent('↑');
    expect(icon).toHaveAttribute('data-style', 'hollow');
  });
});
