import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { SessionService } from '../../core/session.service';
import { accountTypeLabel } from '../../shared/api';
import { MoneyPipe } from '../../shared/money.pipe';
import { displayPhone, mobilePhone, phoneDigits } from '../../shared/phone';
import { PhoneMaskDirective } from '../../shared/phone-mask.directive';
import { problemMessage } from '../../shared/problem';

@Component({
  selector: 'app-account',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MoneyPipe, PhoneMaskDirective, ReactiveFormsModule],
  template: `
    <h1 class="page-title">Minha conta</h1>
    @if (session.customer(); as customer) {
      <section class="card panel">
        <h2>Seus dados</h2>
        <dl>
          <dt>Nome</dt>
          <dd>{{ customer.name }}</dd>
          <dt>E-mail</dt>
          <dd>{{ customer.email }}</dd>
          <dt>Celular</dt>
          <dd>{{ phoneText(customer.phone) }}</dd>
        </dl>
        <form [formGroup]="phoneForm" (ngSubmit)="changePhone()" novalidate>
          <label class="field">
            <span>Novo celular</span>
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
          @if (phoneError()) {
            <p class="form-error" role="alert">{{ phoneError() }}</p>
          }
          @if (phoneSaved()) {
            <p class="muted" role="status">Telefone atualizado.</p>
          }
          <button
            class="btn btn-outline btn-auto"
            type="submit"
            [disabled]="phoneForm.invalid || phoneBusy()"
          >
            Trocar celular
          </button>
        </form>
      </section>
    }

    <section class="card panel">
      <h2>Conta</h2>
      @if (session.account(); as account) {
        <dl>
          <dt>Tipo</dt>
          <dd>{{ typeLabel(account.type) }}</dd>
          <dt>Saldo</dt>
          <dd>{{ account.balance | money }}</dd>
          <dt>Cheque especial</dt>
          <dd>{{ account.overdraftLimit | money }}</dd>
        </dl>
        @if (closeError()) {
          <p class="form-error" role="alert">{{ closeError() }}</p>
        }
        @if (confirmingClose()) {
          <p>
            Tem certeza? A conta só pode ser encerrada com saldo zero e sem transferências
            pendentes.
          </p>
          <button
            class="btn btn-dark btn-auto"
            type="button"
            [disabled]="closeBusy()"
            (click)="closeAccount()"
          >
            Sim, encerrar
          </button>
          <button
            class="btn btn-outline btn-auto"
            type="button"
            (click)="confirmingClose.set(false)"
          >
            Cancelar
          </button>
        } @else {
          <button class="btn btn-outline btn-auto" type="button" (click)="askToClose()">
            Encerrar conta
          </button>
        }
      } @else {
        <p class="muted">Você não tem conta aberta. Abra uma na tela Início.</p>
      }
    </section>
  `,
  styles: `
    h2 {
      margin: 0 0 8px;
      font-size: 16px;
    }
    dl {
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: 6px 16px;
      margin: 0 0 16px;
      font-size: 14px;
    }
    dt {
      color: var(--bank-muted);
    }
    dd {
      margin: 0;
    }
    .btn + .btn {
      margin-left: 8px;
    }
  `,
})
export class AccountComponent {
  private readonly http = inject(HttpClient);
  protected readonly session = inject(SessionService);

  protected readonly phoneBusy = signal(false);
  protected readonly phoneError = signal<string | null>(null);
  protected readonly phoneSaved = signal(false);
  protected readonly confirmingClose = signal(false);
  protected readonly closeBusy = signal(false);
  protected readonly closeError = signal<string | null>(null);

  protected readonly phoneForm = inject(FormBuilder).nonNullable.group({
    phone: ['', [Validators.required, mobilePhone]],
  });

  protected typeLabel = accountTypeLabel;
  protected phoneText = displayPhone;

  protected async changePhone(): Promise<void> {
    if (this.phoneForm.invalid || this.phoneBusy()) return;
    this.phoneBusy.set(true);
    this.phoneError.set(null);
    this.phoneSaved.set(false);
    try {
      await firstValueFrom(
        this.http.put('/bff/me/phone', { phone: phoneDigits(this.phoneForm.getRawValue().phone) }),
      );
      await this.session.loadMe();
      this.phoneForm.reset();
      this.phoneSaved.set(true);
    } catch (error) {
      this.phoneError.set(problemMessage(error, 'Não foi possível trocar o celular.'));
    } finally {
      this.phoneBusy.set(false);
    }
  }

  protected askToClose(): void {
    this.closeError.set(null);
    this.confirmingClose.set(true);
  }

  protected async closeAccount(): Promise<void> {
    if (this.closeBusy()) return;
    this.closeBusy.set(true);
    this.closeError.set(null);
    try {
      await firstValueFrom(this.http.post('/bff/accounts/close', {}));
      await this.session.loadMe();
      this.confirmingClose.set(false);
    } catch (error) {
      this.closeError.set(problemMessage(error, 'Não foi possível encerrar a conta.'));
    } finally {
      this.closeBusy.set(false);
    }
  }
}
