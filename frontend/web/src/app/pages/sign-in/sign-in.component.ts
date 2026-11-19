import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SessionService } from '../../core/session.service';
import { sixDigitPassword } from '../../shared/password';
import { problemMessage } from '../../shared/problem';

@Component({
  selector: 'app-sign-in',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-page">
      <section class="card auth-card">
        <p class="muted">Banco Digital</p>
        <h1>Entrar</h1>
        @if (error()) {
          <p class="form-error" role="alert">{{ error() }}</p>
        }
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label class="field">
            <span>E-mail</span>
            <input name="email" type="email" autocomplete="email" formControlName="email" />
          </label>
          <label class="field">
            <span>Senha (6 dígitos)</span>
            <input
              name="password"
              type="password"
              inputmode="numeric"
              maxlength="6"
              autocomplete="current-password"
              formControlName="password"
            />
            @if (form.controls.password.dirty && form.controls.password.invalid) {
              <p class="field-error">A senha tem exatamente 6 números.</p>
            }
          </label>
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid || busy()">
            Entrar
          </button>
        </form>
        <p class="muted" style="margin-top: 20px">
          Ainda não tem conta? <a class="link" routerLink="/criar-conta">Criar conta</a>
        </p>
      </section>
    </main>
  `,
})
export class SignInComponent {
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [sixDigitPassword]],
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    try {
      await this.session.signIn(email, password);
      await this.router.navigateByUrl('/inicio');
    } catch (error) {
      this.error.set(
        error instanceof HttpErrorResponse && error.status === 401
          ? 'E-mail ou senha incorretos.'
          : problemMessage(error, 'Não foi possível entrar agora. Tente de novo.'),
      );
    } finally {
      this.busy.set(false);
    }
  }
}
