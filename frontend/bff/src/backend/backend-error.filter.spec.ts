import { BackendErrorFilter } from './backend-error.filter.js';
import { BackendProblemError, BackendUnavailableError } from './backend-client.js';

function run(error: BackendProblemError | BackendUnavailableError) {
  const sent: { status?: number; body?: unknown } = {};
  const response = {
    status(code: number) {
      sent.status = code;
      return this;
    },
    json(body: unknown) {
      sent.body = body;
    },
  };
  const host = { switchToHttp: () => ({ getResponse: () => response }) };
  new BackendErrorFilter().catch(error, host as never);
  return sent;
}

describe('BackendErrorFilter', () => {
  it('forwards only the allowlisted Problem Details fields', () => {
    const sent = run(
      new BackendProblemError(422, {
        type: 'https://bank/problems/insufficient-funds',
        title: 'Insufficient funds',
        status: 422,
        detail: 'Not enough balance',
        instance: '/internal/accounts/42/debit',
        stackTrace: 'at secret',
        errors: [{ field: 'amount', message: 'must be positive' }],
      }),
    );
    expect(sent.status).toBe(422);
    expect(sent.body).toEqual({
      type: 'https://bank/problems/insufficient-funds',
      title: 'Insufficient funds',
      status: 422,
      detail: 'Not enough balance',
      errors: [{ field: 'amount', message: 'must be positive' }],
    });
  });

  it('omits errors when the backend sent none', () => {
    const sent = run(new BackendProblemError(404, { title: 'Not found', instance: '/x' }));
    expect(sent.body).toEqual({ title: 'Not found' });
  });

  it('answers 503 when the backend is unavailable', () => {
    const sent = run(new BackendUnavailableError('accounts'));
    expect(sent.status).toBe(503);
  });
});
