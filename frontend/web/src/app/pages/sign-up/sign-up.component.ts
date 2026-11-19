import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SessionService } from '../../core/session.service';
import { passwordsMatch, sixDigitPassword } from '../../shared/password';
import { problemMessage } from '../../shared/problem';

@Component({
  selector: 'app-sign-up',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-page">
      <section class="card auth-card">
        <p class="muted">Banco Digital</p>
        <h1>Criar conta</h1>
        @if (error()) {
          <p class="form-error" role="alert">{{ error() }}</p>
        }
        @if (step() === 'details') {
          <form [formGroup]="form" (ngSubmit)="submitDetails()" novalidate>
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
                autocomplete="new-password"
                formControlName="password"
              />
              @if (form.controls.password.dirty && form.controls.password.invalid) {
                <p class="field-error">A senha tem exatamente 6 números.</p>
              }
            </label>
            <label class="field">
              <span>Confirme a senha</span>
              <input
                name="confirmation"
                type="password"
                inputmode="numeric"
                maxlength="6"
                autocomplete="new-password"
                formControlName="confirmation"
              />
              @if (form.controls.confirmation.dirty && form.hasError('mismatch')) {
                <p class="field-error">As senhas não conferem.</p>
              }
            </label>
            <button class="btn btn-primary" type="submit" [disabled]="form.invalid || busy()">
              Continuar
            </button>
          </form>
        } @else {
          <form [formGroup]="codeForm" (ngSubmit)="submitCode()" novalidate>
            <p class="muted">Enviamos um código para {{ form.controls.email.value }}.</p>
            <label class="field">
              <span>Código do e-mail</span>
              <input
                name="code"
                type="text"
                inputmode="numeric"
                autocomplete="one-time-code"
                formControlName="code"
              />
            </label>
            <button class="btn btn-primary" type="submit" [disabled]="codeForm.invalid || busy()">
              Confirmar e entrar
            </button>
          </form>
        }
        <p class="muted" style="margin-top: 20px">
          Já tem conta? <a class="link" routerLink="/entrar">Entrar</a>
        </p>
      </section>
    </main>
  `,
})
export class SignUpComponent {
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly builder = inject(FormBuilder).nonNullable;

  protected readonly step = signal<'details' | 'code'>('details');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.builder.group(
    {
      email: ['', [Validators.required, Validators.email]],
      password: ['', [sixDigitPassword]],
      confirmation: [''],
    },
    { validators: [passwordsMatch] },
  );

  protected readonly codeForm = this.builder.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  protected async submitDetails(): Promise<void> {
    if (this.form.invalid || this.busy()) return;
    const { email, password } = this.form.getRawValue();
    await this.run(async () => {
      const { confirmationRequired } = await this.session.signUp(email, password);
      if (confirmationRequired) {
        this.step.set('code');
        return;
      }
      await this.finish(email, password);
    }, 'Não foi possível criar a conta agora.');
  }

  protected async submitCode(): Promise<void> {
    if (this.codeForm.invalid || this.busy()) return;
    const { email, password } = this.form.getRawValue();
    const { code } = this.codeForm.getRawValue();
    await this.run(async () => {
      await this.session.confirm(email, code);
      await this.finish(email, password);
    }, 'Não foi possível confirmar o código.');
  }

  private async finish(email: string, password: string): Promise<void> {
    await this.session.signIn(email, password);
    await this.router.navigateByUrl('/inicio');
  }

  private async run(action: () => Promise<void>, fallback: string): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
    } catch (error) {
      this.error.set(problemMessage(error, fallback));
    } finally {
      this.busy.set(false);
    }
  }
}
