import type { BackendCall } from '../testing/fake-backend.js';
import { createTestApp, type SignedIn, type TestApp } from '../testing/test-app.js';

const ACCOUNTS = 'http://localhost:8081';
const TRANSFERS = 'http://localhost:8082';
const NOTIFICATION = 'http://localhost:8083';
const STATEMENT = 'http://localhost:8084';
const CUSTOMER = 'http://localhost:8085';

const ME_ID = '11111111-1111-4111-8111-111111111111';
const ACCOUNT_ID = '22222222-2222-4222-8222-222222222222';
const OTHER_CUSTOMER_ID = '33333333-3333-4333-8333-333333333333';
const OTHER_ACCOUNT_ID = '44444444-4444-4444-8444-444444444444';
const TRANSFER_ID = '55555555-5555-4555-8555-555555555555';
const NOTIFICATION_ID = '66666666-6666-4666-8666-666666666666';

const customerDto = {
  id: ME_ID,
  name: 'Ana Souza',
  email: 'ana@example.test',
  phone: '+5511987654321',
  status: 'ACTIVE',
  createdAt: '2026-11-19T10:00:00Z',
  updatedAt: '2026-11-19T10:00:00Z',
};
const accountDto = {
  id: ACCOUNT_ID,
  customerId: ME_ID,
  type: 'FREE',
  status: 'ACTIVE',
  balance: 10000,
  overdraftLimit: 0,
  availableBalance: 10000,
};
const entry = (n: number) => ({
  entryId: `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`,
  sequence: n,
  kind: n % 2 ? 'CREDIT' : 'DEBIT',
  amount: 100 * n,
  balanceAfter: 1000 * n,
  description: `entry ${n}`,
  createdAt: '2026-11-19T10:00:00Z',
});
const problem = (status: number, title: string) => ({
  status,
  body: { type: `/problems/${title}`, title, status, detail: `${title} detail` },
});

describe('screens', () => {
  let t: TestApp;
  let user: SignedIn;

  beforeEach(async () => {
    t = await createTestApp();
    user = await t.signIn();
  });
  afterEach(() => t.close());

  describe('authentication', () => {
    it('answers 401 without a session', async () => {
      const request = (await import('supertest')).default;
      await request(t.app.getHttpServer()).get('/bff/me').expect(401);
      await request(t.app.getHttpServer()).get('/bff/home').expect(401);
      await request(t.app.getHttpServer()).get('/bff/notifications/unread-count').expect(401);
    });

    it('requires the CSRF header on POST', async () => {
      const request = (await import('supertest')).default;
      const login = await request(t.app.getHttpServer())
        .post('/bff/auth/sign-in')
        .send({ email: 'ana@example.test', password: '123456' });
      const cookies = (login.headers['set-cookie'] as unknown as string[]).map(
        (c) => c.split(';')[0]!,
      );
      await request(t.app.getHttpServer())
        .post('/bff/accounts')
        .set('Cookie', cookies.join('; '))
        .expect(403);
    });
  });

  describe('GET /bff/me', () => {
    it('joins the customer and the account', async () => {
      t.backend
        .on('GET', `${CUSTOMER}/customers/me`, { body: customerDto })
        .on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto });
      const response = await user.get('/bff/me').expect(200);
      expect(response.body).toEqual({
        customer: { name: 'Ana Souza', email: 'ana@example.test', phone: '+5511987654321' },
        account: {
          id: ACCOUNT_ID,
          type: 'FREE',
          status: 'ACTIVE',
          balance: 10000,
          overdraftLimit: 0,
          availableBalance: 10000,
        },
      });
    });

    it('answers customer null when the customer does not exist yet', async () => {
      t.backend
        .on('GET', `${CUSTOMER}/customers/me`, problem(404, 'customer-not-found'))
        .on('GET', `${ACCOUNTS}/accounts/me`, problem(404, 'account-not-found'));
      const response = await user.get('/bff/me').expect(200);
      expect(response.body).toEqual({ customer: null, account: null });
    });

    it('answers account null when there is no open account', async () => {
      t.backend
        .on('GET', `${CUSTOMER}/customers/me`, { body: customerDto })
        .on('GET', `${ACCOUNTS}/accounts/me`, problem(404, 'account-not-found'));
      const response = await user.get('/bff/me').expect(200);
      expect(response.body.customer.name).toBe('Ana Souza');
      expect(response.body.account).toBeNull();
    });

    it('does not hide a backend failure behind null', async () => {
      t.backend
        .on('GET', `${CUSTOMER}/customers/me`, problem(500, 'boom'))
        .on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto });
      await user.get('/bff/me').expect(500);
    });

    it('sends the bearer token and a correlation id to the services', async () => {
      t.backend
        .on('GET', `${CUSTOMER}/customers/me`, { body: customerDto })
        .on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto });
      await user.get('/bff/me').set('X-Correlation-Id', 'corr-1').expect(200);
      const call = t.backend.callsTo('GET', `${CUSTOMER}/customers/me`)[0]!;
      expect(call.headers['authorization']).toMatch(/^Bearer .+/);
      expect(call.headers['x-correlation-id']).toBe('corr-1');
    });
  });

  describe('POST /bff/customers', () => {
    beforeEach(() => {
      t.backend.on('POST', `${CUSTOMER}/customers`, (call) => ({
        status: 201,
        body: { ...customerDto, ...(call.body as object) },
      }));
    });

    it.each(['(11) 98765-4321', '11987654321', '+55 11 98765-4321'])(
      'normalizes the phone %s to E.164 before calling the customer service',
      async (phone) => {
        const response = await user.post('/bff/customers').send({ name: ' Ana Souza ', phone });
        expect(response.status).toBe(201);
        const call = t.backend.callsTo('POST', `${CUSTOMER}/customers`)[0]!;
        expect(call.body).toEqual({
          name: 'Ana Souza',
          email: 'ana@example.test',
          phone: '+5511987654321',
        });
      },
    );

    it('answers 400 with a clear message for an invalid phone, without calling the backend', async () => {
      const response = await user.post('/bff/customers').send({ name: 'Ana', phone: '1234' });
      expect(response.status).toBe(400);
      expect(response.body.code).toBe('invalid_phone');
      expect(response.body.message).toContain('celular');
      expect(t.backend.calls).toEqual([]);
    });

    it.each([
      { phone: '11987654321' },
      { name: '   ', phone: '11987654321' },
      { name: 'A'.repeat(121), phone: '11987654321' },
    ])('answers 400 for a missing, blank or long name %j', async (body) => {
      const response = await user.post('/bff/customers').send(body);
      expect(response.status).toBe(400);
      expect(response.body.code).toBe('invalid_name');
      expect(t.backend.calls).toEqual([]);
    });

    it('passes the problem of the backend through (duplicate phone)', async () => {
      t.backend.on('POST', `${CUSTOMER}/customers`, problem(409, 'phone-already-registered'));
      const response = await user
        .post('/bff/customers')
        .send({ name: 'Ana', phone: '11987654321' });
      expect(response.status).toBe(409);
      expect(response.body.detail).toBe('phone-already-registered detail');
    });
  });

  describe('PUT /bff/me/phone', () => {
    it('normalizes the phone and forwards the change', async () => {
      t.backend.on('PUT', `${CUSTOMER}/customers/me/phone`, { body: customerDto });
      await user.put('/bff/me/phone').send({ phone: '(21) 91234-5678' }).expect(200);
      expect(t.backend.callsTo('PUT', `${CUSTOMER}/customers/me/phone`)[0]!.body).toEqual({
        phone: '+5521912345678',
      });
    });

    it('answers 400 for an invalid phone', async () => {
      const response = await user.put('/bff/me/phone').send({ phone: '123' });
      expect(response.status).toBe(400);
      expect(t.backend.calls).toEqual([]);
    });
  });

  describe('accounts', () => {
    it('opens the account with a generated idempotency key', async () => {
      t.backend.on('POST', `${ACCOUNTS}/accounts`, { status: 201, body: accountDto });
      const response = await user.post('/bff/accounts').expect(201);
      expect(response.body.id).toBe(ACCOUNT_ID);
      expect(response.body).not.toHaveProperty('customerId');
      const call = t.backend.callsTo('POST', `${ACCOUNTS}/accounts`)[0]!;
      expect(call.headers['idempotency-key']).toMatch(/^[0-9a-f-]{36}$/);
    });

    it('closes the own account', async () => {
      t.backend
        .on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto })
        .on('POST', `${ACCOUNTS}/accounts/${ACCOUNT_ID}/close`, {
          body: { ...accountDto, status: 'CLOSED' },
        });
      const response = await user.post('/bff/accounts/close').expect(201);
      expect(response.body.status).toBe('CLOSED');
    });

    it('passes the refusal to close through (balance not zero)', async () => {
      t.backend
        .on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto })
        .on('POST', `${ACCOUNTS}/accounts/${ACCOUNT_ID}/close`, problem(422, 'balance-not-zero'));
      const response = await user.post('/bff/accounts/close');
      expect(response.status).toBe(422);
      expect(response.body.detail).toBe('balance-not-zero detail');
    });
  });

  describe('GET /bff/home', () => {
    const entries = [1, 2, 3, 4, 5, 6, 7].map(entry);

    it('aggregates the account, the daily limit and the latest five entries', async () => {
      t.backend
        .on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto })
        .on('GET', `${TRANSFERS}/accounts/${ACCOUNT_ID}/daily-limit`, {
          body: {
            day: '2026-11-19',
            limitCents: 500000,
            usedCents: 120000,
            availableCents: 380000,
          },
        })
        .on('GET', `${STATEMENT}/accounts/${ACCOUNT_ID}/statement`, {
          body: { accountId: ACCOUNT_ID, entries, nextCursor: 'abc' },
        });
      const response = await user.get('/bff/home').expect(200);
      expect(response.body.account.id).toBe(ACCOUNT_ID);
      expect(response.body.dailyLimit).toEqual({ limit: 500000, used: 120000, available: 380000 });
      expect(response.body.latestEntries).toHaveLength(5);
      expect(response.body.latestEntries[0]).toEqual({
        id: entries[0]!.entryId,
        kind: 'CREDIT',
        amount: 100,
        description: 'entry 1',
        balanceAfter: 1000,
        createdAt: '2026-11-19T10:00:00Z',
      });
    });

    it('asks the limit and the statement in parallel', async () => {
      const started: string[] = [];
      let release!: () => void;
      const gate = new Promise<void>((resolve) => (release = resolve));
      t.backend
        .on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto })
        .on('GET', `${TRANSFERS}/accounts/${ACCOUNT_ID}/daily-limit`, async () => {
          started.push('limit');
          await gate;
          return { body: { day: 'd', limitCents: 1, usedCents: 0, availableCents: 1 } };
        })
        .on('GET', `${STATEMENT}/accounts/${ACCOUNT_ID}/statement`, async () => {
          started.push('statement');
          if (started.length === 2) release();
          await gate;
          return { body: { accountId: ACCOUNT_ID, entries: [], nextCursor: null } };
        });
      await user.get('/bff/home').expect(200);
      // Both started before either finished: with a sequential call the gate would never open.
      expect(started.sort()).toEqual(['limit', 'statement']);
    });

    it('answers 404 when the customer has no open account', async () => {
      t.backend.on('GET', `${ACCOUNTS}/accounts/me`, problem(404, 'account-not-found'));
      await user.get('/bff/home').expect(404);
    });

    it('shows an empty list while the statement has not projected the new account yet', async () => {
      // The projection is event-driven: right after "Abrir conta" the statement answers 404 for the account.
      t.backend
        .on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto })
        .on('GET', `${TRANSFERS}/accounts/${ACCOUNT_ID}/daily-limit`, {
          body: { day: 'd', limitCents: 500000, usedCents: 0, availableCents: 500000 },
        })
        .on(
          'GET',
          `${STATEMENT}/accounts/${ACCOUNT_ID}/statement`,
          problem(404, 'account-not-found'),
        );
      const response = await user.get('/bff/home').expect(200);
      expect(response.body.account.id).toBe(ACCOUNT_ID);
      expect(response.body.latestEntries).toEqual([]);
    });
  });

  describe('GET /bff/statement', () => {
    it('forwards the encoded cursor and maps the entries', async () => {
      const url = `${STATEMENT}/accounts/${ACCOUNT_ID}/statement?cursor=a%2Bb%3D%3D`;
      t.backend.on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto }).on('GET', url, {
        body: { accountId: ACCOUNT_ID, entries: [entry(1)], nextCursor: 'next' },
      });
      const response = await user.get('/bff/statement').query({ cursor: 'a+b==' }).expect(200);
      expect(response.body.nextCursor).toBe('next');
      expect(response.body.entries).toEqual([
        {
          id: entry(1).entryId,
          kind: 'CREDIT',
          amount: 100,
          description: 'entry 1',
          balanceAfter: 1000,
          createdAt: '2026-11-19T10:00:00Z',
        },
      ]);
    });

    it('asks the first page without a cursor and answers a null nextCursor at the end', async () => {
      t.backend
        .on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto })
        .on('GET', `${STATEMENT}/accounts/${ACCOUNT_ID}/statement`, {
          body: { accountId: ACCOUNT_ID, entries: [], nextCursor: null },
        });
      const response = await user.get('/bff/statement').expect(200);
      expect(response.body).toEqual({ entries: [], nextCursor: null });
    });

    it('answers an empty page while the statement has not projected the new account yet', async () => {
      t.backend
        .on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto })
        .on(
          'GET',
          `${STATEMENT}/accounts/${ACCOUNT_ID}/statement`,
          problem(404, 'account-not-found'),
        );
      const response = await user.get('/bff/statement').expect(200);
      expect(response.body).toEqual({ entries: [], nextCursor: null });
    });
  });

  describe('GET /bff/transfers/recipient', () => {
    const lookup = (phoneE164: string) =>
      `${CUSTOMER}/customers/lookup?phone=${encodeURIComponent(phoneE164)}`;
    const recipientUrl = `${ACCOUNTS}/accounts/recipient?customerId=${OTHER_CUSTOMER_ID}`;

    beforeEach(() => {
      t.backend
        .on('GET', `${CUSTOMER}/customers/me`, { body: customerDto })
        .on('GET', lookup('+5521912345678'), {
          body: { customerId: OTHER_CUSTOMER_ID, fullName: 'Bruno Lima' },
        })
        .on('GET', recipientUrl, { body: { accountId: OTHER_ACCOUNT_ID } });
    });

    it.each(['(21) 91234-5678', '21912345678', '+55 21 91234-5678'])(
      'finds the recipient by the phone %s',
      async (phone) => {
        const response = await user.get('/bff/transfers/recipient').query({ phone }).expect(200);
        expect(response.body.recipientName).toBe('Bruno Lima');
        expect(typeof response.body.recipientToken).toBe('string');
        expect(JSON.stringify(response.body)).not.toContain(OTHER_ACCOUNT_ID);
        expect(t.backend.callsTo('GET', lookup('+5521912345678'))).toHaveLength(1);
      },
    );

    it('answers 400 for a malformed phone, without calling the backend', async () => {
      const response = await user.get('/bff/transfers/recipient').query({ phone: '1234' });
      expect(response.status).toBe(400);
      expect(response.body.code).toBe('invalid_phone');
      expect(t.backend.calls).toEqual([]);
    });

    it('answers 422 when the phone is the own phone, before looking for an account', async () => {
      t.backend.on('GET', lookup('+5511987654321'), {
        body: { customerId: ME_ID, fullName: 'Ana Souza' },
      });
      const response = await user
        .get('/bff/transfers/recipient')
        .query({ phone: '(11) 98765-4321' });
      expect(response.status).toBe(422);
      expect(response.body.message).toContain('você mesmo');
      expect(t.backend.calls.some((c: BackendCall) => c.url.includes('/accounts/recipient'))).toBe(
        false,
      );
    });

    it('answers 404 "no account to receive" when the customer has no open account', async () => {
      t.backend.on('GET', recipientUrl, problem(404, 'account-not-found'));
      const response = await user.get('/bff/transfers/recipient').query({ phone: '21912345678' });
      expect(response.status).toBe(404);
      expect(response.body.code).toBe('recipient_without_account');
      expect(response.body.message).toBe('Este telefone não tem conta para receber.');
    });

    it('answers 404 when nobody has the phone', async () => {
      t.backend.on('GET', lookup('+5521912345678'), problem(404, 'customer-not-found'));
      const response = await user.get('/bff/transfers/recipient').query({ phone: '21912345678' });
      expect(response.status).toBe(404);
      expect(response.body.code).toBe('recipient_not_found');
    });
  });

  describe('POST /bff/transfers', () => {
    const KEY = 'a3f2c1de-0000-4000-8000-000000000001';

    async function recipientToken(): Promise<string> {
      t.backend
        .on('GET', `${CUSTOMER}/customers/me`, { body: customerDto })
        .on('GET', `${CUSTOMER}/customers/lookup?phone=${encodeURIComponent('+5521912345678')}`, {
          body: { customerId: OTHER_CUSTOMER_ID, fullName: 'Bruno Lima' },
        })
        .on('GET', `${ACCOUNTS}/accounts/recipient?customerId=${OTHER_CUSTOMER_ID}`, {
          body: { accountId: OTHER_ACCOUNT_ID },
        });
      const response = await user
        .get('/bff/transfers/recipient')
        .query({ phone: '21912345678' })
        .expect(200);
      return response.body.recipientToken as string;
    }

    beforeEach(() => {
      t.backend
        .on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto })
        .on('POST', `${TRANSFERS}/transfers`, (call) => ({
          status: 201,
          body: {
            id: TRANSFER_ID,
            ...(call.body as object),
            status: 'PENDING',
            failureReason: null,
          },
        }));
    });

    it('creates the transfer with the account behind the token and the given key', async () => {
      const recipientTokenValue = await recipientToken();
      const response = await user
        .post('/bff/transfers')
        .set('Idempotency-Key', KEY)
        .send({ recipientToken: recipientTokenValue, amount: 2500 })
        .expect(201);
      expect(response.body).toEqual({ id: TRANSFER_ID, status: 'PENDING' });
      const call = t.backend.callsTo('POST', `${TRANSFERS}/transfers`)[0]!;
      expect(call.body).toEqual({
        sourceAccountId: ACCOUNT_ID,
        destinationAccountId: OTHER_ACCOUNT_ID,
        amount: 2500,
      });
      expect(call.headers['idempotency-key']).toBe(KEY);
    });

    it('turns the refusal of the saga (422 transfer-failed) into a FAILED transfer to follow', async () => {
      // The transfers service answers 422 with the id of the refused transfer; the screen shows it as "Recusada".
      t.backend.on('POST', `${TRANSFERS}/transfers`, {
        status: 422,
        body: {
          type: '/problems/transfer-failed',
          title: 'Transfer failed',
          status: 422,
          detail: 'The transfer could not be completed',
          reason: 'daily-limit-exceeded',
          transferId: TRANSFER_ID,
        },
      });
      const recipientTokenValue = await recipientToken();
      const response = await user
        .post('/bff/transfers')
        .set('Idempotency-Key', KEY)
        .send({ recipientToken: recipientTokenValue, amount: 500001 })
        .expect(201);
      expect(response.body).toEqual({ id: TRANSFER_ID, status: 'FAILED' });
    });

    it('turns the refusal for insufficient available balance (422 with transferId) into a FAILED transfer to follow', async () => {
      t.backend.on('POST', `${TRANSFERS}/transfers`, {
        status: 422,
        body: {
          type: '/problems/insufficient-available-balance',
          title: 'Insufficient available balance',
          status: 422,
          detail: 'The available balance of the source account does not cover the transfer',
          transferId: TRANSFER_ID,
        },
      });
      const recipientTokenValue = await recipientToken();
      const response = await user
        .post('/bff/transfers')
        .set('Idempotency-Key', KEY)
        .send({ recipientToken: recipientTokenValue, amount: 2500 })
        .expect(201);
      expect(response.body).toEqual({ id: TRANSFER_ID, status: 'FAILED' });
    });

    it('keeps passing other 422 problems through', async () => {
      t.backend.on('POST', `${TRANSFERS}/transfers`, problem(422, 'customer-blocked'));
      const recipientTokenValue = await recipientToken();
      await user
        .post('/bff/transfers')
        .set('Idempotency-Key', KEY)
        .send({ recipientToken: recipientTokenValue, amount: 2500 })
        .expect(422);
    });

    it('a double click sends the same Idempotency-Key twice and gets the same transfer', async () => {
      const recipientTokenValue = await recipientToken();
      const send = () =>
        user
          .post('/bff/transfers')
          .set('Idempotency-Key', KEY)
          .send({ recipientToken: recipientTokenValue, amount: 2500 });
      const [first, second] = await Promise.all([send(), send()]);
      expect(first.body.id).toBe(TRANSFER_ID);
      expect(second.body.id).toBe(TRANSFER_ID);
      const keys = t.backend
        .callsTo('POST', `${TRANSFERS}/transfers`)
        .map((c) => c.headers['idempotency-key']);
      expect(keys).toEqual([KEY, KEY]);
    });

    it('answers 400 for an unknown recipientToken, without calling the backend', async () => {
      const response = await user
        .post('/bff/transfers')
        .set('Idempotency-Key', KEY)
        .send({ recipientToken: 'not-issued-by-the-bff', amount: 2500 });
      expect(response.status).toBe(400);
      expect(response.body.code).toBe('invalid_recipient');
      expect(response.body.message).toBe('Destinatário expirado. Busque o telefone de novo.');
      expect(t.backend.callsTo('POST', `${TRANSFERS}/transfers`)).toEqual([]);
    });

    it('issues a recipientToken that does not carry the account id', async () => {
      const recipientTokenValue = await recipientToken();
      expect(recipientTokenValue).not.toContain(OTHER_ACCOUNT_ID);
      expect(Buffer.from(recipientTokenValue, 'base64url').toString()).not.toContain(
        OTHER_ACCOUNT_ID,
      );
    });

    it('answers 400 for an expired recipientToken', async () => {
      const recipientTokenValue = await recipientToken();
      t.clock.now += 11 * 60_000;
      // The session keeps living, so renew the access token expiry by signing in again is not needed:
      // the fake identity provider refreshes it.
      await user
        .post('/bff/transfers')
        .set('Idempotency-Key', KEY)
        .send({ recipientToken: recipientTokenValue, amount: 2500 })
        .expect(400);
    });

    it('answers 400 for a recipientToken issued to another session', async () => {
      const recipientTokenValue = await recipientToken();
      const other = await t.signIn();
      await other
        .post('/bff/transfers')
        .set('Idempotency-Key', KEY)
        .send({ recipientToken: recipientTokenValue, amount: 2500 })
        .expect(400);
    });

    it('answers 400 without the Idempotency-Key header', async () => {
      const recipientTokenValue = await recipientToken();
      const response = await user
        .post('/bff/transfers')
        .send({ recipientToken: recipientTokenValue, amount: 2500 });
      expect(response.status).toBe(400);
      expect(response.body.code).toBe('idempotency_key_required');
    });

    it.each([0, -5, 12.5, '2500', null, 2 ** 53])(
      'answers 400 for the amount %j',
      async (amount) => {
        const recipientTokenValue = await recipientToken();
        const response = await user
          .post('/bff/transfers')
          .set('Idempotency-Key', KEY)
          .send({ recipientToken: recipientTokenValue, amount });
        expect(response.status).toBe(400);
        expect(response.body.code).toBe('invalid_amount');
      },
    );

    it('answers 422 when the token leads to the own account', async () => {
      t.backend.on('GET', `${ACCOUNTS}/accounts/recipient?customerId=${OTHER_CUSTOMER_ID}`, {
        body: { accountId: ACCOUNT_ID },
      });
      t.backend
        .on('GET', `${CUSTOMER}/customers/me`, { body: customerDto })
        .on('GET', `${CUSTOMER}/customers/lookup?phone=${encodeURIComponent('+5521912345678')}`, {
          body: { customerId: OTHER_CUSTOMER_ID, fullName: 'Bruno Lima' },
        });
      const token = (
        await user.get('/bff/transfers/recipient').query({ phone: '21912345678' }).expect(200)
      ).body.recipientToken as string;
      const response = await user
        .post('/bff/transfers')
        .set('Idempotency-Key', KEY)
        .send({ recipientToken: token, amount: 100 });
      expect(response.status).toBe(422);
      expect(t.backend.callsTo('POST', `${TRANSFERS}/transfers`)).toEqual([]);
    });
  });

  describe('GET /bff/transfers/:id', () => {
    const url = `${TRANSFERS}/transfers/${TRANSFER_ID}`;
    const transfer = (status: string, failureReason: string | null) => ({
      id: TRANSFER_ID,
      sourceAccountId: ACCOUNT_ID,
      destinationAccountId: OTHER_ACCOUNT_ID,
      amount: 100,
      description: null,
      status,
      failureReason,
      createdAt: '2026-11-19T10:00:00Z',
      updatedAt: '2026-11-19T10:00:01Z',
    });

    it('answers the status without the account ids', async () => {
      t.backend.on('GET', url, { body: transfer('COMPLETED', null) });
      const response = await user.get(`/bff/transfers/${TRANSFER_ID}`).expect(200);
      expect(response.body).toEqual({
        id: TRANSFER_ID,
        status: 'COMPLETED',
        reason: null,
        reasonCode: null,
      });
    });

    it.each([
      ['daily-limit-exceeded', 'limite diário'],
      ['insufficient-available-balance', 'Saldo'],
    ])('explains %s in Portuguese', async (code, text) => {
      t.backend.on('GET', url, { body: transfer('FAILED', code) });
      const response = await user.get(`/bff/transfers/${TRANSFER_ID}`).expect(200);
      expect(response.body.reason).toContain(text);
      expect(response.body.reasonCode).toBe(code);
    });

    it('gives a generic Portuguese reason for an unknown code', async () => {
      t.backend.on('GET', url, { body: transfer('COMPENSATED', 'something-new') });
      const response = await user.get(`/bff/transfers/${TRANSFER_ID}`).expect(200);
      expect(response.body.reason).toBe('A transferência não pôde ser concluída.');
    });

    it('answers 400 when the id is not a UUID', async () => {
      await user.get('/bff/transfers/not-a-uuid').expect(400);
      expect(t.backend.calls).toEqual([]);
    });

    it('passes a 404 through', async () => {
      t.backend.on('GET', url, problem(404, 'transfer-not-found'));
      await user.get(`/bff/transfers/${TRANSFER_ID}`).expect(404);
    });
  });

  describe('notifications', () => {
    const item = (id: string, readAt: string | null) => ({
      id,
      kind: 'TRANSFER_RECEIVED',
      title: 'Transferência recebida',
      body: 'Você recebeu R$ 25,00',
      data: {},
      createdAt: '2026-11-19T10:00:00Z',
      readAt,
    });
    const page = { items: [item(NOTIFICATION_ID, null)], unreadCount: 3, nextCursor: 'c2' };

    it('lists the page', async () => {
      t.backend.on('GET', `${NOTIFICATION}/notifications`, { body: page });
      const response = await user.get('/bff/notifications').expect(200);
      expect(response.body).toEqual(page);
    });

    it('forwards the cursor', async () => {
      t.backend.on('GET', `${NOTIFICATION}/notifications?cursor=x%3Dy`, { body: page });
      await user.get('/bff/notifications').query({ cursor: 'x=y' }).expect(200);
    });

    it('answers the unread count', async () => {
      t.backend.on('GET', `${NOTIFICATION}/notifications`, { body: page });
      const response = await user.get('/bff/notifications/unread-count').expect(200);
      expect(response.body).toEqual({ count: 3 });
    });

    it('marks as read up to the given notification', async () => {
      t.backend.on('POST', `${NOTIFICATION}/notifications/read`, { body: { marked: 2 } });
      const response = await user
        .post('/bff/notifications/read')
        .send({ upTo: NOTIFICATION_ID })
        .expect(200);
      expect(response.body).toEqual({ marked: 2 });
      expect(t.backend.callsTo('POST', `${NOTIFICATION}/notifications/read`)[0]!.body).toEqual({
        upTo: NOTIFICATION_ID,
      });
    });

    it('marks everything as read up to the newest when no upTo is given', async () => {
      t.backend
        .on('GET', `${NOTIFICATION}/notifications`, { body: page })
        .on('POST', `${NOTIFICATION}/notifications/read`, { body: { marked: 3 } });
      await user.post('/bff/notifications/read').send({}).expect(200);
      expect(t.backend.callsTo('POST', `${NOTIFICATION}/notifications/read`)[0]!.body).toEqual({
        upTo: NOTIFICATION_ID,
      });
    });

    it('answers marked 0 when there is nothing to mark', async () => {
      t.backend.on('GET', `${NOTIFICATION}/notifications`, {
        body: { items: [], unreadCount: 0, nextCursor: null },
      });
      const response = await user.post('/bff/notifications/read').send({}).expect(200);
      expect(response.body).toEqual({ marked: 0 });
    });

    it('answers 400 for an upTo that is not a UUID', async () => {
      await user.post('/bff/notifications/read').send({ upTo: 'x' }).expect(400);
      expect(t.backend.calls).toEqual([]);
    });
  });

  describe('backend failures', () => {
    it('answers 503 in Portuguese when a service is down', async () => {
      t.backend.on('GET', `${CUSTOMER}/customers/me`, () => {
        throw new TypeError('connect ECONNREFUSED');
      });
      t.backend.on('GET', `${ACCOUNTS}/accounts/me`, { body: accountDto });
      const response = await user.get('/bff/me');
      expect(response.status).toBe(503);
      expect(response.body.code).toBe('unavailable');
    });
  });
});
