import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from '@/app';

describe('App', () => {
  it('opens on Entrar for a visitor, with the capzzPay title', async () => {
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
    await waitFor(() => expect(document.title).toBe('Entrar · capzzPay'));
  });
});
