import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Discreet signature at the bottom of every page. */
@Component({
  selector: 'app-footer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<footer>© 2026 capzz.labs</footer>`,
  styles: `
    footer {
      padding: 20px 0 8px;
      color: var(--bank-muted);
      font-size: 12px;
      text-align: center;
    }
  `,
})
export class AppFooterComponent {}
