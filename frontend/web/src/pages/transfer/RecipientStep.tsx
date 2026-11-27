import { useState, type FormEvent } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { formatPhone, isMobilePhone } from '@/lib/phone';

/** Step 1: the phone of the person who receives. */
export function RecipientStep({
  phone,
  busy,
  onPhoneChange,
  onNext,
}: {
  phone: string;
  busy: boolean;
  onPhoneChange: (phone: string) => void;
  onNext: () => void;
}) {
  const [touched, setTouched] = useState(false);
  const valid = isMobilePhone(phone);
  const showError = touched && phone !== '' && !valid;

  const submit = (event: FormEvent): void => {
    event.preventDefault();
    setTouched(true);
    if (valid && !busy) onNext();
  };

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-5">
      <h2 className="font-heading text-4xl leading-[1.05] font-bold tracking-[-0.035em]">
        Para quem você quer enviar?
      </h2>
      <div className="flex flex-col gap-2">
        <Label htmlFor="recipient-phone">Celular do destinatário</Label>
        <Input
          id="recipient-phone"
          name="phone"
          type="tel"
          inputMode="numeric"
          autoComplete="off"
          placeholder="(11) 98765-4321"
          value={phone}
          aria-invalid={showError}
          aria-describedby={showError ? 'recipient-phone-error' : undefined}
          onChange={(event) => onPhoneChange(formatPhone(event.target.value))}
          onBlur={() => setTouched(true)}
          className="h-12 rounded-lg px-3.5 text-lg"
        />
        {showError ? (
          <p id="recipient-phone-error" className="text-sm text-negative">
            Informe um celular com DDD, como (11) 98765-4321.
          </p>
        ) : null}
      </div>
      <Button type="submit" size="lg" disabled={!valid || busy} className="h-[46px] text-base">
        {busy ? 'Procurando…' : 'Próximo'}
      </Button>
    </form>
  );
}
