import { act, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AccountCard } from '@/design/AccountCard';
import { AmbientLights } from '@/design/AmbientLights';
import { CountUp, usePrefersReducedMotion } from '@/design/motion';

function emulateReducedMotion(reduce: boolean) {
  vi.stubGlobal(
    'matchMedia',
    vi.fn((query: string) => ({
      matches: reduce && query === '(prefers-reduced-motion: reduce)',
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })),
  );
}

function Probe() {
  return <p>{usePrefersReducedMotion() ? 'reduced' : 'full'}</p>;
}

describe('motion', () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('reads the reduced-motion preference, defaulting to full motion without matchMedia', () => {
    emulateReducedMotion(true);
    const { unmount } = render(<Probe />);
    expect(screen.getByText('reduced')).toBeInTheDocument();
    unmount();
    vi.unstubAllGlobals();
    vi.stubGlobal('matchMedia', undefined);
    render(<Probe />);
    expect(screen.getByText('full')).toBeInTheDocument();
  });

  it('keeps the card and the lights still when motion is reduced', () => {
    emulateReducedMotion(true);
    render(
      <>
        <AmbientLights />
        <AccountCard type="BLACK" />
      </>,
    );
    expect(screen.getByRole('img', { name: 'Cartão Black' })).toHaveAttribute('data-still', 'true');
    expect(document.querySelector('[data-ambient-lights]')).toHaveAttribute('data-still', 'true');
  });

  it('lets the card float with full motion, unless asked to stay still', () => {
    emulateReducedMotion(false);
    render(
      <>
        <AccountCard type="GOLD" />
        <AccountCard type="FREE" still />
      </>,
    );
    expect(screen.getByRole('img', { name: 'Cartão Gold' })).toHaveAttribute('data-still', 'false');
    expect(screen.getByRole('img', { name: 'Cartão Free' })).toHaveAttribute('data-still', 'true');
  });

  it('shows the final amount at once when motion is reduced', () => {
    emulateReducedMotion(true);
    render(<CountUp cents={1_248_090} />);
    expect(screen.getByText('R$ 12.480,90')).toBeInTheDocument();
  });

  it('counts up and always ends on the exact amount', () => {
    vi.useFakeTimers();
    emulateReducedMotion(false);
    render(<CountUp cents={1_248_091} />);
    expect(screen.queryByText('R$ 12.480,91')).not.toBeInTheDocument();
    act(() => vi.advanceTimersByTime(3_000));
    expect(screen.getByText('R$ 12.480,91')).toBeInTheDocument();
  });

  it('formats negative amounts exactly', () => {
    emulateReducedMotion(true);
    render(<CountUp cents={-8_990} />);
    expect(screen.getByText('-R$ 89,90')).toBeInTheDocument();
  });
});
