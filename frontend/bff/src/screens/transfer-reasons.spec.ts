import { IDS, URLS } from '../testing/bodies.js';
import { createTestApp, type SignedIn, type TestApp } from '../testing/test-app.js';

const GENERIC = 'A transferência não pôde ser concluída.';

describe('reason of a refused or returned transfer', () => {
  let t: TestApp;
  let user: SignedIn;

  beforeEach(async () => {
    t = await createTestApp();
    user = await t.signIn();
  });
  afterEach(() => t.close());

  // Every value `Transfer.failureReason` can hold, with the status it ends in.
  it.each([
    ['FAILED', 'daily-limit-exceeded', 'Você atingiu o limite diário de transferências.'],
    [
      'FAILED',
      'daily-limit-unavailable',
      'Não foi possível conferir o limite diário agora. Tente de novo.',
    ],
    ['FAILED', 'insufficient-available-balance', 'Saldo insuficiente para esta transferência.'],
    [
      'FAILED',
      'debit-not-confirmed',
      'Não foi possível confirmar o débito. Nenhum valor saiu da sua conta.',
    ],
    ['FAILED', 'account-blocked', 'Sua conta está bloqueada e não pode enviar transferências.'],
    ['FAILED', 'account-closed', 'Uma das contas está encerrada.'],
    ['FAILED', 'account-not-found', 'Uma das contas não foi encontrada.'],
    ['COMPENSATED', 'account-closed', 'Uma das contas está encerrada.'],
    ['COMPENSATED', 'account-not-found', 'Uma das contas não foi encontrada.'],
    [
      'COMPENSATED',
      'credit-not-confirmed',
      'Não foi possível confirmar o crédito na conta de destino.',
    ],
    // The debit was found after the deadline: the saga returns the money and records no reason.
    [
      'COMPENSATED',
      null,
      'A transferência não pôde ser concluída e o valor voltou para a sua conta.',
    ],
    // Refusals with nothing to tell the customer: raw `type` or status of accounts.
    ['FAILED', 'entry-not-found', GENERIC],
    ['FAILED', 'validation-error', GENERIC],
    ['FAILED', 'idempotency-key-reused', GENERIC],
    ['FAILED', 'http-409', GENERIC],
    ['COMPENSATED', 'validation-error', GENERIC],
    ['FAILED', 'constructor', GENERIC],
    ['COMPLETED', null, null],
    ['PENDING', null, null],
  ])('%s with %s says "%s"', async (status, failureReason, reason) => {
    t.backend.on('GET', `${URLS.transfers}/transfers/${IDS.transfer}`, {
      body: {
        id: IDS.transfer,
        sourceAccountId: IDS.account,
        destinationAccountId: IDS.otherAccount,
        amount: 2500,
        description: null,
        status,
        failureReason,
        createdAt: '2026-11-22T10:00:00Z',
        updatedAt: '2026-11-22T10:00:01Z',
      },
    });
    const response = await user.get(`/bff/transfers/${IDS.transfer}`).expect(200);
    expect(response.body).toEqual({ id: IDS.transfer, status, reason, reasonCode: failureReason });
  });
});
