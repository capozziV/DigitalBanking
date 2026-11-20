import { loadConfig as load } from './config.js';

const SECRET = { RECIPIENT_TOKEN_SECRET: 'test-recipient-secret-0123456789' };
const loadConfig = (env: Record<string, string>) => load({ ...SECRET, ...env });

describe('loadConfig', () => {
  it('reads the optional MOCK_OAUTH2_HOST and leaves it undefined when empty or absent', () => {
    expect(
      loadConfig({ IDENTITY_PROVIDER: 'local', MOCK_OAUTH2_HOST: 'localhost:8090' }).mockOauth2Host,
    ).toBe('localhost:8090');
    expect(
      loadConfig({ IDENTITY_PROVIDER: 'local', MOCK_OAUTH2_HOST: '' }).mockOauth2Host,
    ).toBeUndefined();
    expect(loadConfig({ IDENTITY_PROVIDER: 'local' }).mockOauth2Host).toBeUndefined();
  });

  it('requires COGNITO_REGION and COGNITO_CLIENT_ID with cognito', () => {
    expect(() => loadConfig({ IDENTITY_PROVIDER: 'cognito', COGNITO_CLIENT_ID: 'c' })).toThrow(
      /COGNITO_REGION/,
    );
  });

  it('requires RECIPIENT_TOKEN_SECRET with at least 16 characters', () => {
    expect(() => load({ IDENTITY_PROVIDER: 'local' })).toThrow(/RECIPIENT_TOKEN_SECRET/);
    expect(() => load({ IDENTITY_PROVIDER: 'local', RECIPIENT_TOKEN_SECRET: 'short' })).toThrow(
      /RECIPIENT_TOKEN_SECRET/,
    );
  });
});
