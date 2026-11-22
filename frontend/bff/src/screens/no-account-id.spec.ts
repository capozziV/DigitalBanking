import { IDS, URLS, serviceProblem } from '../testing/bodies.js';
import { createTestApp, type SignedIn, type TestApp } from '../testing/test-app.js';

const PHONE = '21912345678';

interface Route {
  route: string;
  call: (user: SignedIn) => ReturnType<SignedIn['get']> | Promise<{ text: string; status: number }>;
}

const ROUTES: Route[] = [
  { route: 'GET /bff/health', call: (u) => u.get('/bff/health') },
  {
    route: 'POST /bff/auth/sign-up',
    call: (u) => u.post('/bff/auth/sign-up').send({ email: u.email, password: '123456' }),
  },
  {
    route: 'POST /bff/auth/confirm',
    call: (u) => u.post('/bff/auth/confirm').send({ email: u.email, code: '123456' }),
  },
  {
    route: 'POST /bff/auth/sign-in',
    call: (u) => u.post('/bff/auth/sign-in').send({ email: u.email, password: '123456' }),
  },
  { route: 'POST /bff/auth/sign-out', call: (u) => u.post('/bff/auth/sign-out').send({}) },
  { route: 'GET /bff/auth/session', call: (u) => u.get('/bff/auth/session') },
  { route: 'GET /bff/me', call: (u) => u.get('/bff/me') },
  {
    route: 'POST /bff/customers',
    call: (u) => u.post('/bff/customers').send({ name: 'Ana', phone: '11987654321' }),
  },
  {
    route: 'PUT /bff/me/phone',
    call: (u) => u.put('/bff/me/phone').send({ phone: '11987654321' }),
  },
  { route: 'POST /bff/accounts', call: (u) => u.post('/bff/accounts') },
  { route: 'POST /bff/accounts/close', call: (u) => u.post('/bff/accounts/close') },
  { route: 'GET /bff/home', call: (u) => u.get('/bff/home') },
  { route: 'GET /bff/statement', call: (u) => u.get('/bff/statement') },
  {
    route: 'GET /bff/transfers/recipient',
    call: (u) => u.get('/bff/transfers/recipient').query({ phone: PHONE }),
  },
  {
    route: 'POST /bff/transfers',
    call: async (u) => {
      const found = await u.get('/bff/transfers/recipient').query({ phone: PHONE }).expect(200);
      return u
        .post('/bff/transfers')
        .set('Idempotency-Key', 'key-1')
        .send({ recipientToken: found.body.recipientToken, amount: 100 });
    },
  },
  { route: 'GET /bff/transfers/:id', call: (u) => u.get(`/bff/transfers/${IDS.transfer}`) },
  { route: 'GET /bff/notifications', call: (u) => u.get('/bff/notifications') },
  {
    route: 'GET /bff/notifications/unread-count',
    call: (u) => u.get('/bff/notifications/unread-count'),
  },
  { route: 'POST /bff/notifications/read', call: (u) => u.post('/bff/notifications/read') },
];

describe('no account or customer id reaches the browser', () => {
  let t: TestApp;
  let user: SignedIn;

  beforeEach(async () => {
    const customer = {
      id: IDS.customer,
      name: 'Ana Souza',
      email: 'ana@example.test',
      phone: '+5511987654321',
      status: 'ACTIVE',
    };
    const account = {
      id: IDS.account,
      customerId: IDS.customer,
      type: 'FREE',
      status: 'ACTIVE',
      balance: 10000,
      overdraftLimit: 0,
      availableBalance: 10000,
    };
    const transfer = {
      id: IDS.transfer,
      sourceAccountId: IDS.account,
      destinationAccountId: IDS.otherAccount,
      amount: 100,
      status: 'COMPENSATED',
      failureReason: 'account-closed',
    };
    t = await createTestApp();
    t.backend
      .on('GET', `${URLS.customer}/customers/me`, { body: customer })
      .on('POST', `${URLS.customer}/customers`, { status: 201, body: customer })
      .on('PUT', `${URLS.customer}/customers/me/phone`, { body: customer })
      .on('GET', `${URLS.customer}/customers/lookup?phone=%2B55${PHONE}`, {
        body: { customerId: IDS.otherCustomer, fullName: 'Bruno Lima' },
      })
      .on('GET', `${URLS.accounts}/accounts/me`, { body: account })
      .on('POST', `${URLS.accounts}/accounts`, { status: 201, body: account })
      .on('POST', `${URLS.accounts}/accounts/${IDS.account}/close`, {
        body: { ...account, status: 'CLOSED' },
      })
      .on('GET', `${URLS.accounts}/accounts/recipient?customerId=${IDS.otherCustomer}`, {
        body: { accountId: IDS.otherAccount },
      })
      .on('GET', `${URLS.transfers}/accounts/${IDS.account}/daily-limit`, {
        body: { limitCents: 500000, usedCents: 0, availableCents: 500000 },
      })
      .on('GET', `${URLS.statement}/accounts/${IDS.account}/statement`, {
        body: {
          accountId: IDS.account,
          entries: [
            {
              entryId: '77777777-7777-4777-8777-777777777777',
              kind: 'DEBIT',
              amount: 100,
              balanceAfter: 9900,
              description: `Transfer to ${IDS.otherAccount}`,
              createdAt: '2026-11-22T10:00:00Z',
            },
          ],
          nextCursor: null,
        },
      })
      .on('POST', `${URLS.transfers}/transfers`, { status: 201, body: transfer })
      .on('GET', `${URLS.transfers}/transfers/${IDS.transfer}`, { body: transfer })
      .on('GET', `${URLS.notification}/notifications`, {
        body: {
          items: [
            {
              id: IDS.notification,
              kind: 'TRANSFER_RECEIVED',
              title: 'Você recebeu uma transferência',
              body: 'Você recebeu R$ 1,00 de Bruno Lima.',
              data: { sourceAccountId: IDS.otherAccount, destinationAccountId: IDS.account },
              createdAt: '2026-11-22T10:00:00Z',
              readAt: null,
            },
          ],
          unreadCount: 1,
          nextCursor: null,
        },
      })
      .on('POST', `${URLS.notification}/notifications/read`, { body: { marked: 1 } });
    user = await t.signIn();
  });
  afterEach(() => t.close());

  function expectNoIds(text: string) {
    for (const id of [IDS.account, IDS.otherAccount, IDS.customer, IDS.otherCustomer]) {
      expect(text).not.toContain(id);
    }
  }

  it.each(ROUTES)('$route', async ({ call }) => {
    const response = await call(user);
    expect(response.status).toBeLessThan(300);
    expectNoIds(response.text);
  });

  it.each(ROUTES.filter(({ route }) => !/health|auth|POST .bff.transfers$/.test(route)))(
    '$route, with the service refusing',
    async ({ call }) => {
      t.backend.failAll(
        serviceProblem(
          422,
          'account-not-found',
          'Account not found',
          `account ${IDS.otherAccount} of customer ${IDS.customer} not found`,
          `/accounts/${IDS.account}`,
          { accountId: IDS.otherAccount },
        ),
      );
      const response = await call(user);
      expect(response.status).toBe(422);
      expectNoIds(response.text);
    },
  );

  it('covers every route of the BFF', () => {
    const { router } = t.app.getHttpAdapter().getInstance() as {
      router: { stack: { route?: { path: string; methods: Record<string, boolean> } }[] };
    };
    const registered = router.stack.flatMap(({ route }) =>
      route
        ? Object.keys(route.methods).map((method) => `${method.toUpperCase()} ${route.path}`)
        : [],
    );
    expect(ROUTES.map(({ route }) => route).sort()).toEqual(registered.sort());
  });
});
