import { ChangeDetectionStrategy, Component, output } from '@angular/core';

/** Discreet notice for a load that failed (BFF down, network), with a retry button. */
@Component({
  selector: 'app-load-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p class="notice" role="status">
      Não foi possível carregar. Tente de novo.
      <button type="button" (click)="retry.emit()">Tentar de novo</button>
    </p>
  `,
  styles: `
    .notice {
      margin: 0 0 14px;
      color: var(--bank-muted);
      font-size: 13px;
    }
    button {
      margin-left: 8px;
      padding: 0;
      border: 0;
      background: transparent;
      color: var(--bank-text);
      font: inherit;
      font-weight: 600;
      text-decoration: underline;
      cursor: pointer;
    }
  `,
})
export class LoadErrorComponent {
  readonly retry = output<void>();
}
