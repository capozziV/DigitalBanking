import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { Button } from '@/components/ui/button';
import { getSnapshot, reload } from '@/lib/session';
import { AuthHeading, AuthLayout, SUBMIT_CLASS } from '@/pages/sign-in/AuthLayout';

/** Where the guards send a session that could not be loaded. It has no guard, so it cannot loop. */
export function UnavailablePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [busy, setBusy] = useState(false);
  const from = (location.state as { from?: string } | null)?.from;

  /** `loadFailed` stays on until a reload succeeds, so leave only when it did. */
  async function retry() {
    setBusy(true);
    try {
      await reload();
      if (getSnapshot().loadFailed) return;
      await navigate(from && from !== '/indisponivel' ? from : '/', { replace: true });
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout>
      <AuthHeading>Não foi possível carregar</AuthHeading>
      <p role="alert" className="text-sm text-muted-foreground">
        O serviço está indisponível no momento. Aguarde um instante e tente de novo.
      </p>
      <Button type="button" className={SUBMIT_CLASS} disabled={busy} onClick={() => void retry()}>
        Tentar de novo
      </Button>
    </AuthLayout>
  );
}
