import type { EntryKind } from '@/lib/api-types';

/** Credit: filled circle with a down arrow. Debit: hollow circle with an up arrow. */
export function EntryIcon({ kind }: { kind: EntryKind }) {
  const credit = kind === 'CREDIT';
  return (
    <span
      role="img"
      aria-label={credit ? 'Entrada' : 'Saída'}
      data-style={credit ? 'filled' : 'hollow'}
      className={
        credit
          ? 'grid size-9 shrink-0 place-items-center rounded-full bg-foreground text-sm text-background'
          : 'grid size-9 shrink-0 place-items-center rounded-full border border-line text-sm text-foreground'
      }
    >
      {credit ? '↓' : '↑'}
    </span>
  );
}
