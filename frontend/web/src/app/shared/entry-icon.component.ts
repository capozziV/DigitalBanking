import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type EntryKind = 'CREDIT' | 'DEBIT';

/** Credit: filled black circle with a down arrow. Debit: hollow circle with an up arrow. */
@Component({
  selector: 'app-entry-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span
    class="icon"
    [class.filled]="credit()"
    [class.hollow]="!credit()"
    role="img"
    [attr.aria-label]="credit() ? 'Entrada' : 'Saída'"
    >{{ credit() ? '↓' : '↑' }}</span
  >`,
  styles: `
    .icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      box-sizing: border-box;
      width: 30px;
      height: 30px;
      border-radius: 50%;
      font-size: 14px;
    }
    .filled {
      background: #1d1d1f;
      color: #fff;
    }
    .hollow {
      border: 1px solid #d2d2d7;
      color: #1d1d1f;
    }
  `,
})
export class EntryIconComponent {
  readonly kind = input.required<EntryKind>();
  protected readonly credit = computed(() => this.kind() === 'CREDIT');
}
