import { loadConfig as load } from './config.js';

const loadConfig = (env: Record<string, string>) => load(env);

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
});
