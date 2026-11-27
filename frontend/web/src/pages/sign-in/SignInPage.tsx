import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { Button } from '@/components/ui/button';
import { AccountCard } from '@/design/AccountCard';
import { GradientText } from '@/design/GradientText';
import { Reveal } from '@/design/motion';
import { BffError } from '@/lib/bff-client';
import { isSixDigitPassword } from '@/lib/password';
import { problemMessage } from '@/lib/problem';
import { signIn } from '@/lib/session';
import { AuthHeading, AuthLayout, Field, FormError, SUBMIT_CLASS, isEmail } from './AuthLayout';

export function SignInPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const passwordInvalid = password !== '' && !isSixDigitPassword(password);
  const valid = isEmail(email) && isSixDigitPassword(password);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!valid || busy) return;
    setBusy(true);
    setError(null);
    try {
      await signIn(email, password);
      await navigate('/inicio');
    } catch (failure) {
      setError(
        failure instanceof BffError && failure.status === 401
          ? 'E-mail ou senha incorretos.'
          : problemMessage(failure, 'Não foi possível entrar agora. Tente de novo.'),
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout
      aside={
        <>
          <Reveal>
            <p className="font-heading text-4xl leading-[1.05] font-bold tracking-tight md:text-5xl">
              Seu dinheiro.
              <br />
              <GradientText>Sem atrito.</GradientText>
            </p>
          </Reveal>
          <Reveal delay={0.05} className="hidden justify-start lg:flex">
            <AccountCard type="BLACK" last4="4821" size="lg" />
          </Reveal>
        </>
      }
    >
      <AuthHeading>Entrar</AuthHeading>
      <FormError>{error}</FormError>
      <form onSubmit={submit} noValidate className="flex flex-col gap-4">
        <Field
          label="E-mail"
          name="email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <Field
          label="Senha (6 dígitos)"
          name="password"
          type="password"
          inputMode="numeric"
          maxLength={6}
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={passwordInvalid ? 'A senha tem exatamente 6 números.' : null}
        />
        <Button type="submit" className={SUBMIT_CLASS} disabled={!valid || busy}>
          Entrar
        </Button>
      </form>
      <p className="text-sm text-muted-foreground">
        Novo por aqui?{' '}
        <Link
          to="/criar-conta"
          className="font-medium text-foreground underline underline-offset-4"
        >
          Criar conta
        </Link>
      </p>
    </AuthLayout>
  );
}
