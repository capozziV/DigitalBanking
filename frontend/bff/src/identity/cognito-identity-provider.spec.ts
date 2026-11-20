import type { CognitoIdentityProviderClient } from '@aws-sdk/client-cognito-identity-provider';
import { CognitoIdentityProvider } from './cognito-identity-provider.js';

function providerFailingWith(name: string): CognitoIdentityProvider {
  const error = Object.assign(new Error(name), { name });
  const client = { send: () => Promise.reject(error) } as unknown as CognitoIdentityProviderClient;
  return new CognitoIdentityProvider('sa-east-1', 'client-id', client);
}

describe('CognitoIdentityProvider.refresh', () => {
  it.each(['NotAuthorizedException', 'UserNotFoundException'])(
    'maps %s to refresh_failed',
    async (name) => {
      await expect(providerFailingWith(name).refresh('rt')).rejects.toMatchObject({
        code: 'refresh_failed',
      });
    },
  );

  it('keeps service errors retryable as unavailable', async () => {
    await expect(providerFailingWith('InternalErrorException').refresh('rt')).rejects.toMatchObject(
      { code: 'unavailable' },
    );
  });

  it('maps a response without access token to refresh_failed', async () => {
    const client = {
      send: () => Promise.resolve({ AuthenticationResult: {} }),
    } as unknown as CognitoIdentityProviderClient;
    const provider = new CognitoIdentityProvider('sa-east-1', 'client-id', client);
    await expect(provider.refresh('rt')).rejects.toMatchObject({ code: 'refresh_failed' });
  });
});
