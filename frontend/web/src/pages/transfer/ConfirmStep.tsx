import { Button } from '@/components/ui/button';
import { formatCents } from '@/lib/money';
import { displayPhone } from '@/lib/phone';

/** Step 3: the name and the amount to be checked before anything is sent. */
export function ConfirmStep({
  recipientName,
  phone,
  cents,
  busy,
  onBack,
  onConfirm,
}: {
  recipientName: string;
  phone: string;
  cents: number;
  busy: boolean;
  onBack: () => void;
  onConfirm: () => void;
}) {
  return (
    <div className="flex flex-col gap-6">
      <h2 className="font-heading text-4xl leading-[1.05] font-bold tracking-[-0.035em]">
        Confirme o envio
      </h2>
      <dl className="flex flex-col gap-4">
        <div className="flex flex-col gap-1">
          <dt className="text-xs text-muted-foreground">Você vai enviar</dt>
          <dd className="font-heading text-4xl font-bold tracking-[-0.035em] tabular-nums">
            {formatCents(cents)}
          </dd>
        </div>
        <div className="flex flex-col gap-1">
          <dt className="text-xs text-muted-foreground">para</dt>
          <dd className="text-xl font-semibold tracking-[-0.02em]">{recipientName}</dd>
          <dd className="text-sm text-muted-foreground tabular-nums">{displayPhone(phone)}</dd>
        </div>
      </dl>
      <div className="flex gap-3">
        <Button
          type="button"
          variant="secondary"
          size="lg"
          disabled={busy}
          onClick={onBack}
          className="h-[46px] flex-1 text-base"
        >
          Voltar
        </Button>
        <Button
          type="button"
          size="lg"
          disabled={busy}
          onClick={onConfirm}
          className="h-[46px] flex-[2] text-base"
        >
          {busy ? 'Enviando…' : 'Confirmar'}
        </Button>
      </div>
    </div>
  );
}
