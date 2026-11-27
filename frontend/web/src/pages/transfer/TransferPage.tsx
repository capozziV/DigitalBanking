import { useQuery } from '@tanstack/react-query';
import { useCallback, useRef, useState } from 'react';
import { GlassPanel } from '@/design/GlassPanel';
import type { DailyLimit } from '@/lib/api-types';
import { bff } from '@/lib/bff-client';
import { phoneDigits } from '@/lib/phone';
import { problemMessage } from '@/lib/problem';
import { useSession } from '@/lib/session';
import { openAttempt, type TransferAttempt } from '@/lib/transfer-attempt';
import { AmountStep } from './AmountStep';
import { ConfirmStep } from './ConfirmStep';
import { RecipientStep } from './RecipientStep';
import { StepBar } from './StepBar';
import { TransferResult } from './TransferResult';
import type { Recipient, TransferStatus, TransferStep } from './transfer-types';

interface Outcome {
  created: TransferStatus;
  attempt: TransferAttempt;
  resumed: boolean;
}

/** Transfer by phone in three steps (who, how much, confirm) and the result of the transfer. */
export function TransferPage() {
  const { email } = useSession();
  const [step, setStep] = useState<TransferStep>('phone');
  const [phone, setPhone] = useState('');
  const [recipient, setRecipient] = useState<{ name: string; token: string; phone: string } | null>(
    null,
  );
  const [cents, setCents] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [outcome, setOutcome] = useState<Outcome | null>(null);
  // State updates are not instant: this guard is what makes two fast clicks one request.
  const sending = useRef(false);

  // The available amount of today comes from the home, as the home itself shows it.
  const limit = useQuery({
    queryKey: ['transfer', 'daily-limit'],
    queryFn: async () => (await bff<{ dailyLimit: DailyLimit }>('/bff/home')).dailyLimit,
    gcTime: 0,
    staleTime: 0,
    refetchOnMount: 'always',
    retry: false,
  });

  const lookUp = async (): Promise<void> => {
    if (sending.current) return;
    sending.current = true;
    setBusy(true);
    setError(null);
    const digits = phoneDigits(phone);
    try {
      const found = await bff<Recipient>(`/bff/transfers/recipient?phone=${digits}`);
      setRecipient({ name: found.recipientName, token: found.recipientToken, phone: digits });
      setStep('amount');
    } catch (failure) {
      setError(problemMessage(failure, 'Não foi possível encontrar o destinatário.'));
    } finally {
      sending.current = false;
      setBusy(false);
    }
  };

  const confirm = async (): Promise<void> => {
    if (sending.current || !recipient) return;
    sending.current = true;
    setBusy(true);
    setError(null);
    // Read before the first await, so that the key and the body are of the same transfer.
    const body = { recipientToken: recipient.token, amount: cents };
    const intent = { owner: email, phone: recipient.phone, amount: cents };
    try {
      const attempt = await openAttempt(intent);
      const created = await bff<TransferStatus>('/bff/transfers', {
        method: 'POST',
        body,
        headers: { 'Idempotency-Key': attempt.idempotencyKey },
      });
      // If the user left meanwhile, no result is mounted and the attempt stays open for the return.
      setOutcome({ created, attempt, resumed: attempt.resumed });
      setStep('result');
    } catch (failure) {
      setError(
        problemMessage(
          failure,
          'Não foi possível confirmar o envio. Tente de novo aqui ou confira o extrato.',
        ),
      );
    } finally {
      sending.current = false;
      setBusy(false);
    }
  };

  const goTo = (target: Exclude<TransferStep, 'result'>): void => {
    if (sending.current) return;
    setError(null);
    setStep(target);
  };

  const startOver = (): void => {
    setStep('phone');
    setPhone('');
    setRecipient(null);
    setCents(0);
    setOutcome(null);
    setError(null);
    void limit.refetch();
  };

  const changeCents = useCallback((next: number) => setCents(next), []);

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-6">
      <h1 className="sr-only">Transferir</h1>
      {step !== 'result' ? <StepBar current={step} disabled={busy} onGo={goTo} /> : null}
      <GlassPanel className="flex flex-col gap-5 p-5 sm:p-6">
        {error ? (
          <p
            role="alert"
            className="rounded-xl border border-negative/40 bg-negative/10 px-4 py-3 text-sm"
          >
            {error}
          </p>
        ) : null}
        {step === 'phone' ? (
          <RecipientStep phone={phone} busy={busy} onPhoneChange={setPhone} onNext={lookUp} />
        ) : null}
        {step === 'amount' && recipient ? (
          <AmountStep
            recipientName={recipient.name}
            phone={recipient.phone}
            cents={cents}
            onChange={changeCents}
            available={limit.data?.available ?? null}
            limit={limit.data?.limit ?? null}
            limitFailed={limit.isError}
            onContinue={() => goTo('confirm')}
          />
        ) : null}
        {step === 'confirm' && recipient ? (
          <ConfirmStep
            recipientName={recipient.name}
            phone={recipient.phone}
            cents={cents}
            busy={busy}
            onBack={() => goTo('amount')}
            onConfirm={confirm}
          />
        ) : null}
        {step === 'result' && recipient && outcome ? (
          <TransferResult
            recipientName={recipient.name}
            cents={cents}
            created={outcome.created}
            attempt={outcome.attempt}
            resumed={outcome.resumed}
            onNew={startOver}
          />
        ) : null}
      </GlassPanel>
    </div>
  );
}
