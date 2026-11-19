import {
  CognitoIdentityProviderClient,
  ConfirmSignUpCommand,
  InitiateAuthCommand,
  SignUpCommand,
} from '@aws-sdk/client-cognito-identity-provider';
import {
  IdentityError,
  IdentityProvider,
  type IdentityErrorCode,
  type Tokens,
} from './identity-provider.js';

const ERROR_CODES: Record<string, IdentityErrorCode> = {
  UsernameExistsException: 'user_exists',
  CodeMismatchException: 'invalid_code',
  ExpiredCodeException: 'invalid_code',
  NotAuthorizedException: 'invalid_credentials',
  UserNotFoundException: 'invalid_credentials',
  UserNotConfirmedException: 'not_confirmed',
};

function translate(error: unknown): IdentityError {
  if (error instanceof IdentityError) return error;
  const name = error instanceof Error ? error.name : '';
  return new IdentityError(ERROR_CODES[name] ?? 'unavailable');
}

export class CognitoIdentityProvider extends IdentityProvider {
  private readonly client: CognitoIdentityProviderClient;

  constructor(
    region: string,
    private readonly clientId: string,
    client?: CognitoIdentityProviderClient,
  ) {
    super();
    this.client = client ?? new CognitoIdentityProviderClient({ region });
  }

  async signUp(email: string, password: string): Promise<{ confirmationRequired: boolean }> {
    try {
      const result = await this.client.send(
        new SignUpCommand({
          ClientId: this.clientId,
          Username: email,
          Password: password,
          UserAttributes: [{ Name: 'email', Value: email }],
        }),
      );
      return { confirmationRequired: !result.UserConfirmed };
    } catch (error) {
      throw translate(error);
    }
  }

  async confirmSignUp(email: string, code: string): Promise<void> {
    try {
      await this.client.send(
        new ConfirmSignUpCommand({
          ClientId: this.clientId,
          Username: email,
          ConfirmationCode: code,
        }),
      );
    } catch (error) {
      throw translate(error);
    }
  }

  async signIn(email: string, password: string): Promise<Tokens> {
    try {
      const result = await this.client.send(
        new InitiateAuthCommand({
          ClientId: this.clientId,
          AuthFlow: 'USER_PASSWORD_AUTH',
          AuthParameters: { USERNAME: email, PASSWORD: password },
        }),
      );
      const auth = result.AuthenticationResult;
      if (!auth?.AccessToken || !auth.RefreshToken) throw new IdentityError('invalid_credentials');
      return {
        accessToken: auth.AccessToken,
        refreshToken: auth.RefreshToken,
        expiresInSeconds: auth.ExpiresIn ?? 3600,
      };
    } catch (error) {
      throw translate(error);
    }
  }

  async refresh(refreshToken: string): Promise<Tokens> {
    try {
      const result = await this.client.send(
        new InitiateAuthCommand({
          ClientId: this.clientId,
          AuthFlow: 'REFRESH_TOKEN_AUTH',
          AuthParameters: { REFRESH_TOKEN: refreshToken },
        }),
      );
      const auth = result.AuthenticationResult;
      if (!auth?.AccessToken) throw new IdentityError('refresh_failed');
      // Cognito does not rotate the refresh token on REFRESH_TOKEN_AUTH.
      return {
        accessToken: auth.AccessToken,
        refreshToken: auth.RefreshToken ?? refreshToken,
        expiresInSeconds: auth.ExpiresIn ?? 3600,
      };
    } catch (error) {
      if (error instanceof IdentityError) throw error;
      // Only a refused token ends the session; network and service errors are retryable.
      throw new IdentityError(
        error instanceof Error && error.name === 'NotAuthorizedException'
          ? 'refresh_failed'
          : 'unavailable',
      );
    }
  }
}
