import { useState, type FormEvent } from 'react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { AccountCard } from '@/design/AccountCard';
import { GlassPanel } from '@/design/GlassPanel';
import { Reveal } from '@/design/motion';
import { accountTypeLabel, type AccountType } from '@/lib/api-types';
import { bff } from '@/lib/bff-client';
import { formatCents } from '@/lib/money';
import { displayPhone, formatPhone, isMobilePhone, phoneDigits } from '@/lib/phone';
import { problemMessage } from '@/lib/problem';
import { loadMe, signOut, useSession } from '@/lib/session';

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4 border-t border-line py-3 first:border-t-0">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="m-0 text-right text-sm">{children}</dd>
    </div>
  );
}

/** My account: the card, the customer and account data, phone change, sign out and close. */
export function AccountPage() {
  const { customer, account } = useSession();

  const [phone, setPhone] = useState('');
  const [phoneTouched, setPhoneTouched] = useState(false);
  const [phoneBusy, setPhoneBusy] = useState(false);
  const [phoneError, setPhoneError] = useState<string | null>(null);
  const [phoneSaved, setPhoneSaved] = useState(false);

  const [confirmingClose, setConfirmingClose] = useState(false);
  const [closeBusy, setCloseBusy] = useState(false);
  const [closeError, setCloseError] = useState<string | null>(null);

  const phoneValid = isMobilePhone(phone);

  const changePhone = async (event: FormEvent) => {
    event.preventDefault();
    if (!phoneValid || phoneBusy) return;
    setPhoneBusy(true);
    setPhoneError(null);
    setPhoneSaved(false);
    try {
      await bff('/bff/me/phone', { method: 'PUT', body: { phone: phoneDigits(phone) } });
      await loadMe();
      setPhone('');
      setPhoneTouched(false);
      setPhoneSaved(true);
    } catch (caught) {
      setPhoneError(problemMessage(caught, 'Não foi possível trocar o celular.'));
    } finally {
      setPhoneBusy(false);
    }
  };

  const closeAccount = async () => {
    if (closeBusy) return;
    setCloseBusy(true);
    setCloseError(null);
    try {
      await bff('/bff/accounts/close', { method: 'POST', body: {} });
      await loadMe();
      setConfirmingClose(false);
    } catch (caught) {
      setCloseError(problemMessage(caught, 'Não foi possível encerrar a conta.'));
    } finally {
      setCloseBusy(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-6 font-heading text-3xl font-bold tracking-[-0.03em]">Minha conta</h1>

      {account ? (
        <Reveal className="mb-6 flex justify-center">
          <AccountCard type={account.type as AccountType} size="lg" />
        </Reveal>
      ) : null}

      {customer ? (
        <Reveal delay={0.06}>
          <GlassPanel className="mb-4 p-5">
            <h2 className="mb-2 font-heading text-base font-semibold">Seus dados</h2>
            <dl className="m-0">
              <Row label="Nome">{customer.name}</Row>
              <Row label="E-mail">{customer.email}</Row>
              <Row label="Celular">{displayPhone(customer.phone)}</Row>
            </dl>
            <form
              onSubmit={(event) => void changePhone(event)}
              noValidate
              className="mt-4 grid gap-3"
            >
              <div className="grid gap-2">
                <Label htmlFor="new-phone">Novo celular</Label>
                <Input
                  id="new-phone"
                  name="phone"
                  type="tel"
                  inputMode="numeric"
                  autoComplete="off"
                  placeholder="(11) 98765-4321"
                  value={phone}
                  onChange={(event) => {
                    setPhone(formatPhone(event.target.value));
                    setPhoneTouched(true);
                  }}
                />
                {phoneTouched && !phoneValid ? (
                  <p className="text-sm text-negative">
                    Informe um celular com DDD, como (11) 98765-4321.
                  </p>
                ) : null}
              </div>
              {phoneError ? (
                <p role="alert" className="text-sm text-negative">
                  {phoneError}
                </p>
              ) : null}
              {phoneSaved ? (
                <p role="status" className="text-sm text-muted-foreground">
                  Telefone atualizado.
                </p>
              ) : null}
              <Button
                type="submit"
                variant="outline"
                className="justify-self-start"
                disabled={!phoneValid || phoneBusy}
              >
                Trocar celular
              </Button>
            </form>
          </GlassPanel>
        </Reveal>
      ) : null}

      <Reveal delay={0.12}>
        <GlassPanel className="mb-4 p-5">
          <h2 className="mb-2 font-heading text-base font-semibold">Conta</h2>
          {account ? (
            <>
              <dl className="m-0">
                <Row label="Tipo">{accountTypeLabel(account.type)}</Row>
                <Row label="Saldo">{formatCents(account.balance)}</Row>
                <Row label="Cheque especial">{formatCents(account.overdraftLimit)}</Row>
              </dl>
              <Button
                type="button"
                variant="destructive"
                className="mt-4"
                onClick={() => {
                  setCloseError(null);
                  setConfirmingClose(true);
                }}
              >
                Encerrar conta
              </Button>
            </>
          ) : (
            <p className="text-sm text-muted-foreground">
              Você não tem conta aberta. Abra uma na tela Início.
            </p>
          )}
        </GlassPanel>
      </Reveal>

      <Reveal delay={0.18}>
        <GlassPanel className="p-5">
          <h2 className="mb-3 font-heading text-base font-semibold">Sessão</h2>
          <Button type="button" variant="outline" onClick={() => void signOut()}>
            Sair
          </Button>
        </GlassPanel>
      </Reveal>

      <Dialog
        open={confirmingClose}
        onOpenChange={(open) => !closeBusy && setConfirmingClose(open)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Encerrar conta</DialogTitle>
            <DialogDescription>
              Tem certeza? A conta só pode ser encerrada com saldo zero e sem transferências
              pendentes.
            </DialogDescription>
          </DialogHeader>
          {closeError ? (
            <p role="alert" className="text-sm text-negative">
              {closeError}
            </p>
          ) : null}
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              disabled={closeBusy}
              onClick={() => setConfirmingClose(false)}
            >
              Cancelar
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={closeBusy}
              onClick={() => void closeAccount()}
            >
              Sim, encerrar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
