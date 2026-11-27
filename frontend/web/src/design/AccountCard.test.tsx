import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AccountCard } from '@/design/AccountCard';
import type { AccountType } from '@/lib/api-types';

describe('AccountCard', () => {
  it.each<[AccountType, string]>([
    ['FREE', 'Cartão Free'],
    ['STANDARD', 'Cartão Standard'],
    ['GOLD', 'Cartão Gold'],
    ['BLACK', 'Cartão Black'],
    ['DIAMOND', 'Cartão Diamond'],
  ])('names the %s card "%s" and gives it its finish', (type, name) => {
    render(<AccountCard type={type} />);
    const card = screen.getByRole('img', { name });
    expect(card).toHaveAttribute('data-finish', type.toLowerCase());
  });

  it('shows the last four digits when given, and the size', () => {
    render(<AccountCard type="BLACK" last4="4821" size="lg" />);
    const card = screen.getByRole('img', { name: 'Cartão Black' });
    expect(card).toHaveTextContent('•••• 4821');
    expect(card).toHaveAttribute('data-size', 'lg');
  });

  it('defaults to the medium size without digits', () => {
    render(<AccountCard type="DIAMOND" />);
    const card = screen.getByRole('img', { name: 'Cartão Diamond' });
    expect(card).toHaveAttribute('data-size', 'md');
    expect(card).not.toHaveTextContent('••••');
  });
});
