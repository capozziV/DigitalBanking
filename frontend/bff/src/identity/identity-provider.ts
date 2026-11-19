export interface Tokens {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
}

export type IdentityErrorCode =
  | 'invalid_credentials'
  | 'user_exists'
  | 'invalid_code'
  | 'not_confirmed'
  | 'refresh_failed'
  | 'unavailable';

export class IdentityError extends Error {
  constructor(readonly code: IdentityErrorCode) {
    super(code);
    this.name = 'IdentityError';
  }
}

/** Port to the identity service: Cognito on AWS, a local adapter over the mock-oauth2-server. */
export abstract class IdentityProvider {
  abstract signUp(email: string, password: string): Promise<{ confirmationRequired: boolean }>;
  abstract confirmSignUp(email: string, code: string): Promise<void>;
  abstract signIn(email: string, password: string): Promise<Tokens>;
  abstract refresh(refreshToken: string): Promise<Tokens>;
}
