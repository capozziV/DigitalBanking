import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { Button } from '@/components/ui/button';
import { isSixDigitPassword, passwordsMatch } from '@/lib/password';
import { problemMessage } from '@/lib/problem';
import { confirm, signIn, signUp } from '@/lib/session';
import {
  AuthHeading,
  AuthLayout,
  Field,
  FormError,
  SUBMIT_CLASS,
  isEmail,
} from '@/pages/sign-in/AuthLayout';

export function SignUpPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<'details' | 'code'>('details');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const passwordInvalid = password !== '' && !isSixDigitPassword(password);
  const mismatch = confirmation !== '' && !passwordsMatch(password, confirmation);
  const detailsValid =
    isEmail(email) && isSixDigitPassword(password) && passwordsMatch(password, confirmation);
  const codeValid = /^\d{6}$/.test(code);

  async function run(action: () => Promise<void>, fallback: string) {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (failure) {
      setError(problemMessage(failure, fallback));
    } finally {
      setBusy(false);
    }
  }

  async function finish() {
    await signIn(email, password);
    await navigate('/inicio');
  }

  function submitDetails(event: FormEvent) {
    event.preventDefault();
    if (!detailsValid || busy) return;
    void run(async () => {
      const { confirmationRequired } = await signUp(email, password);
      if (confirmationRequired) setStep('code');
      else await finish();
    }, 'Não foi possível criar a conta agora.');
  }

  function submitCode(event: FormEvent) {
    event.preventDefault();
    if (!codeValid || busy) return;
    void run(async () => {
      await confirm(email, code);
      await finish();
    }, 'Não foi possível confirmar o código.');
  }

  return (
    <AuthLayout>
      <AuthHeading>Criar conta</AuthHeading>
      <FormError>{error}</FormError>
      {step === 'details' ? (
        <form onSubmit={submitDetails} noValidate className="flex flex-col gap-4">
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
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            error={passwordInvalid ? 'A senha tem exatamente 6 números.' : null}
          />
          <Field
            label="Confirme a senha"
            name="confirmation"
            type="password"
            inputMode="numeric"
            maxLength={6}
            autoComplete="new-password"
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
            error={mismatch ? 'As senhas não conferem.' : null}
          />
          <Button type="submit" className={SUBMIT_CLASS} disabled={!detailsValid || busy}>
            Continuar
          </Button>
        </form>
      ) : (
        <form onSubmit={submitCode} noValidate className="flex flex-col gap-4">
          <p className="text-sm text-muted-foreground">Enviamos um código para {email}.</p>
          <Field
            label="Código do e-mail"
            name="code"
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            value={code}
            onChange={(event) => setCode(event.target.value)}
          />
          <Button type="submit" className={SUBMIT_CLASS} disabled={!codeValid || busy}>
            Confirmar e entrar
          </Button>
        </form>
      )}
      <p className="text-sm text-muted-foreground">
        Já tem conta?{' '}
        <Link to="/entrar" className="font-medium text-foreground underline underline-offset-4">
          Entrar
        </Link>
      </p>
    </AuthLayout>
  );
}
