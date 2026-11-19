import { Module } from '@nestjs/common';
import { APP_FILTER } from '@nestjs/core';
import { AuthController } from './auth/auth.controller.js';
import { BackendErrorFilter } from './backend/backend-error.filter.js';
import { BackendClient, FETCH } from './backend/backend-client.js';
import { APP_CONFIG, loadConfig, type AppConfig } from './config/config.js';
import { HealthController } from './health.controller.js';
import { CognitoIdentityProvider } from './identity/cognito-identity-provider.js';
import { IdentityProvider } from './identity/identity-provider.js';
import { LocalIdentityProvider } from './identity/local-identity-provider.js';
import { LOGGER, createLogger } from './observability/logger.js';
import { KEY_VALUE_STORE, type KeyValueStore } from './redis/key-value-store.js';
import { RedisKeyValueStore } from './redis/redis-key-value-store.js';
import { SessionGuard } from './session/session.guard.js';
import { CLOCK, SessionService } from './session/session.service.js';

@Module({
  controllers: [HealthController, AuthController],
  providers: [
    { provide: APP_CONFIG, useFactory: () => loadConfig(process.env) },
    {
      provide: LOGGER,
      inject: [APP_CONFIG],
      useFactory: (config: AppConfig) => createLogger(config.logLevel),
    },
    {
      provide: KEY_VALUE_STORE,
      inject: [APP_CONFIG],
      useFactory: (config: AppConfig) => new RedisKeyValueStore(config.redisUrl),
    },
    {
      provide: IdentityProvider,
      inject: [APP_CONFIG, KEY_VALUE_STORE],
      useFactory: (config: AppConfig, store: KeyValueStore) =>
        config.identityProvider === 'cognito'
          ? new CognitoIdentityProvider(config.cognitoRegion, config.cognitoClientId)
          : new LocalIdentityProvider(store, config.mockOauth2Url),
    },
    { provide: CLOCK, useValue: () => Date.now() },
    { provide: FETCH, useValue: fetch },
    { provide: APP_FILTER, useClass: BackendErrorFilter },
    SessionService,
    SessionGuard,
    BackendClient,
  ],
  exports: [SessionGuard, SessionService, BackendClient, APP_CONFIG, LOGGER],
})
export class AppModule {}
