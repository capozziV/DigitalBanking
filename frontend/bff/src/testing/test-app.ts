import { Test } from '@nestjs/testing';
import type { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from '../app.module.js';
import { FETCH } from '../backend/backend-client.js';
import { configureApp } from '../configure-app.js';
import { APP_CONFIG, loadConfig } from '../config/config.js';
import { IdentityProvider } from '../identity/identity-provider.js';
import { KEY_VALUE_STORE } from '../redis/key-value-store.js';
import { CLOCK } from '../session/session.service.js';
import { FakeBackend } from './fake-backend.js';
import { FakeIdentityProvider } from './fake-identity-provider.js';
import { InMemoryKeyValueStore } from './in-memory-key-value-store.js';

export const TEST_ENV = {
  IDENTITY_PROVIDER: 'local',
  REDIS_URL: 'redis://127.0.0.1:1',
  COOKIE_SECURE: 'true',
  LOG_LEVEL: 'silent',
  RECIPIENT_TOKEN_SECRET: 'test-recipient-secret-0123456789',
};

export interface SignedIn {
  email: string;
  sessionId: string;
  get(path: string): request.Test;
  post(path: string): request.Test;
  put(path: string): request.Test;
}

export interface TestApp {
  app: INestApplication;
  backend: FakeBackend;
  clock: { now: number };
  /** Signs in as ana@example.test and returns request helpers that carry the cookies and the CSRF header. */
  signIn(): Promise<SignedIn>;
  close(): Promise<void>;
}

export async function createTestApp(): Promise<TestApp> {
  const backend = new FakeBackend();
  const clock = { now: 1_000_000 };
  const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
    .overrideProvider(APP_CONFIG)
    .useValue(loadConfig(TEST_ENV))
    .overrideProvider(IdentityProvider)
    .useValue(new FakeIdentityProvider())
    .overrideProvider(KEY_VALUE_STORE)
    .useValue(new InMemoryKeyValueStore())
    .overrideProvider(FETCH)
    .useValue(backend.fetch)
    .overrideProvider(CLOCK)
    .useValue(() => clock.now)
    .compile();
  const app = moduleRef.createNestApplication();
  configureApp(app);
  await app.init();

  return {
    app,
    backend,
    clock,
    async signIn() {
      const response = await request(app.getHttpServer())
        .post('/bff/auth/sign-in')
        .send({ email: 'ana@example.test', password: '123456' })
        .expect(200);
      const cookies = response.headers['set-cookie'] as unknown as string[];
      const value = (name: string) =>
        cookies
          .find((c) => c.startsWith(`${name}=`))
          ?.split(';')[0]
          ?.split('=')[1] ?? '';
      const sessionId = value('bank_session');
      const csrf = value('bank_csrf');
      const cookie = `bank_session=${sessionId}; bank_csrf=${csrf}`;
      const http = () => request(app.getHttpServer());
      return {
        email: 'ana@example.test',
        sessionId,
        get: (path) => http().get(path).set('Cookie', cookie),
        post: (path) => http().post(path).set('Cookie', cookie).set('X-CSRF-Token', csrf),
        put: (path) => http().put(path).set('Cookie', cookie).set('X-CSRF-Token', csrf),
      };
    },
    close: () => app.close(),
  };
}
