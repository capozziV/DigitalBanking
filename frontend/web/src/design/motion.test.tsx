import type { ReactNode } from 'react';
import { act, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AccountCard } from '@/design/AccountCard';
import { AmbientLights } from '@/design/AmbientLights';
import { CountUp, PageTransition, Reveal, usePrefersReducedMotion } from '@/design/motion';
import { formatCents } from '@/lib/money';
import { MemoryRouter } from 'react-router';

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

  it.each([1_248_091, 1, 29, -8_990, Number.MAX_SAFE_INTEGER])(
    'ends on exactly formatCents(%i)',
    (cents) => {
      vi.useFakeTimers();
      emulateReducedMotion(false);
      const { container } = render(<CountUp cents={cents} />);
      act(() => vi.advanceTimersByTime(3_000));
      expect(container.textContent).toBe(formatCents(cents));
    },
  );

  it('shows formatCents of the amount at once when motion is reduced', () => {
    emulateReducedMotion(true);
    const { container } = render(<CountUp cents={Number.MAX_SAFE_INTEGER} />);
    expect(container.textContent).toBe(formatCents(Number.MAX_SAFE_INTEGER));
  });

  const moving = (element: HTMLElement): boolean =>
    /opacity|transform/.test(element.getAttribute('style') ?? '');

  it.each([
    ['PageTransition', (child: ReactNode) => <PageTransition>{child}</PageTransition>],
    ['Reveal', (child: ReactNode) => <Reveal>{child}</Reveal>],
  ])('%s renders without motion when motion is reduced', (_name, wrap) => {
    emulateReducedMotion(true);
    render(<MemoryRouter>{wrap(<p>conteúdo</p>)}</MemoryRouter>);
    expect(moving(screen.getByText('conteúdo').parentElement!)).toBe(false);
  });

  it.each([
    ['PageTransition', (child: ReactNode) => <PageTransition>{child}</PageTransition>],
    ['Reveal', (child: ReactNode) => <Reveal>{child}</Reveal>],
  ])('%s starts transparent with full motion', (_name, wrap) => {
    emulateReducedMotion(false);
    render(<MemoryRouter>{wrap(<p>conteúdo</p>)}</MemoryRouter>);
    const wrapper = screen.getByText('conteúdo').parentElement!;
    expect(moving(wrapper)).toBe(true);
    expect(wrapper.style.opacity).toBe('0');
  });

  it('formats negative amounts exactly', () => {
    emulateReducedMotion(true);
    render(<CountUp cents={-8_990} />);
    expect(screen.getByText('-R$ 89,90')).toBeInTheDocument();
  });
});
