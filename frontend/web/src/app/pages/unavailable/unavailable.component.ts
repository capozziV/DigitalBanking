import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AppFooterComponent } from '../../shared/app-footer.component';

/** Where the guards send a session that could not be loaded. It has no guard, so it cannot loop. */
@Component({
  selector: 'app-unavailable',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AppFooterComponent],
  template: `
    <main class="auth-page">
      <section class="card auth-card">
        <p class="muted">capzzPay</p>
        <h1>Não foi possível carregar</h1>
        <p class="muted" role="alert">
          O serviço está indisponível no momento. Aguarde um instante e tente de novo.
        </p>
        <button class="btn btn-primary" type="button" [disabled]="busy()" (click)="retry()">
          Tentar de novo
        </button>
      </section>
      <app-footer />
    </main>
  `,
})
export class UnavailableComponent {
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  protected readonly busy = signal(false);

  /** The address still is the page that was asked: navigating to it runs its guard again. */
  protected async retry(): Promise<void> {
    this.busy.set(true);
    const path = this.location.path();
    try {
      await this.router.navigateByUrl(path === '/indisponivel' ? '/' : path);
    } finally {
      this.busy.set(false);
    }
  }
}
