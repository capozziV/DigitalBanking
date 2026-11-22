import request from 'supertest';
import { URLS } from './testing/bodies.js';
import { createTestApp, type TestApp } from './testing/test-app.js';

describe('writes take JSON only (login CSRF)', () => {
  let t: TestApp;

  beforeEach(async () => {
    t = await createTestApp();
  });
  afterEach(() => t.close());

  const FORM = 'application/x-www-form-urlencoded';
  const UNSUPPORTED = { code: 'unsupported_media_type', message: 'Envie os dados em JSON.' };
  const http = () => request(t.app.getHttpServer());
  const credentials = 'email=attacker%40example.test&password=123456';

  it('refuses the cross-site form sign-in with 415 and creates no session', async () => {
    const response = await http()
      .post('/bff/auth/sign-in')
      .set('Origin', 'https://evil.example')
      .set('Content-Type', FORM)
      .send(credentials);
    expect(response.status).toBe(415);
    expect(response.body).toEqual(UNSUPPORTED);
    expect(response.headers['set-cookie']).toBeUndefined();
    expect(t.store.keys()).toEqual([]);
    expect(t.identity.calls).toEqual([]);
  });

  it.each([
    FORM,
    'multipart/form-data; boundary=x',
    'text/plain',
    'text/plain; application/json',
    'application/json-seq',
  ])('refuses %s', async (type) => {
    await http().post('/bff/auth/sign-in').set('Content-Type', type).send(credentials).expect(415);
  });

  it.each([
    ['post', '/bff/auth/sign-up'],
    ['post', '/bff/auth/confirm'],
    ['post', '/bff/auth/sign-out'],
    ['post', '/bff/customers'],
    ['post', '/bff/accounts'],
    ['post', '/bff/accounts/close'],
    ['post', '/bff/transfers'],
    ['post', '/bff/notifications/read'],
    ['put', '/bff/me/phone'],
    ['patch', '/bff/me/phone'],
    ['delete', '/bff/me/phone'],
  ] as const)('refuses a form %s %s', async (method, path) => {
    const response = await http()[method](path).set('Content-Type', FORM).send(credentials);
    expect(response.status).toBe(415);
  });

  it('refuses an empty form (a form without fields still says its type)', async () => {
    const response = await http().post('/bff/auth/sign-out').set('Content-Type', FORM);
    expect(response.status).toBe(415);
    expect(response.headers['set-cookie']).toBeUndefined();
  });

  it('refuses a body without a type', async () => {
    await t.app.listen(0, '127.0.0.1');
    const response = await fetch(`${await t.app.getUrl()}/bff/auth/sign-in`, {
      method: 'POST',
      body: new Blob([credentials]),
    });
    expect(response.status).toBe(415);
  });

  it('refuses a streamed body without a type', async () => {
    await t.app.listen(0, '127.0.0.1');
    const response = await fetch(`${await t.app.getUrl()}/bff/auth/sign-in`, {
      method: 'POST',
      body: new Blob([credentials]).stream(),
      duplex: 'half',
    } as RequestInit);
    expect(response.status).toBe(415);
  });

  it.each(['application/json', 'application/json; charset=utf-8', 'Application/JSON'])(
    'takes %s',
    async (type) => {
      await http()
        .post('/bff/auth/sign-in')
        .set('Content-Type', type)
        .send('{"email":"ana@example.test","password":"123456"}')
        .expect(200);
    },
  );

  it('takes the POST without body and the POST {} that the web sends', async () => {
    t.backend.on('POST', `${URLS.accounts}/accounts`, { status: 201, body: { type: 'FREE' } });
    const user = await t.signIn();
    await user.post('/bff/accounts').expect(201);
    await user.post('/bff/accounts').set('Content-Length', '0').expect(201);
    await user.post('/bff/accounts').send({}).expect(201);
  });

  it('does not look at the type of a GET', async () => {
    await http().get('/bff/health').set('Content-Type', FORM).expect(200);
  });
});
