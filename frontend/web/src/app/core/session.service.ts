import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import type { AccountSummary } from '../shared/api';

export interface Customer {
  name: string;
  email: string;
  phone: string;
}

export interface Me {
  customer: Customer | null;
  account: AccountSummary | null;
}

interface SessionDTO {
  authenticated: boolean;
  email?: string | null;
}

const UNREAD_POLL_MS = 30_000;

const isSessionGone = (error: unknown): boolean =>
  error instanceof HttpErrorResponse && error.status === 401;

/** Holds who is signed in. Tokens never reach the browser: only the BFF cookie does. */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly authenticated = signal(false);
  readonly email = signal<string | null>(null);
  readonly customer = signal<Customer | null>(null);
  readonly account = signal<AccountSummary | null>(null);
  readonly loaded = signal(false);
  readonly profileLoaded = signal(false);
  readonly unreadCount = signal(0);
  /** The last load failed for a reason other than a lost session (BFF down, network). */
  readonly loadFailed = signal(false);
  /** The last sign-out was not confirmed by the BFF: the session cookie may still work. */
  readonly signOutFailed = signal(false);

  private loading: Promise<void> | null = null;
  private refreshing: Promise<void> | null = null;
  /** Bumped to discard the answer of any unread call started before it. */
  private unreadGeneration = 0;
  private pollTimer: ReturnType<typeof setInterval> | null = null;

  /** Loads the session (and the profile when signed in) once. */
  ensureLoaded(): Promise<void> {
    if (this.loaded()) return Promise.resolve();
    this.loading ??= this.load().finally(() => (this.loading = null));
    return this.loading;
  }

  /** Forces a fresh load of the session and profile. */
  async reload(): Promise<void> {
    this.loaded.set(false);
    await this.ensureLoaded();
  }

  async loadMe(): Promise<void> {
    const me = await firstValueFrom(this.http.get<Me>('/bff/me'));
    this.customer.set(me.customer);
    this.account.set(me.account);
    this.profileLoaded.set(true);
  }

  async signIn(email: string, password: string): Promise<void> {
    await firstValueFrom(this.http.post('/bff/auth/sign-in', { email, password }));
    await this.reload();
  }

  signUp(email: string, password: string): Promise<{ confirmationRequired: boolean }> {
    return firstValueFrom(
      this.http.post<{ confirmationRequired: boolean }>('/bff/auth/sign-up', { email, password }),
    );
  }

  async confirm(email: string, code: string): Promise<void> {
    await firstValueFrom(this.http.post('/bff/auth/confirm', { email, code }));
  }

  /** The cookie is HttpOnly, so only the BFF can end the session: a failure is not a sign-out. */
  async signOut(): Promise<void> {
    this.signOutFailed.set(false);
    try {
      await firstValueFrom(this.http.post('/bff/auth/sign-out', {}));
    } catch (error) {
      if (!isSessionGone(error)) {
        this.signOutFailed.set(true);
        return;
      }
    }
    this.clear();
    await this.router.navigateByUrl('/entrar');
  }

  /** Forgets the local state, as when the BFF says the session is gone. */
  clear(): void {
    this.stopUnreadPolling();
    this.discardUnreadInFlight();
    this.signOutFailed.set(false);
    this.loadFailed.set(false);
    this.authenticated.set(false);
    this.email.set(null);
    this.customer.set(null);
    this.account.set(null);
    this.profileLoaded.set(false);
    this.unreadCount.set(0);
    this.loaded.set(true);
  }

  /**
   * Reads the unread count; a call already in flight is reused, a failure keeps the last number.
   * With `fresh` (after marking as read) a call started earlier is discarded and a new one is made.
   */
  refreshUnread(options?: { fresh?: boolean }): Promise<void> {
    if (options?.fresh) this.discardUnreadInFlight();
    if (this.refreshing === null) {
      const call: Promise<void> = this.fetchUnread(this.unreadGeneration).finally(() => {
        if (this.refreshing === call) this.refreshing = null;
      });
      this.refreshing = call;
    }
    return this.refreshing;
  }

  /** Refreshes the counter every 30 s while the tab is visible, and when it becomes visible. */
  startUnreadPolling(): void {
    if (this.pollTimer !== null) return;
    this.pollTimer = setInterval(() => this.pollIfVisible(), UNREAD_POLL_MS);
    document.addEventListener('visibilitychange', this.onVisibilityChange);
  }

  stopUnreadPolling(): void {
    if (this.pollTimer === null) return;
    clearInterval(this.pollTimer);
    this.pollTimer = null;
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
  }

  private discardUnreadInFlight(): void {
    this.unreadGeneration++;
    this.refreshing = null;
  }

  private async fetchUnread(generation: number): Promise<void> {
    try {
      const result = await firstValueFrom(
        this.http.get<{ count: number }>('/bff/notifications/unread-count'),
      );
      if (generation === this.unreadGeneration) this.unreadCount.set(result.count);
    } catch {
      // Transient failure: keep the last number.
    }
  }

  private pollIfVisible(): void {
    if (document.visibilityState === 'visible') void this.refreshUnread();
  }

  private readonly onVisibilityChange = () => this.pollIfVisible();

  private async load(): Promise<void> {
    try {
      const session = await firstValueFrom(this.http.get<SessionDTO>('/bff/auth/session'));
      if (!session.authenticated) {
        this.clear();
        return;
      }
      this.authenticated.set(true);
      this.email.set(session.email ?? null);
      await this.loadMe();
      this.loaded.set(true);
      this.loadFailed.set(false);
    } catch (error) {
      if (isSessionGone(error)) {
        this.clear();
        return;
      }
      // Transient failure (503, network): keep the state, so it does not look like a sign out.
      this.loadFailed.set(true);
    }
  }
}
