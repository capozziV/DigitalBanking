import { loadConfig } from '../config/config.js';
import { runWithCorrelationId } from '../observability/correlation.js';
import { BackendClient, BackendProblemError, BackendUnavailableError } from './backend-client.js';

const config = loadConfig({
  IDENTITY_PROVIDER: 'local',
  ACCOUNTS_URL: 'http://accounts.test:8081',
  TRANSFERS_URL: 'http://transfers.test:8082',
  CUSTOMER_URL: 'http://customer.test:8085',
  STATEMENT_URL: 'http://statement.test:8084',
  NOTIFICATION_URL: 'http://notification.test:8083',
});

function jsonResponse(status: number, body: unknown, contentType = 'application/json'): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'content-type': contentType } });
}

describe('BackendClient', () => {
  let fetchMock: ReturnType<typeof vi.fn>;
  let client: BackendClient;

  beforeEach(() => {
    fetchMock = vi.fn();
    client = new BackendClient(config, fetchMock as unknown as typeof fetch);
  });

  it('sends Bearer token and the correlation id of the current request', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { ok: true }));

    const body = await runWithCorrelationId('corr-9', () =>
      client.get('accounts', '/accounts/me', { token: 'tok' }),
    );

    expect(body).toEqual({ ok: true });
    const [url, init] = fetchMock.mock.calls[0]!;
    expect(url).toBe('http://accounts.test:8081/accounts/me');
    expect(init.method).toBe('GET');
    expect(init.headers['Authorization']).toBe('Bearer tok');
    expect(init.headers['X-Correlation-ID']).toBe('corr-9');
  });

  it('routes each service to its own base URL', async () => {
    fetchMock.mockImplementation(async () => jsonResponse(200, {}));
    await client.get('statement', '/s', { token: 't' });
    await client.get('transfers', '/t', { token: 't' });
    await client.get('customer', '/c', { token: 't' });
    await client.get('notification', '/n', { token: 't' });
    expect(fetchMock.mock.calls.map((c) => c[0])).toEqual([
      'http://statement.test:8084/s',
      'http://transfers.test:8082/t',
      'http://customer.test:8085/c',
      'http://notification.test:8083/n',
    ]);
  });

  it('posts JSON with the Idempotency-Key it was given', async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, { id: 't1' }));

    const body = await client.post(
      'transfers',
      '/transfers',
      { amount: 1000 },
      { token: 'tok', idempotencyKey: 'key-1' },
    );

    expect(body).toEqual({ id: 't1' });
    const [, init] = fetchMock.mock.calls[0]!;
    expect(init.method).toBe('POST');
    expect(init.headers['Idempotency-Key']).toBe('key-1');
    expect(init.headers['Content-Type']).toBe('application/json');
    expect(init.body).toBe('{"amount":1000}');
  });

  it('supports put and returns undefined on 204', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    await expect(
      client.put('customer', '/customers/me/phone', { phone: '+5511987654321' }, { token: 't' }),
    ).resolves.toBeUndefined();
    expect(fetchMock.mock.calls[0]![1].method).toBe('PUT');
  });

  it('converts Problem Details into a typed error', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        422,
        { type: 'urn:problem:customer-blocked', title: 'Blocked', status: 422, detail: 'no' },
        'application/problem+json',
      ),
    );

    const error = await client.post('transfers', '/transfers', {}, { token: 't' }).catch((e) => e);

    expect(error).toBeInstanceOf(BackendProblemError);
    expect(error.status).toBe(422);
    expect(error.problem.type).toBe('urn:problem:customer-blocked');
    expect(error.problem.detail).toBe('no');
  });

  it('builds a problem when the error body is not JSON', async () => {
    fetchMock.mockResolvedValue(new Response('boom', { status: 502 }));
    const error = await client.get('accounts', '/x', { token: 't' }).catch((e) => e);
    expect(error).toBeInstanceOf(BackendProblemError);
    expect(error.status).toBe(502);
  });

  it('turns network failures into BackendUnavailableError', async () => {
    fetchMock.mockRejectedValue(new TypeError('fetch failed'));
    await expect(client.get('accounts', '/x', { token: 't' })).rejects.toBeInstanceOf(
      BackendUnavailableError,
    );
  });
});
