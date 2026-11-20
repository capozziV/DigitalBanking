import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { SessionService } from '../../core/session.service';
import { LoadErrorComponent } from '../../shared/load-error.component';
import { problemMessage } from '../../shared/problem';

interface NotificationDTO {
  id: string;
  kind: string;
  title: string;
  body: string;
  createdAt: string;
  readAt: string | null;
}

interface NotificationPageDTO {
  items: NotificationDTO[];
  unreadCount: number;
  nextCursor: string | null;
}

@Component({
  selector: 'app-notifications',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, LoadErrorComponent],
  template: `
    <h1 class="page-title">Notificações</h1>
    @if (failed()) {
      <app-load-error (retry)="load()" />
    }
    @if (error()) {
      <p class="form-error" role="alert">{{ error() }}</p>
    }
    @if (loaded()) {
      @if (hasUnread()) {
        <button
          class="btn btn-outline btn-auto mark"
          type="button"
          [disabled]="busy()"
          (click)="markAllRead()"
        >
          Marcar como lidas
        </button>
      }
      <section class="card panel">
        @if (items().length > 0) {
          <ul>
            @for (item of items(); track item.id) {
              <li [class.unread]="!item.readAt">
                <span class="title">{{ item.title }}</span>
                <span class="body">{{ item.body }}</span>
                <span class="small-label">{{
                  item.createdAt | date: "dd/MM/yyyy 'às' HH:mm"
                }}</span>
              </li>
            }
          </ul>
        } @else {
          <p class="muted">Nenhuma notificação por enquanto.</p>
        }
      </section>
    }
  `,
  styles: `
    .mark {
      margin-bottom: 14px;
    }
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
export class NotificationsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);

  protected readonly items = signal<NotificationDTO[]>([]);
  protected readonly loaded = signal(false);
  protected readonly failed = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly hasUnread = computed(() => this.items().some((item) => !item.readAt));

  ngOnInit(): void {
    void this.load();
  }

  protected async load(): Promise<void> {
    this.failed.set(false);
    try {
      const page = await firstValueFrom(this.http.get<NotificationPageDTO>('/bff/notifications'));
      this.items.set(page.items);
      this.loaded.set(true);
    } catch {
      this.failed.set(true);
    }
  }

  /** Marks as read everything up to the newest notification (first in the list, by id). */
  protected async markAllRead(): Promise<void> {
    const newest = this.items()[0]?.id;
    if (!newest || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.http.post('/bff/notifications/read', { upTo: newest }));
      await this.load();
      await this.session.refreshUnread();
    } catch (error) {
      this.error.set(problemMessage(error, 'Não foi possível marcar como lidas.'));
    } finally {
      this.busy.set(false);
    }
  }
}
