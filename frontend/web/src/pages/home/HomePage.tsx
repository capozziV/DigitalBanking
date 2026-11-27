import { useState } from 'react';
import { Link } from 'react-router';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { AccountCard } from '@/design/AccountCard';
import { GlassPanel } from '@/design/GlassPanel';
import { CountUp, Reveal } from '@/design/motion';
import { accountTypeLabel, type AccountType, type DailyLimit } from '@/lib/api-types';
import { bff } from '@/lib/bff-client';
import { formatCents } from '@/lib/money';
import { problemMessage } from '@/lib/problem';
import { loadMe, useSession } from '@/lib/session';
import { EntryList } from '../statement/EntryList';
import { LoadError } from '../statement/LoadError';
import { NO_ACCOUNT, useHome } from './use-home';

const usedPercent = (limit: DailyLimit | null): number =>
  !limit || limit.limit <= 0 ? 0 : Math.min(100, Math.round((limit.used / limit.limit) * 100));

/** Home: balance, the card of the account type, shortcuts, daily limit and latest entries. */
export function HomePage() {
  const session = useSession();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const hasAccount = session.account !== null;

  const query = useHome(hasAccount);
  const failed = query.isError;
  const home = query.data ?? (hasAccount ? null : NO_ACCOUNT);

  const openAccount = async () => {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await bff('/bff/accounts', { method: 'POST', body: {} });
      await loadMe();
      await query.refetch();
    } catch (caught) {
      setError(problemMessage(caught, 'Não foi possível abrir a conta.'));
    } finally {
      setBusy(false);
    }
  };

  const firstName = session.customer?.name.trim().split(/\s+/)[0] ?? '';
  const account = home?.account ?? null;
  const limit = home?.dailyLimit ?? null;
  const percent = usedPercent(limit);

  return (
    <div>
      <h1 className="font-heading text-3xl font-bold tracking-[-0.03em] lg:text-4xl">Início</h1>
      {failed ? (
        <div className="mt-4">
          <LoadError onRetry={() => void query.refetch()} />
        </div>
      ) : null}
      {home === null && !failed ? (
        <div className="mt-6 grid gap-4" aria-busy="true">
          <Skeleton className="h-40 rounded-2xl" />
          <Skeleton className="h-32 rounded-2xl" />
        </div>
      ) : null}
      {home !== null ? (
        <>
          <p className="mt-1 mb-6 text-sm text-muted-foreground">Olá, {firstName}</p>
          {account ? (
            <div className="grid gap-4 lg:grid-cols-2">
              <Reveal>
                <GlassPanel className="flex h-full flex-col p-5">
                  <span className="text-xs tracking-wide text-muted-foreground uppercase">
                    Saldo disponível
                  </span>
                  <strong
                    data-testid="balance"
                    className="mt-1 mb-1 block font-heading text-4xl font-bold tracking-[-0.04em] tabular-nums"
                  >
                    <CountUp cents={account.availableBalance} />
                  </strong>
                  <span className="mb-5 text-sm text-muted-foreground">
                    Conta {accountTypeLabel(account.type)} · saldo {formatCents(account.balance)} ·
                    cheque especial {formatCents(account.overdraftLimit)}
                  </span>
                  <div className="mt-auto flex gap-2">
                    <Button asChild className="flex-1">
                      <Link to="/transferir">Transferir</Link>
                    </Button>
                    <Button asChild variant="outline" className="flex-1">
                      <Link to="/extrato">Extrato</Link>
                    </Button>
                  </div>
                </GlassPanel>
              </Reveal>
              <Reveal delay={0.08} className="flex items-center justify-center py-2">
                <AccountCard type={account.type as AccountType} size="lg" />
              </Reveal>
              {limit ? (
                <Reveal delay={0.16}>
                  <GlassPanel className="p-5">
                    <span className="text-xs tracking-wide text-muted-foreground uppercase">
                      Limite diário · {formatCents(limit.used)} de {formatCents(limit.limit)}
                    </span>
                    <div
                      role="progressbar"
                      aria-label="Limite diário usado"
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-valuenow={percent}
                      className="mt-3 h-1 rounded-full bg-line"
                    >
                      <div
                        data-testid="limit-fill"
                        className="h-1 rounded-full bg-foreground"
                        style={{ width: `${percent}%` }}
                      />
                    </div>
                  </GlassPanel>
                </Reveal>
              ) : null}
              <Reveal delay={0.24} className={limit ? undefined : 'lg:col-span-2'}>
                <GlassPanel className="px-5 py-4">
                  <h2 className="mb-1 font-heading text-base font-semibold">Últimos lançamentos</h2>
                  {home.latestEntries.length > 0 ? (
                    <EntryList entries={home.latestEntries} detail="home" />
                  ) : (
                    <p className="py-2 text-sm text-muted-foreground">Nenhum lançamento ainda.</p>
                  )}
                </GlassPanel>
              </Reveal>
            </div>
          ) : (
            <GlassPanel className="max-w-xl p-6">
              <h2 className="mb-2 font-heading text-lg font-semibold">Você ainda não tem conta</h2>
              <p className="mb-5 text-sm text-muted-foreground">
                Abra uma conta para transferir. O bônus de boas-vindas vale só para a primeira
                conta.
              </p>
              {error ? (
                <p role="alert" className="mb-4 text-sm text-negative">
                  {error}
                </p>
              ) : null}
              <Button type="button" disabled={busy} onClick={() => void openAccount()}>
                Abrir conta
              </Button>
            </GlassPanel>
          )}
        </>
      ) : null}
    </div>
  );
}
