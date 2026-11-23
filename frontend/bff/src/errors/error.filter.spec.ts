import { Writable } from 'node:stream';
import request from 'supertest';
import { createLogger } from '../observability/logger.js';
import { IDS, URLS, serviceProblem } from '../testing/bodies.js';
import { createTestApp, type SignedIn, type TestApp } from '../testing/test-app.js';

const GENERIC = 'Não foi possível concluir a operação. Tente de novo.';
const UNAVAILABLE = 'Serviço indisponível. Tente novamente.';

describe('errors that reach the screen', () => {
  let t: TestApp;
  let user: SignedIn;
  let logs: string;

  beforeEach(async () => {
    logs = '';
    const sink = new Writable({
      write(chunk, _encoding, callback) {
        logs += chunk.toString();
        callback();
      },
    });
    t = await createTestApp(createLogger('debug', sink));
    user = await t.signIn();
  });
  afterEach(() => t.close());

  const ownAccount = () =>
    t.backend.on('GET', `${URLS.accounts}/accounts/me`, { body: { id: IDS.account } });
  const CLOSE = `/accounts/${IDS.account}/close`;

  type Failure = ReturnType<typeof serviceProblem>;
  const routes = {
    register: (failure: Failure) => {
      t.backend.on('POST', `${URLS.customer}/customers`, failure);
      return user.post('/bff/customers').send({ name: 'Ana', phone: '11987654321' });
    },
    changePhone: (failure: Failure) => {
      t.backend.on('PUT', `${URLS.customer}/customers/me/phone`, failure);
      return user.put('/bff/me/phone').send({ phone: '11987654321' });
    },
    open: (failure: Failure) => {
      t.backend.on('POST', `${URLS.accounts}/accounts`, failure);
      return user.post('/bff/accounts');
    },
    close: (failure: Failure) => {
      ownAccount();
      t.backend.on('POST', `${URLS.accounts}${CLOSE}`, failure);
      return user.post('/bff/accounts/close');
    },
    home: (failure: Failure) => {
      t.backend.on('GET', `${URLS.accounts}/accounts/me`, failure);
      return user.get('/bff/home');
    },
    dailyLimit: (failure: Failure) => {
      ownAccount();
      t.backend
        .on('GET', `${URLS.transfers}/accounts/${IDS.account}/daily-limit`, failure)
        .on('GET', `${URLS.statement}/accounts/${IDS.account}/statement`, {
          body: { entries: [], nextCursor: null },
        });
      return user.get('/bff/home');
    },
    statement: (failure: Failure) => {
      ownAccount();
      t.backend.on('GET', `${URLS.statement}/accounts/${IDS.account}/statement`, failure);
      return user.get('/bff/statement');
    },
    status: (failure: Failure) => {
      t.backend.on('GET', `${URLS.transfers}/transfers/${IDS.transfer}`, failure);
      return user.get(`/bff/transfers/${IDS.transfer}`);
    },
    read: (failure: Failure) => {
      t.backend.on('POST', `${URLS.notification}/notifications/read`, failure);
      return user.post('/bff/notifications/read').send({ upTo: IDS.notification });
    },
    transfer: async (failure: Failure) => {
      ownAccount();
      t.backend
        .on('GET', `${URLS.customer}/customers/me`, { body: { id: IDS.customer } })
        .on('GET', `${URLS.customer}/customers/lookup?phone=%2B5521912345678`, {
          body: { customerId: IDS.otherCustomer, fullName: 'Bruno Lima' },
        })
        .on('GET', `${URLS.accounts}/accounts/recipient?customerId=${IDS.otherCustomer}`, {
          body: { accountId: IDS.otherAccount },
        })
        .on('POST', `${URLS.transfers}/transfers`, failure);
      const found = await user
        .get('/bff/transfers/recipient')
        .query({ phone: '21912345678' })
        .expect(200);
      return user
        .post('/bff/transfers')
        .set('Idempotency-Key', 'key-1')
        .send({ recipientToken: found.body.recipientToken, amount: 2500 });
    },
  };

  type Case = [keyof typeof routes, Failure, string];
  const cases: Case[] = [
    // customer
    [
      'register',
      serviceProblem(
        409,
        'phone-already-in-use',
        'Phone already in use',
        'Another customer already has this phone',
        '/customers',
      ),
      'Este celular já está em uso.',
    ],
    [
      'changePhone',
      serviceProblem(
        409,
        'phone-already-in-use',
        'Phone already in use',
        'Another customer already has this phone',
        '/customers/me/phone',
      ),
      'Este celular já está em uso.',
    ],
    [
      'changePhone',
      serviceProblem(
        404,
        'customer-not-found',
        'Customer not found',
        `customer ${IDS.customer} not found`,
        '/customers/me/phone',
      ),
      'Cadastro não encontrado. Complete seu cadastro.',
    ],
    [
      'register',
      serviceProblem(
        400,
        'validation-error',
        'Validation error',
        'The request has invalid fields',
        '/customers',
        { errors: [{ field: 'phone', message: 'must not be blank' }] },
      ),
      'Confira os dados informados.',
    ],
    // accounts
    [
      'home',
      serviceProblem(
        404,
        'account-not-found',
        'Account not found',
        `customer ${IDS.customer} has no open account`,
        '/accounts/me',
      ),
      'Conta não encontrada.',
    ],
    [
      'open',
      serviceProblem(
        409,
        'customer-already-has-account',
        'Customer already has an account',
        `customer ${IDS.customer} already has an account`,
        '/accounts',
      ),
      'Você já tem uma conta.',
    ],
    [
      'open',
      serviceProblem(
        422,
        'customer-not-found',
        'Customer not found',
        'The customer of the token is not registered',
        '/accounts',
      ),
      'Cadastro não encontrado. Complete seu cadastro.',
    ],
    [
      'open',
      serviceProblem(
        422,
        'customer-blocked',
        'Customer blocked',
        'The customer is blocked',
        '/accounts',
      ),
      'Seu cadastro está bloqueado para esta operação.',
    ],
    [
      'open',
      serviceProblem(
        503,
        'customers-unavailable',
        'Customer service unavailable',
        'The customer service could not be reached; nothing was written, retry later',
        '/accounts',
      ),
      UNAVAILABLE,
    ],
    [
      'open',
      serviceProblem(
        400,
        'missing-idempotency-key',
        'Missing Idempotency-Key',
        'The Idempotency-Key header is required',
        '/accounts',
      ),
      GENERIC,
    ],
    [
      'open',
      serviceProblem(
        403,
        'forbidden',
        'Forbidden',
        'Only a customer can access its own account',
        '/accounts',
      ),
      'Você não pode fazer esta operação.',
    ],
    [
      'close',
      serviceProblem(
        422,
        'balance-not-zero',
        'Balance not zero',
        'Only an account with a balance of exactly zero can be closed',
        CLOSE,
        { balance: 7500 },
      ),
      'Para encerrar a conta, o saldo precisa estar zerado.',
    ],
    [
      'close',
      serviceProblem(
        422,
        'pending-transfers',
        'Pending transfers',
        'The account has outgoing transfers still pending; retry when they finish',
        CLOSE,
        { count: 1 },
      ),
      'Há transferências em andamento. Tente encerrar quando elas terminarem.',
    ],
    [
      'close',
      serviceProblem(
        422,
        'account-closed',
        'Account closed',
        'The account is closed: it takes no entries and no changes',
        CLOSE,
      ),
      'Esta conta está encerrada.',
    ],
    [
      'close',
      serviceProblem(
        404,
        'account-not-found',
        'Account not found',
        `account ${IDS.account} not found`,
        CLOSE,
      ),
      'Conta não encontrada.',
    ],
    [
      'close',
      serviceProblem(
        503,
        'transfers-unavailable',
        'Transfer service unavailable',
        'The transfer service could not be reached; the account was not closed, retry later',
        CLOSE,
      ),
      UNAVAILABLE,
    ],
    [
      'close',
      serviceProblem(
        503,
        'account-busy',
        'Account busy',
        'The account is busy; retry shortly',
        CLOSE,
      ),
      'Sua conta está processando outra operação. Tente de novo em instantes.',
    ],
    // transfers
    [
      'transfer',
      serviceProblem(
        422,
        'account-not-found',
        'Account not found',
        `account ${IDS.otherAccount} not found`,
        '/transfers',
        { accountId: IDS.otherAccount },
      ),
      'Conta não encontrada.',
    ],
    [
      'transfer',
      serviceProblem(
        404,
        'account-not-found',
        'Account not found',
        `account ${IDS.account} not found`,
        '/transfers',
        { accountId: IDS.account },
      ),
      'Conta não encontrada.',
    ],
    [
      'transfer',
      serviceProblem(
        422,
        'same-account-transfer',
        'Same account transfer',
        'The source and the destination account must differ',
        '/transfers',
      ),
      'Não é possível transferir para você mesmo.',
    ],
    [
      'transfer',
      serviceProblem(
        422,
        'customer-blocked',
        'Customer blocked',
        'The customer that owns the source account is blocked and cannot transfer',
        '/transfers',
      ),
      'Seu cadastro está bloqueado para esta operação.',
    ],
    [
      'transfer',
      serviceProblem(
        422,
        'idempotency-key-reused',
        'Idempotency key reused',
        'The Idempotency-Key was already used for another transfer',
        '/transfers',
      ),
      'Este envio já foi feito com outros dados. Confira o extrato antes de tentar de novo.',
    ],
    // Without a transferId there is nothing to follow, so these two reach the screen as errors.
    [
      'transfer',
      serviceProblem(
        422,
        'insufficient-available-balance',
        'Insufficient available balance',
        'The available balance of the source account does not cover the transfer',
        '/transfers',
      ),
      'Saldo insuficiente para esta transferência.',
    ],
    [
      'transfer',
      serviceProblem(
        422,
        'transfer-failed',
        'Transfer failed',
        'The transfer could not be completed',
        '/transfers',
      ),
      'A transferência não pôde ser concluída.',
    ],
    [
      'transfer',
      serviceProblem(
        503,
        'accounts-unavailable',
        'Accounts unavailable',
        'The accounts service did not answer; retry shortly',
        '/transfers',
      ),
      UNAVAILABLE,
    ],
    [
      'transfer',
      serviceProblem(
        503,
        'customers-unavailable',
        'Customers unavailable',
        'The customer service did not answer; retry shortly',
        '/transfers',
      ),
      UNAVAILABLE,
    ],
    [
      'dailyLimit',
      serviceProblem(
        404,
        'account-not-found',
        'Account not found',
        `account ${IDS.account} not found`,
        `/accounts/${IDS.account}/daily-limit`,
        { accountId: IDS.account },
      ),
      'Conta não encontrada.',
    ],
    [
      'status',
      serviceProblem(
        404,
        'transfer-not-found',
        'Transfer not found',
        `transfer ${IDS.transfer} not found`,
        `/transfers/${IDS.transfer}`,
      ),
      'Transferência não encontrada.',
    ],
    // statement, notification and the security library
    [
      'statement',
      serviceProblem(
        400,
        'validation-error',
        'Validation error',
        'The request has invalid fields',
        `/accounts/${IDS.account}/statement`,
        { errors: [{ field: 'cursor', message: 'is invalid' }] },
      ),
      'Confira os dados informados.',
    ],
    [
      'read',
      serviceProblem(
        404,
        'notification-not-found',
        'Notification not found',
        'The notification was not found for this customer',
        '/notifications/read',
      ),
      'Notificação não encontrada.',
    ],
    [
      'read',
      serviceProblem(
        401,
        'unauthorized',
        'Unauthorized',
        'A valid bearer token is required',
        '/notifications/read',
      ),
      'Sessão expirada. Entre de novo.',
    ],
    [
      'statement',
      serviceProblem(
        500,
        'internal-error',
        'Internal error',
        'An unexpected error occurred',
        `/accounts/${IDS.account}/statement`,
      ),
      GENERIC,
    ],
  ];

  it.each(cases)('%s: %j -> "%s"', async (route, failure, message) => {
    const response = await routes[route](failure);
    expect(response.status).toBe(failure.status);
    // `detail` is the field the web reads; nothing else of the service's body goes on.
    expect(response.body).toEqual({
      type: failure.body.type,
      status: failure.status,
      detail: message,
    });
  });

  it.each([
    [
      serviceProblem(418, 'something-new', 'Something new', `account ${IDS.account}`, CLOSE),
      GENERIC,
    ],
    [serviceProblem(400, 'constructor', 'Crafted', 'prototype property', CLOSE), GENERIC],
    [
      {
        status: 503,
        body: { type: 'about:blank', title: 'Service Unavailable', status: 503, instance: CLOSE },
      },
      UNAVAILABLE,
    ],
    [{ status: 502, body: undefined }, GENERIC],
  ])('answers a fixed text, without the type, for the unknown %j', async (failure, message) => {
    const response = await routes.close(failure as Failure);
    expect(response.status).toBe(failure.status);
    expect(response.body).toEqual({ status: failure.status, detail: message });
  });

  it('keeps the status of the answer when the body claims another one', async () => {
    const response = await routes.close({
      ...serviceProblem(409, 'account-busy', 'Account busy', 'busy', CLOSE),
      status: 503,
    });
    expect(response.status).toBe(503);
    expect(response.body.status).toBe(503);
  });

  it('answers 503 unavailable when the service does not answer', async () => {
    t.backend.on('GET', `${URLS.accounts}/accounts/me`, () => {
      throw new TypeError('fetch failed');
    });
    const response = await user.get('/bff/statement').expect(503);
    expect(response.body).toEqual({ code: 'unavailable', message: UNAVAILABLE });
  });

  it('answers an unexpected error in Portuguese and logs it without its properties', async () => {
    const failure = Object.assign(new Error('store exploded'), {
      command: { args: ['stored-access-token'] },
    });
    vi.spyOn(t.store, 'get').mockRejectedValueOnce(failure);
    const response = await user.get('/bff/me');
    expect(response.status).toBe(500);
    expect(response.body).toEqual({ code: 'internal_error', message: GENERIC });
    expect(logs).toContain('unexpected error');
    expect(logs).toContain('store exploded');
    expect(logs).not.toContain('stored-access-token');
  });

  it('keeps code and message of the errors of the BFF itself', async () => {
    const response = await user.get('/bff/transfers/not-a-uuid').expect(400);
    expect(response.body).toEqual({ code: 'invalid_id', message: 'Transferência inválida.' });
  });

  it('answers an unknown route in Portuguese, without echoing it', async () => {
    const response = await user.get(`/bff/accounts/${IDS.otherAccount}`).expect(404);
    expect(response.body).toEqual({ code: 'not_found', message: 'Página não encontrada.' });
  });

  it('answers malformed JSON in Portuguese', async () => {
    const response = await request(t.app.getHttpServer())
      .post('/bff/auth/sign-in')
      .set('Content-Type', 'application/json')
      .send('{"email":')
      .expect(400);
    expect(response.body).toEqual({ code: 'invalid_request', message: 'Requisição inválida.' });
  });

  it('keeps the 413 of a body that is too large', async () => {
    const response = await request(t.app.getHttpServer())
      .post('/bff/auth/sign-in')
      .send({ email: 'a'.repeat(200_000) })
      .expect(413);
    expect(response.body).toEqual({ code: 'invalid_request', message: 'Requisição inválida.' });
  });
});
