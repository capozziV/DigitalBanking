import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '@/app';
import { fakeSession } from '@/test/fake-session';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());

describe('App', () => {
  beforeEach(() => fakeSession.reset());

  it('opens on Entrar for a visitor, with the capzzPay title', async () => {
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
    await waitFor(() => expect(document.title).toBe('Entrar · capzzPay'));
  });
});
