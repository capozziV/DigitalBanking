import { cn } from 'cn';
import type { TransferStep } from './transfer-types';

const STEPS: { id: Exclude<TransferStep, 'result'>; label: string }[] = [
  { id: 'phone', label: 'Destinatário' },
  { id: 'amount', label: 'Valor' },
  { id: 'confirm', label: 'Confirmar' },
];

/** The three steps; the ones already done are buttons that go back without losing what was typed. */
export function StepBar({
  current,
  disabled,
  onGo,
}: {
  current: Exclude<TransferStep, 'result'>;
  disabled: boolean;
  onGo: (step: Exclude<TransferStep, 'result'>) => void;
}) {
  const currentIndex = STEPS.findIndex((step) => step.id === current);
  return (
    <ol aria-label="Passos da transferência" className="flex items-center gap-2">
      {STEPS.map((step, index) => {
        const done = index < currentIndex;
        const active = index === currentIndex;
        const body = (
          <>
            <span
              aria-hidden="true"
              className={cn(
                'h-1 w-full rounded-full transition-colors duration-300',
                done || active ? 'bg-foreground' : 'bg-glass-strong',
              )}
            />
            <span
              className={cn(
                'text-[11px] font-medium',
                active ? 'text-foreground' : 'text-muted-foreground',
              )}
            >
              {index + 1}. {step.label}
            </span>
          </>
        );
        return (
          <li key={step.id} className="flex-1" aria-current={active ? 'step' : undefined}>
            {done ? (
              <button
                type="button"
                disabled={disabled}
                onClick={() => onGo(step.id)}
                aria-label={`Voltar para ${step.label}`}
                className="flex min-h-11 w-full flex-col justify-center gap-1.5 rounded-sm py-1 text-left disabled:opacity-50"
              >
                {body}
              </button>
            ) : (
              <div className="flex flex-col gap-1.5 py-1">{body}</div>
            )}
          </li>
        );
      })}
    </ol>
  );
}
