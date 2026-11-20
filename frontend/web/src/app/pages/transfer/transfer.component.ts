import { HttpClient, HttpParams } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  EMPTY,
  firstValueFrom,
  Subscription,
  catchError,
  switchMap,
  takeUntil,
  takeWhile,
  tap,
  timer,
} from 'rxjs';
import { parseAmountToCents } from '../../shared/amount';
import { MoneyPipe } from '../../shared/money.pipe';
import { mobilePhone, phoneDigits } from '../../shared/phone';
import { PhoneMaskDirective } from '../../shared/phone-mask.directive';
import { problemMessage } from '../../shared/problem';

type Step = 'phone' | 'amount' | 'confirm' | 'status';

interface RecipientDTO {
  recipientName: string;
  recipientToken: string;
}

interface TransferStatusDTO {
  id: string;
  status: string;
  reason?: string | null;
}

const POLL_EVERY_MS = 1000;
const POLL_DEADLINE_MS = 30_000;
const FINAL_STATUSES = ['COMPLETED', 'FAILED', 'COMPENSATED'];

const STATUS_TITLES: Record<string, string> = {
  COMPLETED: 'Concluída',
  FAILED: 'Recusada',
  COMPENSATED: 'Devolvida',
};

@Component({
  selector: 'app-transfer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MoneyPipe, PhoneMaskDirective, ReactiveFormsModule, RouterLink],
  template: `
    <h1 class="page-title">Transferir</h1>
    <section class="card panel">
      @if (error()) {
        <p class="form-error" role="alert">{{ error() }}</p>
      }
      @switch (step()) {
        @case ('phone') {
          <p class="muted">Para quem você quer enviar? Digite o celular do destinatário.</p>
          <form [formGroup]="phoneForm" (ngSubmit)="lookUp()" novalidate>
            <label class="field">
              <span>Celular do destinatário</span>
              <input
                name="phone"
                type="tel"
                inputmode="numeric"
                autocomplete="off"
                placeholder="(11) 98765-4321"
                appPhoneMask
                formControlName="phone"
              />
              @if (phoneForm.controls.phone.dirty && phoneForm.controls.phone.invalid) {
                <p class="field-error">Informe um celular com DDD, como (11) 98765-4321.</p>
              }
            </label>
            <button
              class="btn btn-primary btn-auto"
              type="submit"
              [disabled]="phoneForm.invalid || busy()"
            >
              Próximo
            </button>
          </form>
        }
        @case ('amount') {
          <span class="small-label">Destinatário</span>
          <strong class="recipient">{{ recipientName() }}</strong>
          <form [formGroup]="amountForm" (ngSubmit)="chooseAmount()" novalidate>
            <label class="field">
              <span>Valor (R$)</span>
              <input
                name="amount"
                type="text"
                inputmode="decimal"
                autocomplete="off"
                placeholder="0,00"
                formControlName="amount"
              />
              @if (amountInvalid()) {
                <p class="field-error">Informe um valor maior que zero, como 25,00.</p>
              }
            </label>
            <button class="btn btn-outline btn-auto" type="button" (click)="back()">Voltar</button>
            <button class="btn btn-primary btn-auto" type="submit">Continuar</button>
          </form>
        }
        @case ('confirm') {
          <span class="small-label">Você vai enviar</span>
          <strong class="value">{{ amountCents() | money }}</strong>
          <span class="small-label">para</span>
          <strong class="recipient">{{ recipientName() }}</strong>
          <div class="row">
            <button class="btn btn-outline btn-auto" type="button" (click)="back()">Voltar</button>
            <button
              class="btn btn-primary btn-auto"
              type="button"
              [disabled]="busy()"
              (click)="confirm()"
            >
              Confirmar
            </button>
          </div>
        }
        @case ('status') {
          @let current = result();
          <span class="small-label">Transferência para {{ recipientName() }}</span>
          <strong class="value">{{ amountCents() | money }}</strong>
          <p class="status" role="status">
            @if (timedOut()) {
              Ainda processando. Veja o resultado no extrato em instantes.
            } @else if (isFinal(current.status)) {
              <strong>{{ statusTitle(current.status) }}</strong>
              @if (current.reason) {
                <span class="muted"> — {{ current.reason }}</span>
              }
            } @else {
              Processando…
            }
          </p>
          @if (timedOut() || isFinal(current.status)) {
            <div class="row">
              <button class="btn btn-primary btn-auto" type="button" (click)="reset()">
                Nova transferência
              </button>
              <a class="btn btn-outline btn-auto" routerLink="/extrato">Ver extrato</a>
            </div>
          }
        }
      }
    </section>
  `,
  styles: `
    .recipient,
    .value {
      display: block;
      margin-bottom: 14px;
      font-size: 20px;
    }
    .value {
      font-size: 28px;
      letter-spacing: -1px;
    }
    .row {
      display: flex;
      gap: 8px;
      margin-top: 8px;
    }
    form .btn {
      margin-right: 8px;
    }
    .status {
      margin: 0 0 16px;
    }
  `,
})
export class TransferComponent {
  private readonly http = inject(HttpClient);

  protected readonly step = signal<Step>('phone');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly recipientName = signal('');
  protected readonly amountCents = signal(0);
  protected readonly amountInvalid = signal(false);
  protected readonly result = signal<TransferStatusDTO>({ id: '', status: 'PENDING' });
  protected readonly timedOut = signal(false);

  protected readonly phoneForm = inject(FormBuilder).nonNullable.group({
    phone: ['', [Validators.required, mobilePhone]],
  });
  protected readonly amountForm = inject(FormBuilder).nonNullable.group({ amount: [''] });

  private recipientToken = '';
  /** Born when the form opens and kept on every retry and double click of "Confirmar". */
  private idempotencyKey = crypto.randomUUID();
  private polling: Subscription | null = null;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.polling?.unsubscribe());
  }

  protected isFinal(status: string): boolean {
    return FINAL_STATUSES.includes(status);
  }

  protected statusTitle(status: string): string {
    return STATUS_TITLES[status] ?? status;
  }

  protected async lookUp(): Promise<void> {
    if (this.phoneForm.invalid || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    const phone = phoneDigits(this.phoneForm.getRawValue().phone);
    try {
      const recipient = await firstValueFrom(
        this.http.get<RecipientDTO>('/bff/transfers/recipient', {
          params: new HttpParams().set('phone', phone),
        }),
      );
      this.recipientName.set(recipient.recipientName);
      this.recipientToken = recipient.recipientToken;
      this.step.set('amount');
    } catch (error) {
      this.error.set(problemMessage(error, 'Não foi possível encontrar o destinatário.'));
    } finally {
      this.busy.set(false);
    }
  }

  protected chooseAmount(): void {
    const cents = parseAmountToCents(this.amountForm.getRawValue().amount);
    this.amountInvalid.set(cents === null);
    if (cents === null) return;
    this.amountCents.set(cents);
    this.error.set(null);
    this.step.set('confirm');
  }

  protected back(): void {
    this.error.set(null);
    this.step.set(this.step() === 'confirm' ? 'amount' : 'phone');
  }

  protected async confirm(): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      const created = await firstValueFrom(
        this.http.post<TransferStatusDTO>(
          '/bff/transfers',
          { recipientToken: this.recipientToken, amount: this.amountCents() },
          { headers: { 'Idempotency-Key': this.idempotencyKey } },
        ),
      );
      this.step.set('status');
      this.startPolling(created);
    } catch (error) {
      this.error.set(problemMessage(error, 'Não foi possível enviar a transferência.'));
    } finally {
      this.busy.set(false);
    }
  }

  protected reset(): void {
    this.polling?.unsubscribe();
    this.idempotencyKey = crypto.randomUUID();
    this.recipientToken = '';
    this.phoneForm.reset();
    this.amountForm.reset();
    this.amountInvalid.set(false);
    this.timedOut.set(false);
    this.error.set(null);
    this.step.set('phone');
  }

  /** Asks for the status every second until it is final, for at most 30 s. */
  private startPolling(created: TransferStatusDTO): void {
    this.polling?.unsubscribe();
    this.timedOut.set(false);
    this.result.set(created);
    const deadline = timer(POLL_DEADLINE_MS).pipe(tap(() => this.timedOut.set(true)));
    this.polling = timer(POLL_EVERY_MS, POLL_EVERY_MS)
      .pipe(
        switchMap(() =>
          this.http
            .get<TransferStatusDTO>(`/bff/transfers/${created.id}`)
            .pipe(catchError(() => EMPTY)),
        ),
        takeWhile((transfer) => !this.isFinal(transfer.status), true),
        takeUntil(deadline),
      )
      .subscribe((transfer) => this.result.set(transfer));
  }
}
