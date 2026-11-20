import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import type { Entry } from './api';
import { EntryIconComponent } from './entry-icon.component';
import { MoneyPipe } from './money.pipe';

/** Credit: ↓ in a filled circle, bold amount. Debit: ↑ in a hollow circle, grey amount. */
@Component({
  selector: 'app-entry-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, EntryIconComponent, MoneyPipe],
  template: `
    <ul>
      @for (entry of entries(); track entry.id) {
        <li>
          <app-entry-icon [kind]="entry.kind" />
          <span class="text">
            <span class="description">{{
              entry.description ?? (entry.kind === 'CREDIT' ? 'Crédito' : 'Débito')
            }}</span>
            <span class="date">{{ entry.createdAt | date: "dd/MM/yyyy 'às' HH:mm" }}</span>
          </span>
          <span
            class="amount"
            [class.in]="entry.kind === 'CREDIT'"
            [class.out]="entry.kind === 'DEBIT'"
          >
            {{ entry.kind === 'CREDIT' ? '+' : '−' }} {{ entry.amount | money }}
          </span>
        </li>
      }
    </ul>
  `,
  styles: `
    ul {
      margin: 0;
      padding: 0;
      list-style: none;
    }
    li {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 0;
      border-top: 1px solid var(--bank-line);
    }
    li:first-child {
      border-top: 0;
    }
    .text {
      display: flex;
      flex: 1;
      flex-direction: column;
      min-width: 0;
    }
    .description {
      font-size: 14px;
    }
    .date {
      color: var(--bank-muted);
      font-size: 12px;
    }
    .amount {
      font-size: 14px;
      white-space: nowrap;
    }
    .in {
      font-weight: 700;
    }
    .out {
      color: var(--bank-muted);
    }
  `,
})
export class EntryListComponent {
  readonly entries = input.required<Entry[]>();
}
