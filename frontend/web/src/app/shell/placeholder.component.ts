import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Stand-in for the screens of Task 9. */
@Component({
  selector: 'app-placeholder',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<section class="card" style="padding: 24px">
    <p class="muted">Em breve.</p>
  </section>`,
})
export class PlaceholderComponent {}
