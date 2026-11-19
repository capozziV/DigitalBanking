import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

export interface Customer {
  name: string;
  email: string;
  phone: string;
}

export interface Me {
  customer: Customer | null;
  account: unknown | null;
}

interface SessionDTO {
  authenticated: boolean;
  email?: string | null;
}

/** Holds who is signed in. Tokens never reach the browser: only the BFF cookie does. */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly authenticated = signal(false);
  readonly email = signal<string | null>(null);
  readonly customer = signal<Customer | null>(null);
  readonly account = signal<unknown | null>(null);
  readonly loaded = signal(false);
  readonly profileLoaded = signal(false);
  readonly unreadCount = signal(0);
  /** The last load failed for a reason other than a lost session (BFF down, network). */
  readonly loadFailed = signal(false);

  private loading: Promise<void> | null = null;

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

  async signOut(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/bff/auth/sign-out', {}));
    } catch {
      // The local state is cleared either way.
    }
    this.clear();
    await this.router.navigateByUrl('/entrar');
  }

  /** Forgets the local state, as when the BFF says the session is gone. */
  clear(): void {
    this.authenticated.set(false);
    this.email.set(null);
    this.customer.set(null);
    this.account.set(null);
    this.profileLoaded.set(false);
    this.unreadCount.set(0);
    this.loaded.set(true);
  }

  async refreshUnread(): Promise<void> {
    try {
      const result = await firstValueFrom(
        this.http.get<{ count: number }>('/bff/notifications/unread-count'),
      );
      this.unreadCount.set(result.count);
    } catch {
      this.unreadCount.set(0);
    }
  }

  private async load(): Promise<void> {
    try {
      const session = await firstValueFrom(this.http.get<SessionDTO>('/bff/auth/session'));
      if (!session.authenticated) {
        this.loadFailed.set(false);
        this.clear();
        return;
      }
      this.authenticated.set(true);
      this.email.set(session.email ?? null);
      await this.loadMe();
      this.loaded.set(true);
      this.loadFailed.set(false);
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        this.loadFailed.set(false);
        this.clear();
        return;
      }
      // Transient failure (503, network): keep the state, so it does not look like a sign out.
      this.loadFailed.set(true);
    }
  }
}
