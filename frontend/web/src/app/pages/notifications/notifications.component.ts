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
import {
  NotificationDTO,
  NotificationListComponent,
  NotificationPageDTO,
} from '../../shared/notifications';
import { problemMessage } from '../../shared/problem';

@Component({
  selector: 'app-notifications',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LoadErrorComponent, NotificationListComponent],
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
          <app-notification-list [items]="items()" />
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
