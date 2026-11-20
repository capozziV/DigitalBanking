import { Writable } from 'node:stream';
import { Test } from '@nestjs/testing';
import type { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from '../app.module.js';
import { configureApp } from '../configure-app.js';
import { APP_CONFIG, loadConfig } from '../config/config.js';
import { IdentityProvider } from '../identity/identity-provider.js';
import { createLogger, LOGGER } from '../observability/logger.js';
import { KEY_VALUE_STORE } from '../redis/key-value-store.js';
import { CLOCK } from '../session/session.service.js';
import { FakeIdentityProvider } from '../testing/fake-identity-provider.js';
import { InMemoryKeyValueStore } from '../testing/in-memory-key-value-store.js';

const TEST_ENV = {
  IDENTITY_PROVIDER: 'local',
  REDIS_URL: 'redis://127.0.0.1:1',
  COOKIE_SECURE: 'true',
  LOG_LEVEL: 'silent',
};

describe('AuthController', () => {
  let app: INestApplication;
  let identity: FakeIdentityProvider;
  let store: InMemoryKeyValueStore;
  let logs: string;
  let now: number;

  beforeEach(async () => {
    identity = new FakeIdentityProvider();
    store = new InMemoryKeyValueStore();
    logs = '';
    now = 1_000_000;
    const sink = new Writable({
      write(chunk, _encoding, callback) {
        logs += chunk.toString();
        callback();
      },
    });
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(APP_CONFIG)
      .useValue(loadConfig(TEST_ENV))
      .overrideProvider(IdentityProvider)
      .useValue(identity)
      .overrideProvider(KEY_VALUE_STORE)
      .useValue(store)
      .overrideProvider(LOGGER)
      .useValue(createLogger('debug', sink))
      .overrideProvider(CLOCK)
      .useValue(() => now)
      .compile();
    app = moduleRef.createNestApplication();
    configureApp(app);
    await app.init();
  });

  afterEach(async () => {
    await app.close();
  });

  const http = () => request(app.getHttpServer());

  async function signIn() {
    const response = await http()
      .post('/bff/auth/sign-in')
      .send({ email: 'ana@example.test', password: '123456' })
      .expect(200);
    const cookies = (response.headers['set-cookie'] as unknown as string[]).map((c) => c);
    const cookieValue = (name: string) =>
      cookies
        .find((c) => c.startsWith(`${name}=`))
        ?.split(';')[0]
        ?.split('=')[1] ?? '';
    const session = cookieValue('bank_session');
    const csrf = cookieValue('bank_csrf');
    return {
      response,
      cookies,
      session,
      csrf,
      header: `bank_session=${session}; bank_csrf=${csrf}`,
    };
  }

  describe('password validation (before reaching the identity provider)', () => {
    it.each(['12345', '1234567', '12a456'])('sign-up rejects %s with 400', async (password) => {
      await http()
        .post('/bff/auth/sign-up')
        .send({ email: 'ana@example.test', password })
        .expect(400);
      expect(identity.calls).toEqual([]);
    });

    it.each(['12345', '1234567', '12a456'])('sign-in rejects %s with 400', async (password) => {
      await http()
        .post('/bff/auth/sign-in')
        .send({ email: 'ana@example.test', password })
        .expect(400);
      expect(identity.calls).toEqual([]);
    });

    it('sign-up accepts 123456', async () => {
      const response = await http()
        .post('/bff/auth/sign-up')
        .send({ email: 'ana@example.test', password: '123456' })
        .expect(201);
      expect(response.body).toEqual({ confirmationRequired: false });
      expect(identity.calls).toEqual(['signUp:ana@example.test']);
    });

    it('rejects an invalid email with 400', async () => {
      await http()
        .post('/bff/auth/sign-up')
        .send({ email: 'not-an-email', password: '123456' })
        .expect(400);
    });
  });

  describe('confirm', () => {
    it('passes a 6-digit code to the provider', async () => {
      await http()
        .post('/bff/auth/confirm')
        .send({ email: 'ana@example.test', code: '654321' })
        .expect(204);
      expect(identity.calls).toEqual(['confirmSignUp:ana@example.test:654321']);
    });

    it('rejects a malformed code with 400', async () => {
      await http()
        .post('/bff/auth/confirm')
        .send({ email: 'ana@example.test', code: 'abc' })
        .expect(400);
    });
  });

  describe('sign-in and cookies', () => {
    it('sets a HttpOnly, SameSite=Strict, Secure session cookie and a readable csrf cookie', async () => {
      const { cookies, response } = await signIn();
      const sessionCookie = cookies.find((c) => c.startsWith('bank_session='))!;
      expect(sessionCookie).toContain('HttpOnly');
      expect(sessionCookie).toContain('SameSite=Strict');
      expect(sessionCookie).toContain('Secure');
      const csrfCookie = cookies.find((c) => c.startsWith('bank_csrf='))!;
      expect(csrfCookie).not.toContain('HttpOnly');
      expect(csrfCookie).toContain('SameSite=Strict');
      expect(csrfCookie).toContain('Secure');
      expect(response.body).toEqual({ authenticated: true, email: 'ana@example.test' });
    });

    it('never returns the token to the browser', async () => {
      const { response } = await signIn();
      expect(JSON.stringify(response.body)).not.toContain('secret');
      expect(JSON.stringify(response.headers)).not.toContain('access-secret');
    });

    it('keeps the session in Redis under the bff:session: prefix', async () => {
      const { session } = await signIn();
      expect(store.keys()).toEqual([`bff:session:${session}`]);
    });

    it('rejects wrong credentials with 401', async () => {
      await http()
        .post('/bff/auth/sign-in')
        .send({ email: 'ana@example.test', password: '654321' })
        .expect(401);
    });

    it('omits Secure when COOKIE_SECURE=false', async () => {
      await app.close();
      const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
        .overrideProvider(APP_CONFIG)
        .useValue(loadConfig({ ...TEST_ENV, COOKIE_SECURE: 'false' }))
        .overrideProvider(IdentityProvider)
        .useValue(identity)
        .overrideProvider(KEY_VALUE_STORE)
        .useValue(store)
        .compile();
      app = moduleRef.createNestApplication();
      configureApp(app);
      await app.init();
      const response = await http()
        .post('/bff/auth/sign-in')
        .send({ email: 'ana@example.test', password: '123456' })
        .expect(200);
      const cookies = response.headers['set-cookie'] as unknown as string[];
      expect(cookies.some((c) => c.includes('Secure'))).toBe(false);
    });
  });

  describe('GET /bff/auth/session', () => {
    it('reports unauthenticated without a cookie', async () => {
      const response = await http().get('/bff/auth/session').expect(200);
      expect(response.body).toEqual({ authenticated: false, email: null });
    });

    it('reports the email with a valid session', async () => {
      const { header } = await signIn();
      const response = await http().get('/bff/auth/session').set('Cookie', header).expect(200);
      expect(response.body).toEqual({ authenticated: true, email: 'ana@example.test' });
    });

    it('reports unauthenticated when Redis has no such session', async () => {
      const response = await http()
        .get('/bff/auth/session')
        .set('Cookie', 'bank_session=gone; bank_csrf=x')
        .expect(200);
      expect(response.body.authenticated).toBe(false);
    });
  });

  describe('CSRF', () => {
    it('rejects POST sign-out without X-CSRF-Token with 403', async () => {
      const { header } = await signIn();
      await http().post('/bff/auth/sign-out').set('Cookie', header).expect(403);
    });

    it('rejects POST with a wrong X-CSRF-Token with 403', async () => {
      const { header } = await signIn();
      await http()
        .post('/bff/auth/sign-out')
        .set('Cookie', header)
        .set('X-CSRF-Token', 'wrong')
        .expect(403);
    });

    it('signs out with the right token, deleting the session', async () => {
      const { header, csrf } = await signIn();
      await http()
        .post('/bff/auth/sign-out')
        .set('Cookie', header)
        .set('X-CSRF-Token', csrf)
        .expect(204);
      expect(store.keys()).toEqual([]);
      const response = await http().get('/bff/auth/session').set('Cookie', header).expect(200);
      expect(response.body.authenticated).toBe(false);
    });

    it('rejects with 403 when the header matches the session but the bank_csrf cookie differs', async () => {
      const { session, csrf } = await signIn();
      await http()
        .post('/bff/auth/sign-out')
        .set('Cookie', `bank_session=${session}; bank_csrf=other`)
        .set('X-CSRF-Token', csrf)
        .expect(403);
      expect(store.keys()).toHaveLength(1);
    });

    it('rejects with 403 a header of a different length than the session token', async () => {
      const { header, csrf } = await signIn();
      await http()
        .post('/bff/auth/sign-out')
        .set('Cookie', header)
        .set('X-CSRF-Token', csrf + 'x')
        .expect(403);
    });

    it('answers 401 (not 403) on sign-out without a session', async () => {
      await http().post('/bff/auth/sign-out').set('X-CSRF-Token', 'x').expect(401);
    });
  });

  describe('token refresh', () => {
    it('refreshes when fewer than 60 s remain and keeps the session', async () => {
      identity.expiresInSeconds = 100;
      const { header, csrf } = await signIn();
      now += 30_000; // 70 s left: no refresh
      await http()
        .post('/bff/auth/sign-out')
        .set('Cookie', header)
        .set('X-CSRF-Token', 'wrong')
        .expect(403);
      expect(identity.calls.filter((c) => c.startsWith('refresh'))).toEqual([]);
      now += 20_000; // 50 s left: refresh
      await http()
        .post('/bff/auth/sign-out')
        .set('Cookie', header)
        .set('X-CSRF-Token', 'wrong')
        .expect(403);
      expect(identity.calls.filter((c) => c.startsWith('refresh'))).toEqual(['refresh:refresh']);
      expect(csrf).not.toBe('');
      expect(store.keys()).toHaveLength(1);
    });

    it('deletes the session and answers 401 when the refresh fails', async () => {
      identity.expiresInSeconds = 30;
      const { header, csrf } = await signIn();
      identity.failRefresh = true;
      await http()
        .post('/bff/auth/sign-out')
        .set('Cookie', header)
        .set('X-CSRF-Token', csrf)
        .expect(401);
      expect(store.keys()).toEqual([]);
    });

    it('keeps the session and answers 503 when the identity provider is unavailable', async () => {
      identity.expiresInSeconds = 30;
      const { header, csrf } = await signIn();
      identity.refreshError = 'unavailable';
      const response = await http()
        .post('/bff/auth/sign-out')
        .set('Cookie', header)
        .set('X-CSRF-Token', csrf)
        .expect(503);
      expect(response.body.code).toBe('unavailable');
      expect(store.keys()).toHaveLength(1);
    });

    it('keeps the session and answers 503 on an unexpected refresh error (network)', async () => {
      identity.expiresInSeconds = 30;
      const { header, csrf } = await signIn();
      identity.refreshError = new TypeError('fetch failed');
      await http()
        .post('/bff/auth/sign-out')
        .set('Cookie', header)
        .set('X-CSRF-Token', csrf)
        .expect(503);
      expect(store.keys()).toHaveLength(1);
    });
  });

  describe('malformed cookies', () => {
    it('treats a bank_session that cannot be decoded as no session (401, not 500)', async () => {
      await http()
        .post('/bff/auth/sign-out')
        .set('Cookie', 'bank_session=%; bank_csrf=x')
        .set('X-CSRF-Token', 'x')
        .expect(401);
    });

    it('reports unauthenticated on GET session with a malformed cookie', async () => {
      const response = await http()
        .get('/bff/auth/session')
        .set('Cookie', 'bank_session=%E0%A4%A')
        .expect(200);
      expect(response.body.authenticated).toBe(false);
    });
  });

  describe('correlation id and logs', () => {
    it('generates X-Correlation-ID when the request has none', async () => {
      const response = await http().get('/bff/health').expect(200);
      expect(response.headers['x-correlation-id']).toMatch(/^[0-9a-f-]{36}$/);
    });

    it('echoes the X-Correlation-ID of the request', async () => {
      const response = await http()
        .get('/bff/health')
        .set('X-Correlation-ID', 'abc-123')
        .expect(200);
      expect(response.headers['x-correlation-id']).toBe('abc-123');
    });

    it('logs requests with the correlation id and never the token or the password', async () => {
      await http()
        .post('/bff/auth/sign-in')
        .set('X-Correlation-ID', 'corr-1')
        .send({ email: 'ana@example.test', password: '123456' })
        .expect(200);
      await http()
        .post('/bff/auth/sign-in')
        .send({ email: 'ana@example.test', password: '654321' })
        .expect(401);
      expect(logs).toContain('corr-1');
      expect(logs).not.toContain('secret');
      expect(logs).not.toContain('123456');
      expect(logs).not.toContain('654321');
      expect(logs).not.toContain('refresh');
    });
  });
});
