import { usePrefersReducedMotion } from '@/design/motion';

/** The diffuse blue, lilac and pink lights behind the content; still when motion is reduced. */
export function AmbientLights({ intensity = 'low' }: { intensity?: 'low' | 'high' }) {
  const reduced = usePrefersReducedMotion();
  return (
    <div
      aria-hidden="true"
      className="ambient-lights"
      data-ambient-lights=""
      data-intensity={intensity}
      data-still={String(reduced)}
    >
      <span className="ambient-light ambient-light--blue" />
      <span className="ambient-light ambient-light--lilac" />
      <span className="ambient-light ambient-light--pink" />
    </div>
  );
}
