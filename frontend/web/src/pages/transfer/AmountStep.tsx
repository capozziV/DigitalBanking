import { useEffect } from 'react';
import { Delete } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { appendDigit, dropDigit, formatCents } from '@/lib/money';
import { displayPhone } from '@/lib/phone';

const KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9'] as const;
const KEY_CLASS =
  'grid h-16 place-items-center rounded-2xl bg-glass-strong text-2xl font-semibold tabular-nums transition-[background-color,transform] duration-150 hover:bg-white/15 active:scale-95 disabled:opacity-50';

/** Step 2: the amount on a big numeric keypad, with what is left of today's limit. */
export function AmountStep({
  recipientName,
  phone,
  cents,
  onChange,
  available,
  limit,
  limitFailed,
  onContinue,
}: {
  recipientName: string;
  phone: string;
  cents: number;
  onChange: (cents: number) => void;
  /** Cents the account may still send today; `null` while unknown. */
  available: number | null;
  limit: number | null;
  limitFailed: boolean;
  onContinue: () => void;
}) {
  const overLimit = available !== null && cents > available;
  const canContinue = cents > 0 && !overLimit;

  // A desktop keyboard types the same digits as the keypad.
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent): void => {
      if (event.ctrlKey || event.metaKey || event.altKey) return;
      const target = event.target instanceof Element ? event.target : null;
      if (target?.closest('input, textarea, select, [contenteditable], [role="dialog"]')) return;
      if (/^[0-9]$/.test(event.key)) onChange(appendDigit(cents, Number(event.key)));
      else if (event.key === 'Backspace') onChange(dropDigit(cents));
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [cents, onChange]);

  const text = formatCents(cents);
  const comma = text.lastIndexOf(',');

  return (
    <div className="flex flex-col gap-5">
      <h2 className="font-heading text-4xl leading-[1.05] font-bold tracking-[-0.035em]">
        Quanto você quer enviar?
      </h2>
      <div className="flex items-center gap-3">
        <span
          aria-hidden="true"
          className="grid size-10 shrink-0 place-items-center rounded-full bg-glass-strong text-sm font-semibold"
        >
          {initials(recipientName)}
        </span>
        <div className="min-w-0">
          <p className="truncate font-medium">{recipientName}</p>
          <p className="text-sm text-muted-foreground tabular-nums">{displayPhone(phone)}</p>
        </div>
      </div>

      <output
        aria-label="Valor"
        className={`block text-center font-heading text-5xl font-bold tracking-[-0.04em] tabular-nums ${overLimit ? 'text-negative' : ''}`}
      >
        {text.slice(0, comma)}
        <span className="text-muted-foreground">{text.slice(comma)}</span>
      </output>

      <LimitBar available={available} limit={limit} failed={limitFailed} />
      {overLimit ? (
        <p aria-live="polite" className="text-center text-sm text-negative">
          Esse valor está acima do que resta no limite de hoje ({formatCents(available)}).
        </p>
      ) : null}

      <div className="grid grid-cols-3 gap-2.5">
        {KEYS.map((key) => (
          <button
            key={key}
            type="button"
            className={KEY_CLASS}
            onClick={() => onChange(appendDigit(cents, Number(key)))}
          >
            {key}
          </button>
        ))}
        <span aria-hidden="true" />
        <button type="button" className={KEY_CLASS} onClick={() => onChange(appendDigit(cents, 0))}>
          0
        </button>
        <button
          type="button"
          aria-label="Apagar"
          className={KEY_CLASS}
          onClick={() => onChange(dropDigit(cents))}
        >
          <Delete aria-hidden="true" className="size-6" />
        </button>
      </div>

      <Button
        type="button"
        size="lg"
        disabled={!canContinue}
        onClick={onContinue}
        className="h-[46px] text-base"
      >
        Continuar
      </Button>
    </div>
  );
}

function LimitBar({
  available,
  limit,
  failed,
}: {
  available: number | null;
  limit: number | null;
  failed: boolean;
}) {
  if (failed) {
    return (
      <p className="text-center text-sm text-muted-foreground">
        Não foi possível ler o limite de hoje. O envio é conferido de novo ao confirmar.
      </p>
    );
  }
  if (available === null || limit === null) return <Skeleton className="mx-auto h-4 w-64" />;
  const share = limit > 0 ? Math.min(100, Math.max(0, (available / limit) * 100)) : 0;
  return (
    <div className="flex flex-col gap-2">
      <p className="text-center text-sm text-muted-foreground">
        Disponível hoje: {formatCents(available)} de {formatCents(limit)}
      </p>
      <div
        role="progressbar"
        aria-label="Limite diário disponível"
        aria-valuemin={0}
        aria-valuemax={limit}
        aria-valuenow={available}
        className="mx-8 h-1.5 overflow-hidden rounded-full bg-glass-strong"
      >
        <div className="h-full rounded-full bg-foreground" style={{ width: `${share}%` }} />
      </div>
    </div>
  );
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  const first = parts[0]?.[0] ?? '';
  const last = parts.length > 1 ? (parts.at(-1)?.[0] ?? '') : '';
  return (first + last).toUpperCase();
}
