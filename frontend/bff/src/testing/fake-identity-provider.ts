import {
  IdentityError,
  IdentityProvider,
  type IdentityErrorCode,
  type Tokens,
} from '../identity/identity-provider.js';

export class FakeIdentityProvider extends IdentityProvider {
  readonly calls: string[] = [];
  confirmationRequired = false;
  expiresInSeconds = 3600;
  failRefresh = false;
  refreshError: IdentityErrorCode | Error | null = null;
  /** Lets a test hold a refresh in flight. */
  beforeRefresh: () => Promise<void> = async () => undefined;
  private issued = 0;

  async signUp(email: string): Promise<{ confirmationRequired: boolean }> {
    this.calls.push(`signUp:${email}`);
    return { confirmationRequired: this.confirmationRequired };
  }

  async confirmSignUp(email: string, code: string): Promise<void> {
    this.calls.push(`confirmSignUp:${email}:${code}`);
  }

  async signIn(email: string, password: string): Promise<Tokens> {
    this.calls.push(`signIn:${email}`);
    if (password !== '123456') throw new IdentityError('invalid_credentials');
    return this.issue('access', 'refresh');
  }

  async refresh(refreshToken: string): Promise<Tokens> {
    this.calls.push(`refresh:${refreshToken}`);
    await this.beforeRefresh();
    if (this.refreshError) {
      throw typeof this.refreshError === 'string'
        ? new IdentityError(this.refreshError)
        : this.refreshError;
    }
    if (this.failRefresh) throw new IdentityError('refresh_failed');
    return this.issue('access', refreshToken);
  }

  private issue(access: string, refresh: string): Tokens {
    this.issued += 1;
    return {
      accessToken: `${access}-secret-${this.issued}`,
      refreshToken: refresh,
      expiresInSeconds: this.expiresInSeconds,
    };
  }
}
