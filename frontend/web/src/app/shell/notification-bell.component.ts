import { HttpClient, HttpParams } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, firstValueFrom } from 'rxjs';
import { SessionService } from '../core/session.service';
import { LoadErrorComponent } from '../shared/load-error.component';
import {
  NotificationDTO,
  NotificationListComponent,
  NotificationPageDTO,
} from '../shared/notifications';

/** Distance from the end of the list, in px, at which the next page is fetched. */
const LOAD_MORE_THRESHOLD = 24;

/** Bell with the unread counter and a panel with the notifications, opened without leaving the page. */
@Component({
  selector: 'app-notification-bell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'escape()',
    '(document:click)': 'clickOutside($event)',
  },
  imports: [LoadErrorComponent, NotificationListComponent],
  template: `
    <button
      #bell
      class="bell"
      type="button"
      aria-haspopup="dialog"
      [attr.aria-expanded]="open()"
      (click)="toggle()"
    >
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        width="18"
        height="18"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
        <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
      </svg>
      <span class="visually-hidden">Notificações</span>
      @if (session.unreadCount() > 0) {
        <span class="bell-badge"
          ><span class="bell-count">{{ session.unreadCount() }}</span
          ><span class="visually-hidden"> não lidas</span></span
        >
      }
    </button>
    @if (open()) {
      <div #panelRef class="panel card" role="dialog" aria-label="Notificações" tabindex="-1">
        @if (failed()) {
          <app-load-error (retry)="load()" />
        }
        @if (loaded()) {
          <div class="scroll" (scroll)="onScroll($event)">
            @if (items().length > 0) {
              <app-notification-list [items]="items()" />
            } @else {
              <p class="muted empty">Nenhuma notificação por enquanto.</p>
            }
          </div>
        }
      </div>
    }
  `,
  styleUrl: './notification-bell.component.scss',
})
export class NotificationBellComponent {
  protected readonly session = inject(SessionService);
  private readonly http = inject(HttpClient);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly bell = viewChild<ElementRef<HTMLButtonElement>>('bell');
  private readonly panelRef = viewChild<ElementRef<HTMLElement>>('panelRef');

  protected readonly open = signal(false);
  protected readonly items = signal<NotificationDTO[]>([]);
  protected readonly loaded = signal(false);
  protected readonly failed = signal(false);

  private nextCursor: string | null = null;
  private fetchingMore = false;
  /** Bumped on every open/close so a response that arrives late is ignored. */
  private generation = 0;

  constructor() {
    inject(Router)
      .events.pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe(() => this.close());
  }

  protected toggle(): void {
    if (this.open()) {
      this.close();
      return;
    }
    this.open.set(true);
    void this.load();
    setTimeout(() => this.panelRef()?.nativeElement.focus());
  }

  protected escape(): void {
    if (!this.open()) return;
    this.close();
    this.bell()?.nativeElement.focus();
  }

  protected clickOutside(event: Event): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) this.close();
  }

  private close(): void {
    this.generation++;
    this.open.set(false);
    this.items.set([]);
    this.loaded.set(false);
    this.failed.set(false);
    this.nextCursor = null;
    this.fetchingMore = false;
  }

  /** Loads the first page, then marks everything shown as read (the rows stay highlighted). */
  protected async load(): Promise<void> {
    const generation = ++this.generation;
    this.failed.set(false);
    try {
      const page = await firstValueFrom(this.http.get<NotificationPageDTO>('/bff/notifications'));
      if (generation !== this.generation) return;
      this.items.set(page.items);
      this.nextCursor = page.nextCursor;
      this.loaded.set(true);
      await this.markRead(page.items, generation);
    } catch {
      if (generation === this.generation) this.failed.set(true);
    }
  }

  protected onScroll(event: Event): void {
    const list = event.target as HTMLElement;
    const remaining = list.scrollHeight - list.clientHeight - list.scrollTop;
    if (remaining <= LOAD_MORE_THRESHOLD) void this.loadMore();
  }

  private async loadMore(): Promise<void> {
    if (!this.nextCursor || this.fetchingMore) return;
    const generation = this.generation;
    this.fetchingMore = true;
    try {
      const page = await firstValueFrom(
        this.http.get<NotificationPageDTO>('/bff/notifications', {
          params: new HttpParams().set('cursor', this.nextCursor),
        }),
      );
      if (generation !== this.generation) return;
      this.items.update((current) => [...current, ...page.items]);
      this.nextCursor = page.nextCursor;
    } catch {
      // The next scroll tries again.
    } finally {
      if (generation === this.generation) this.fetchingMore = false;
    }
  }

  private async markRead(items: NotificationDTO[], generation: number): Promise<void> {
    if (!items.some((item) => !item.readAt)) return;
    try {
      await firstValueFrom(this.http.post('/bff/notifications/read', { upTo: items[0].id }));
      if (generation === this.generation) await this.session.refreshUnread();
    } catch {
      // Not marked: the counter stays and the next open tries again.
    }
  }
}
