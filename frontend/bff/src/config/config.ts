export const APP_CONFIG = Symbol('APP_CONFIG');

export type IdentityProviderKind = 'cognito' | 'local';
export type BackendService = 'accounts' | 'transfers' | 'customer' | 'statement' | 'notification';

export interface AppConfig {
  identityProvider: IdentityProviderKind;
  cognitoRegion: string;
  cognitoClientId: string;
  redisUrl: string;
  cookieSecure: boolean;
  mockOauth2Url: string;
  mockOauth2Host?: string;
  services: Record<BackendService, string>;
  /** HMAC key of the recipient tokens (RECIPIENT_TOKEN_SECRET); never versioned. */
  recipientTokenSecret: string;
  logLevel: string;
}

type Env = Record<string, string | undefined>;

export function loadConfig(env: Env): AppConfig {
  const kind = env['IDENTITY_PROVIDER'];
  if (kind !== 'cognito' && kind !== 'local') {
    throw new Error('IDENTITY_PROVIDER must be "cognito" or "local"');
  }
  const cognitoRegion = env['COGNITO_REGION'] ?? '';
  const cognitoClientId = env['COGNITO_CLIENT_ID'] ?? '';
  if (kind === 'cognito' && (!cognitoRegion || !cognitoClientId)) {
    throw new Error(
      'COGNITO_REGION and COGNITO_CLIENT_ID are required with IDENTITY_PROVIDER=cognito',
    );
  }
  const recipientTokenSecret = env['RECIPIENT_TOKEN_SECRET'] ?? '';
  if (recipientTokenSecret.length < 16) {
    throw new Error('RECIPIENT_TOKEN_SECRET is required and must have at least 16 characters');
  }
  return {
    identityProvider: kind,
    cognitoRegion,
    cognitoClientId,
    redisUrl: env['REDIS_URL'] ?? 'redis://localhost:6379',
    cookieSecure: env['COOKIE_SECURE'] !== 'false',
    mockOauth2Url: env['MOCK_OAUTH2_URL'] ?? 'http://localhost:8090/default',
    mockOauth2Host: env['MOCK_OAUTH2_HOST'] || undefined,
    services: {
      accounts: env['ACCOUNTS_URL'] ?? 'http://localhost:8081',
      transfers: env['TRANSFERS_URL'] ?? 'http://localhost:8082',
      notification: env['NOTIFICATION_URL'] ?? 'http://localhost:8083',
      statement: env['STATEMENT_URL'] ?? 'http://localhost:8084',
      customer: env['CUSTOMER_URL'] ?? 'http://localhost:8085',
    },
    recipientTokenSecret,
    logLevel: env['LOG_LEVEL'] ?? 'info',
  };
}
