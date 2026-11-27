import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { AmountStep } from './AmountStep';

function Harness({
  available = 475_000,
  limit = 500_000,
  onContinue = () => undefined,
}: {
  available?: number | null;
  limit?: number | null;
  onContinue?: () => void;
}) {
  const [cents, setCents] = useState(0);
  return (
    <AmountStep
      recipientName="Ana Souza"
      phone="+5511999990002"
      cents={cents}
      onChange={setCents}
      available={available}
      limit={limit}
      limitFailed={false}
      onContinue={onContinue}
    />
  );
}

const press = async (...keys: string[]) => {
  const user = userEvent.setup({ delay: null });
  for (const key of keys) await user.click(screen.getByRole('button', { name: key }));
  return user;
};

describe('AmountStep', () => {
  it('keys 1,0,0,5 show "R$ 10,05"', async () => {
    render(<Harness />);
    expect(screen.getByRole('status', { name: 'Valor' })).toHaveTextContent('R$ 0,00');
    await press('1', '0', '0', '5');
    expect(screen.getByRole('status', { name: 'Valor' })).toHaveTextContent('R$ 10,05');
  });

  it('the erase key drops the last digit', async () => {
    render(<Harness />);
    await press('1', '2', '3', 'Apagar');
    expect(screen.getByRole('status', { name: 'Valor' })).toHaveTextContent('R$ 0,12');
  });

  it('the physical keyboard types the same digits', async () => {
    render(<Harness />);
    const user = userEvent.setup({ delay: null });
    await user.keyboard('2500');
    expect(screen.getByRole('status', { name: 'Valor' })).toHaveTextContent('R$ 25,00');
    await user.keyboard('{Backspace}');
    expect(screen.getByRole('status', { name: 'Valor' })).toHaveTextContent('R$ 2,50');
  });

  it('keeps "Continuar" disabled while the amount is zero', () => {
    render(<Harness />);
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled();
  });

  it('shows the daily limit left', () => {
    render(<Harness />);
    expect(screen.getByText(/Disponível hoje:/)).toHaveTextContent(
      'Disponível hoje: R$ 4.750,00 de R$ 5.000,00',
    );
    expect(screen.getByRole('progressbar', { name: 'Limite diário disponível' })).toHaveAttribute(
      'aria-valuenow',
      '475000',
    );
  });

  it('above the daily available disables "Continuar" and shows the limit', async () => {
    const onContinue = vi.fn();
    render(<Harness available={10_000} limit={500_000} onContinue={onContinue} />);
    const user = await press('1', '0', '0', '0', '0');
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeEnabled();
    await user.click(screen.getByRole('button', { name: '1' }));
    const next = screen.getByRole('button', { name: 'Continuar' });
    expect(next).toBeDisabled();
    expect(screen.getByText(/acima do que resta no limite de hoje/i)).toHaveTextContent(
      'R$ 100,00',
    );
    await user.click(next);
    expect(onContinue).not.toHaveBeenCalled();
  });

  it('does not block when the limit could not be read', async () => {
    render(<Harness available={null} limit={null} />);
    await press('5');
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeEnabled();
  });

  it.each([null, 475_000])('stops the keypad at the cap with available=%s', async (available) => {
    render(<Harness available={available} limit={available} />);
    await press(...Array<string>(20).fill('9'));
    const shown = screen.getByRole('status', { name: 'Valor' }).textContent ?? '';
    const digits = Number(shown.replace(/\D/g, ''));
    expect(digits).toBeLessThanOrEqual(Number.MAX_SAFE_INTEGER);
    expect(digits).toBe(99_999_999_999);
  });

  it.each([null, 475_000])(
    'stops the physical keyboard at the cap with available=%s',
    async (available) => {
      render(<Harness available={available} limit={available} />);
      const user = userEvent.setup({ delay: null });
      await user.keyboard('9'.repeat(20));
      const digits = Number(
        (screen.getByRole('status', { name: 'Valor' }).textContent ?? '').replace(/\D/g, ''),
      );
      expect(digits).toBeLessThanOrEqual(Number.MAX_SAFE_INTEGER);
      expect(digits).toBeGreaterThan(0);
      await user.keyboard('1');
      expect(
        Number(
          (screen.getByRole('status', { name: 'Valor' }).textContent ?? '').replace(/\D/g, ''),
        ),
      ).toBe(digits);
    },
  );

  it('ignores digits typed inside a dialog or a textarea on top', async () => {
    render(
      <>
        <Harness />
        <div role="dialog" aria-label="Aviso">
          <button type="button">dentro</button>
        </div>
        <textarea aria-label="Nota" />
      </>,
    );
    const user = userEvent.setup({ delay: null });
    await user.click(screen.getByRole('button', { name: 'dentro' }));
    await user.keyboard('123');
    await user.click(screen.getByLabelText('Nota'));
    await user.keyboard('456');
    expect(screen.getByRole('status', { name: 'Valor' })).toHaveTextContent('R$ 0,00');
  });
});
