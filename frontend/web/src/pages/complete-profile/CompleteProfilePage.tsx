import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { Button } from '@/components/ui/button';
import { bff } from '@/lib/bff-client';
import { formatPhone, isMobilePhone, phoneDigits } from '@/lib/phone';
import { problemMessage } from '@/lib/problem';
import { reload } from '@/lib/session';
import {
  AuthHeading,
  AuthLayout,
  Field,
  FormError,
  SUBMIT_CLASS,
} from '@/pages/sign-in/AuthLayout';

export function CompleteProfilePage() {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const phoneInvalid = phone !== '' && !isMobilePhone(phone);
  const valid = /\S{2,}/.test(name) && name.length <= 120 && isMobilePhone(phone);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!valid || busy) return;
    setBusy(true);
    setError(null);
    try {
      await bff('/bff/customers', {
        method: 'POST',
        body: { name: name.trim(), phone: phoneDigits(phone) },
      });
      // Not `loadMe`: if this load fails, the guard of Início shows the error screen.
      await reload();
      await navigate('/inicio');
    } catch (failure) {
      setError(problemMessage(failure, 'Não foi possível concluir o cadastro.'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout>
      <AuthHeading>Completar cadastro</AuthHeading>
      <p className="text-sm text-muted-foreground">
        Diga seu nome completo e seu celular. É pelo celular que outros clientes enviam
        transferências para você.
      </p>
      <FormError>{error}</FormError>
      <form onSubmit={submit} noValidate className="flex flex-col gap-4">
        <Field
          label="Nome completo"
          name="name"
          type="text"
          autoComplete="name"
          maxLength={120}
          value={name}
          onChange={(event) => setName(event.target.value)}
        />
        <Field
          label="Celular"
          name="phone"
          type="tel"
          inputMode="numeric"
          autoComplete="tel-national"
          placeholder="(11) 98765-4321"
          value={phone}
          onChange={(event) => setPhone(formatPhone(event.target.value))}
          error={phoneInvalid ? 'Informe um celular com DDD, como (11) 98765-4321.' : null}
        />
        <Button type="submit" className={SUBMIT_CLASS} disabled={!valid || busy}>
          Concluir
        </Button>
      </form>
    </AuthLayout>
  );
}
