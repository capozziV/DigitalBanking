import { domAnimation, LazyMotion, m } from 'motion/react';
import { useEffect, useRef, useState, useSyncExternalStore, type ReactNode } from 'react';
import { useLocation } from 'react-router';
import { formatCents } from '@/lib/money';

const REDUCED_MOTION = '(prefers-reduced-motion: reduce)';
/** Exponential ease-out: fast start, long settle. Shared by every entrance. */
export const EASE_OUT_EXPO = [0.16, 1, 0.3, 1] as const;
const COUNT_UP_MS = 900;

const mediaQuery = (): MediaQueryList | null =>
  typeof globalThis.matchMedia === 'function' ? globalThis.matchMedia(REDUCED_MOTION) : null;

function subscribe(onChange: () => void): () => void {
  const query = mediaQuery();
  query?.addEventListener?.('change', onChange);
  return () => query?.removeEventListener?.('change', onChange);
}

/** True when the visitor asked the system to reduce motion; false where it cannot be read. */
export function usePrefersReducedMotion(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => mediaQuery()?.matches ?? false,
    () => false,
  );
}

/** Fades the screen in on every route change; a plain cut when motion is reduced. */
export function PageTransition({ children }: { children: ReactNode }) {
  const reduced = usePrefersReducedMotion();
  const { pathname } = useLocation();
  if (reduced) return <div>{children}</div>;
  return (
    <LazyMotion features={domAnimation} strict>
      <m.div
        key={pathname}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.42, ease: EASE_OUT_EXPO }}
      >
        {children}
      </m.div>
    </LazyMotion>
  );
}

/** Soft entrance for one item; `delay` in seconds staggers a group. */
export function Reveal({
  children,
  delay = 0,
  className,
}: {
  children: ReactNode;
  delay?: number;
  className?: string;
}) {
  const reduced = usePrefersReducedMotion();
  if (reduced) return <div className={className}>{children}</div>;
  return (
    <LazyMotion features={domAnimation} strict>
      <m.div
        className={className}
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, delay, ease: EASE_OUT_EXPO }}
      >
        {children}
      </m.div>
    </LazyMotion>
  );
}

/**
 * Counts an amount of cents up to its value. Every frame shows a whole number of cents and the
 * last frame is always the exact value; with reduced motion the value shows at once.
 */
export function CountUp({ cents, className }: { cents: number; className?: string }) {
  const reduced = usePrefersReducedMotion();
  const [shown, setShown] = useState(0);
  const shownRef = useRef(0);

  useEffect(() => {
    if (reduced) {
      shownRef.current = cents;
      return;
    }
    const from = shownRef.current;
    let start: number | null = null;
    let frame = requestAnimationFrame(function step(now) {
      start ??= now;
      const progress = Math.min(1, (now - start) / COUNT_UP_MS);
      const value =
        progress >= 1 ? cents : Math.round(from + (cents - from) * (1 - 2 ** (-10 * progress)));
      shownRef.current = value;
      setShown(value);
      if (progress < 1) frame = requestAnimationFrame(step);
    });
    return () => cancelAnimationFrame(frame);
  }, [cents, reduced]);

  return <span className={className}>{formatCents(reduced ? cents : shown)}</span>;
}
