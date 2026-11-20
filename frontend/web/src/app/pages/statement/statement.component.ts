import { HttpClient, HttpParams } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import type { Entry } from '../../shared/api';
import { EntryListComponent } from '../../shared/entry-list.component';
import { LoadErrorComponent } from '../../shared/load-error.component';

interface StatementPageDTO {
  entries: Entry[];
  nextCursor: string | null;
}

@Component({
  selector: 'app-statement',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EntryListComponent, LoadErrorComponent],
  template: `
    <h1 class="page-title">Extrato</h1>
    @if (failed()) {
      <app-load-error (retry)="loadMore()" />
    }
    @if (loaded()) {
      <section class="card panel">
        @if (entries().length > 0) {
          <app-entry-list [entries]="entries()" />
        } @else {
          <p class="muted">Nenhum lançamento ainda.</p>
        }
      </section>
      @if (nextCursor()) {
        <button
          class="btn btn-outline btn-auto"
          type="button"
          [disabled]="busy()"
          (click)="loadMore()"
        >
          Carregar mais
        </button>
      }
    }
  `,
})
export class StatementComponent implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly entries = signal<Entry[]>([]);
  protected readonly nextCursor = signal<string | null>(null);
  protected readonly loaded = signal(false);
  protected readonly failed = signal(false);
  protected readonly busy = signal(false);

  ngOnInit(): void {
    void this.loadMore();
  }

  /** Loads the first page, or the next one by the cursor. */
  protected async loadMore(): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    this.failed.set(false);
    const cursor = this.nextCursor();
    const params = cursor ? new HttpParams().set('cursor', cursor) : undefined;
    try {
      const page = await firstValueFrom(
        this.http.get<StatementPageDTO>('/bff/statement', { params }),
      );
      this.entries.update((current) => [...current, ...page.entries]);
      this.nextCursor.set(page.nextCursor);
      this.loaded.set(true);
    } catch {
      this.failed.set(true);
    } finally {
      this.busy.set(false);
    }
  }
}
