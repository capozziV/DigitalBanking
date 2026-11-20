import { AppFooterComponent } from '../../shared/app-footer.component';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SessionService } from '../../core/session.service';
import { mobilePhone, phoneDigits } from '../../shared/phone';
import { PhoneMaskDirective } from '../../shared/phone-mask.directive';
import { problemMessage } from '../../shared/problem';

@Component({
  selector: 'app-complete-profile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AppFooterComponent, ReactiveFormsModule, PhoneMaskDirective],
  template: `
    <main class="auth-page">
      <section class="card auth-card">
        <p class="muted">capzzPay</p>
        <h1>Completar cadastro</h1>
        <p class="muted">
          Diga seu nome completo e seu celular. É pelo celular que outros clientes enviam
          transferências para você.
        </p>
        @if (error()) {
          <p class="form-error" role="alert">{{ error() }}</p>
        }
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label class="field">
            <span>Nome completo</span>
            <input name="name" type="text" autocomplete="name" formControlName="name" />
          </label>
          <label class="field">
            <span>Celular</span>
            <input
              name="phone"
              type="tel"
              inputmode="numeric"
              autocomplete="tel-national"
              placeholder="(11) 98765-4321"
              appPhoneMask
              formControlName="phone"
            />
            @if (form.controls.phone.dirty && form.controls.phone.invalid) {
              <p class="field-error">Informe um celular com DDD, como (11) 98765-4321.</p>
            }
          </label>
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid || busy()">
            Concluir
          </button>
        </form>
      </section>
      <app-footer />
    </main>
  `,
})
export class CompleteProfileComponent {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.pattern(/\S{2,}/), Validators.maxLength(120)]],
    phone: ['', [mobilePhone]],
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    const { name, phone } = this.form.getRawValue();
    try {
      await firstValueFrom(
        this.http.post('/bff/customers', { name: name.trim(), phone: phoneDigits(phone) }),
      );
      await this.session.loadMe();
      await this.router.navigateByUrl('/inicio');
    } catch (error) {
      this.error.set(problemMessage(error, 'Não foi possível concluir o cadastro.'));
    } finally {
      this.busy.set(false);
    }
  }
}
