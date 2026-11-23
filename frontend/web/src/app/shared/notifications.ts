import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export interface NotificationDTO {
  id: string;
  kind: string;
  title: string;
  body: string;
  createdAt: string;
  readAt: string | null;
}

export interface NotificationPageDTO {
  items: NotificationDTO[];
  unreadCount: number;
  nextCursor: string | null;
}

/** The notification rows, shared by the Notificações page and the bell panel. */
@Component({
  selector: 'app-notification-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  template: `
    <ul>
      @for (item of items(); track item.id) {
        <li [class.unread]="!item.readAt">
          <span class="title">{{ item.title }}</span>
          <span class="body">{{ item.body }}</span>
          <span class="small-label">{{ item.createdAt | date: "dd/MM/yyyy 'às' HH:mm" }}</span>
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
      flex-direction: column;
      gap: 2px;
      padding: 12px 0;
      border-top: 1px solid var(--bank-line);
      color: var(--bank-muted);
    }
    li:first-child {
      border-top: 0;
    }
    .title,
    .body {
      font-size: 14px;
    }
    .unread {
      color: var(--bank-text);
    }
    .unread .title {
      font-weight: 700;
    }
  `,
})
export class NotificationListComponent {
  readonly items = input.required<NotificationDTO[]>();
}
