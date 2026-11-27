import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from '@/app';
import { renderWithProviders } from '@/test/render';

describe('App', () => {
  it('renders the capzzPay name', () => {
    renderWithProviders(<App />);
    expect(screen.getByText('capzzPay')).toBeInTheDocument();
  });
});
