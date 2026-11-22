import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SessionService } from '../../core/session.service';
import {
  accountTypeLabel,
  type AccountSummary,
  type DailyLimit,
  type Entry,
} from '../../shared/api';
import { EntryListComponent } from '../../shared/entry-list.component';
import { LoadErrorComponent } from '../../shared/load-error.component';
import { MoneyPipe } from '../../shared/money.pipe';
import { problemMessage } from '../../shared/problem';

interface HomeDTO {
  account: AccountSummary | null;
  dailyLimit: DailyLimit | null;
  latestEntries: Entry[];
}

const NO_ACCOUNT: HomeDTO = { account: null, dailyLimit: null, latestEntries: [] };

@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EntryListComponent, LoadErrorComponent, MoneyPipe, RouterLink],
  template: `
    <h1 class="page-title">Início</h1>
    @if (failed()) {
      <app-load-error (retry)="load()" />
    }
    @if (home(); as data) {
      <p class="muted greeting">Olá, {{ firstName() }}</p>
      @if (data.account; as account) {
        <section class="card panel balance-card">
          <span class="small-label">Saldo disponível</span>
          <strong class="balance">{{ account.availableBalance | money }}</strong>
          <span class="small-label sub">
            Conta {{ typeLabel() }} · cheque especial {{ account.overdraftLimit | money }}
          </span>
          <div class="actions">
            <a class="btn btn-dark" routerLink="/transferir">Transferir</a>
            <a class="btn btn-outline" routerLink="/extrato">Extrato</a>
          </div>
        </section>
        @if (data.dailyLimit; as limit) {
          <section class="limit">
            <span class="small-label">
              Limite diário · {{ limit.used | money }} de {{ limit.limit | money }}
            </span>
            <div class="bar" aria-hidden="true">
              <div class="bar-fill" [style.width.%]="usedPercent()"></div>
            </div>
          </section>
        }
        <section class="card panel">
          <h2>Últimos lançamentos</h2>
          @if (data.latestEntries.length > 0) {
            <app-entry-list [entries]="data.latestEntries" />
          } @else {
            <p class="muted">Nenhum lançamento ainda.</p>
          }
        </section>
      } @else {
        <section class="card panel">
          <h2>Você ainda não tem conta</h2>
          <p class="muted">
            Abra uma conta para transferir. O bônus de boas-vindas vale só para a primeira conta.
          </p>
          @if (error()) {
            <p class="form-error" role="alert">{{ error() }}</p>
          }
          <button
            class="btn btn-primary btn-auto"
            type="button"
            [disabled]="busy()"
            (click)="openAccount()"
          >
            Abrir conta
          </button>
        </section>
      }
    }
  `,
  styles: `
    .greeting {
      margin: 0 0 12px;
      font-size: 13px;
    }
    .balance-card .balance {
      display: block;
      margin: 4px 0 2px;
      font-size: 30px;
      letter-spacing: -1px;
    }
    .sub {
      margin-bottom: 14px;
    }
    .actions {
      display: flex;
      gap: 8px;
    }
    .actions .btn {
      flex: 1;
      padding: 10px;
      font-size: 14px;
    }
    .limit {
      margin-bottom: 14px;
    }
    .bar {
      height: 4px;
      margin-top: 6px;
      border-radius: 4px;
      background: var(--bank-line);
    }
    .bar-fill {
      height: 4px;
      border-radius: 4px;
      background: var(--bank-text);
    }
    h2 {
      margin: 0 0 8px;
      font-size: 16px;
    }
  `,
})
export class HomeComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);

  protected readonly home = signal<HomeDTO | null>(null);
  protected readonly failed = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly firstName = computed(
    () => this.session.customer()?.name.trim().split(/\s+/)[0] ?? '',
  );
  protected readonly typeLabel = computed(() => accountTypeLabel(this.home()?.account?.type ?? ''));
  protected readonly usedPercent = computed(() => {
    const limit = this.home()?.dailyLimit;
    if (!limit || limit.limit <= 0) return 0;
    return Math.min(100, Math.round((limit.used / limit.limit) * 100));
  });

  ngOnInit(): void {
    void this.load();
  }

  protected async load(): Promise<void> {
    this.failed.set(false);
    if (!this.session.account()) {
      this.home.set(NO_ACCOUNT);
      return;
    }
    try {
      this.home.set(await firstValueFrom(this.http.get<HomeDTO>('/bff/home')));
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 404) this.home.set(NO_ACCOUNT);
      else this.failed.set(true);
    }
  }

  protected async openAccount(): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.http.post('/bff/accounts', {}));
      await this.session.loadMe();
      await this.load();
    } catch (error) {
      this.error.set(problemMessage(error, 'Não foi possível abrir a conta.'));
    } finally {
      this.busy.set(false);
    }
  }
}
