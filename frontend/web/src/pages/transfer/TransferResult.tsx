import { domAnimation, LazyMotion, m } from 'motion/react';
import { Check, Loader, Undo2, X } from 'lucide-react';
import { useEffect, useState, type ReactNode } from 'react';
import { Link } from 'react-router';
import { Button } from '@/components/ui/button';
import { CountUp, EASE_OUT_EXPO, Reveal, usePrefersReducedMotion } from '@/design/motion';
import { GlassPanel } from '@/design/GlassPanel';
import { bff } from '@/lib/bff-client';
import { formatCents } from '@/lib/money';
import { settleAttempt, type TransferAttempt } from '@/lib/transfer-attempt';
import { isFinalStatus, type TransferStatus } from './transfer-types';

const POLL_EVERY_MS = 1000;
/** The status is asked for up to this many seconds, then the screen sends the user to the statement. */
const POLL_SECONDS = 30;

const COPY: Record<string, { title: string; label: string; tone: string }> = {
  COMPLETED: { title: 'Transferência concluída', label: 'Concluída', tone: 'text-positive' },
  FAILED: { title: 'Transferência recusada', label: 'Recusada', tone: 'text-negative' },
  COMPENSATED: { title: 'Transferência devolvida', label: 'Devolvida', tone: 'text-accent-1' },
};

const RING: Record<string, string> = {
  COMPLETED: 'border-positive/50 bg-positive/10 text-positive',
  FAILED: 'border-negative/50 bg-negative/10 text-negative',
  COMPENSATED: 'border-accent-1/50 bg-accent-1/10 text-accent-1',
};

/** The animated result: pending first, then completed, refused or returned. */
export function TransferResult({
  recipientName,
  cents,
  created,
  attempt,
  resumed,
  onNew,
}: {
  recipientName: string;
  cents: number;
  created: TransferStatus;
  attempt: TransferAttempt;
  resumed: boolean;
  onNew: () => void;
}) {
  const [current, setCurrent] = useState<TransferStatus>(created);
  const [timedOut, setTimedOut] = useState(false);
  const final = isFinalStatus(current.status);

  // A final status that was shown ends the attempt: the next equal transfer is a new one.
  useEffect(() => {
    if (isFinalStatus(current.status)) settleAttempt(attempt);
  }, [current.status, attempt]);

  useEffect(() => {
    let cancelled = false;
    const read = (): Promise<TransferStatus | null> =>
      bff<TransferStatus>(`/bff/transfers/${created.id}`).catch(() => null);

    if (isFinalStatus(created.status)) {
      // The POST carries no reason: read it once now instead of waiting for a poll.
      void read().then((transfer) => {
        if (!cancelled && transfer && isFinalStatus(transfer.status)) setCurrent(transfer);
      });
      return () => {
        cancelled = true;
      };
    }

    let seconds = 0;
    let inFlight = false;
    const timer = setInterval(() => {
      seconds += 1;
      if (seconds >= POLL_SECONDS) {
        clearInterval(timer);
        setTimedOut(true);
        return;
      }
      if (inFlight) return;
      inFlight = true;
      void read().then((transfer) => {
        inFlight = false;
        if (cancelled || !transfer) return;
        setCurrent(transfer);
        if (isFinalStatus(transfer.status)) clearInterval(timer);
      });
    }, POLL_EVERY_MS);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [created.id, created.status]);

  const copy = COPY[current.status];
  const waiting = !final && !timedOut;
  const title = final
    ? (copy?.title ?? 'Transferência')
    : timedOut
      ? 'Transferência em andamento'
      : 'Enviando sua transferência';

  return (
    <section
      aria-label="Resultado da transferência"
      className="flex flex-col items-center gap-5 text-center"
    >
      <StatusBadge status={final ? current.status : 'PENDING'} />
      <Reveal delay={0.12}>
        <h2 className="font-heading text-[1.375rem] leading-[1.15] font-bold tracking-[-0.03em]">
          {title}
        </h2>
      </Reveal>
      <Reveal delay={0.2} className="flex flex-col items-center gap-1">
        <p className="font-heading text-4xl leading-none font-bold tracking-[-0.04em] tabular-nums">
          <span className="sr-only">{formatCents(cents)}</span>
          <span aria-hidden="true">
            <CountUp cents={cents} />
          </span>
        </p>
        <p className="text-sm text-muted-foreground">para {recipientName}</p>
      </Reveal>

      <Reveal delay={0.3} className="w-full">
        <GlassPanel className="flex flex-col gap-3 p-4 text-left">
          <div className="flex items-center justify-between gap-4">
            <span className="text-sm text-muted-foreground">Status</span>
            <span role="status" className={`text-sm font-semibold ${final ? copy?.tone : ''}`}>
              {final
                ? (copy?.label ?? current.status)
                : timedOut
                  ? 'Ainda processando. Veja o resultado no extrato em instantes.'
                  : 'Processando…'}
            </span>
          </div>
          {final && current.reason ? (
            <p className="text-sm text-muted-foreground">Motivo: {current.reason}</p>
          ) : null}
        </GlassPanel>
      </Reveal>

      {resumed ? (
        <p
          role="status"
          data-testid="resumed-notice"
          className="rounded-xl border border-line bg-glass px-4 py-3 text-left text-sm"
        >
          Você já tinha confirmado uma transferência igual sem ver o resultado. Este é o resultado
          dela: não houve um segundo envio. Para enviar mais uma, use Nova transferência.
        </p>
      ) : null}

      {!waiting ? (
        <Reveal delay={0.1} className="flex w-full flex-col gap-3">
          <Button type="button" size="lg" onClick={onNew} className="h-[46px] text-base">
            Nova transferência
          </Button>
          <div className="flex gap-3">
            <LinkButton to="/extrato">Ver extrato</LinkButton>
            <LinkButton to="/inicio">Voltar ao início</LinkButton>
          </div>
        </Reveal>
      ) : null}
    </section>
  );
}

function LinkButton({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Button asChild variant="secondary" size="lg" className="h-[46px] flex-1 text-base">
      <Link to={to}>{children}</Link>
    </Button>
  );
}

function StatusBadge({ status }: { status: string }) {
  const reduced = usePrefersReducedMotion();
  const Icon =
    status === 'COMPLETED'
      ? Check
      : status === 'FAILED'
        ? X
        : status === 'COMPENSATED'
          ? Undo2
          : Loader;
  const pending = status === 'PENDING';
  const ring = RING[status] ?? 'border-line bg-glass text-foreground';
  const icon = (
    <Icon
      aria-hidden="true"
      className={`size-10 ${pending ? 'animate-spin [animation-duration:2.4s]' : ''}`}
      strokeWidth={2.25}
    />
  );
  const classes = `grid size-[5.5rem] place-items-center rounded-full border ${ring}`;
  if (reduced) return <div className={classes}>{icon}</div>;
  return (
    <LazyMotion features={domAnimation} strict>
      <m.div
        key={status}
        className={classes}
        initial={{ scale: pending ? 0.9 : 0.55, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.55, ease: EASE_OUT_EXPO }}
      >
        {icon}
      </m.div>
    </LazyMotion>
  );
}
