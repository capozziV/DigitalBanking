import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { BffError, bff, onSessionLost, onUnavailable } from './bff-client';
import { FakeBff } from './test-support';

describe('bff', () => {
  let fake: FakeBff;
  const sessionLost = vi.fn();
  const unavailable = vi.fn();
  let offLost: () => void;
  let offUnavailable: () => void;

  beforeEach(() => {
    fake = new FakeBff().install();
    sessionLost.mockClear();
    unavailable.mockClear();
    offLost = onSessionLost(sessionLost);
    offUnavailable = onUnavailable(unavailable);
    document.cookie = 'bank_csrf=token-123; path=/';
  });

  afterEach(() => {
    offLost();
    offUnavailable();
    document.cookie = 'bank_csrf=; path=/; max-age=0';
    vi.unstubAllGlobals();
  });

  it('adds X-CSRF-Token from bank_csrf on POST', async () => {
    const promise = bff('/bff/transfers', { method: 'POST', body: { amount: 2500 } });
    const call = fake.only('/bff/transfers');
    expect(call.headers.get('X-CSRF-Token')).toBe('token-123');
    expect(call.method).toBe('POST');
    expect(call.body).toEqual({ amount: 2500 });
    expect(call.headers.get('Content-Type')).toBe('application/json');
    expect(call.credentials).toBe('same-origin');
    call.respond(200, {});
    await promise;
  });

  it('does not add it on GET', async () => {
    const promise = bff('/bff/home');
    const call = fake.only('/bff/home');
    expect(call.headers.has('X-CSRF-Token')).toBe(false);
    expect(call.credentials).toBe('same-origin');
    call.respond(200, {});
    await promise;
  });

  it('does not add it outside /bff/', async () => {
    const promise = bff('https://other.example/x' as `/bff/${string}`, { method: 'POST' });
    const call = fake.only('https://other.example/x');
    expect(call.headers.has('X-CSRF-Token')).toBe(false);
    call.respond(200, {});
    await promise;
  });

  it('decodes the cookie value', async () => {
    document.cookie = 'bank_csrf=a%2Fb; path=/';
    const promise = bff('/bff/x', { method: 'DELETE' });
    const call = fake.only('/bff/x');
    expect(call.headers.get('X-CSRF-Token')).toBe('a/b');
    call.respond(204);
    await promise;
  });

  it('sends custom headers such as Idempotency-Key', async () => {
    const promise = bff('/bff/transfers', { method: 'POST', headers: { 'Idempotency-Key': 'k1' } });
    const call = fake.only('/bff/transfers');
    expect(call.headers.get('Idempotency-Key')).toBe('k1');
    call.respond(200, {});
    await promise;
  });

  it('returns the parsed JSON body', async () => {
    const promise = bff<{ count: number }>('/bff/notifications/unread-count');
    fake.only('/bff/notifications/unread-count').respond(200, { count: 3 });
    expect(await promise).toEqual({ count: 3 });
  });

  it('returns undefined when the answer has no body', async () => {
    const promise = bff('/bff/notifications/read', { method: 'POST' });
    fake.only('/bff/notifications/read').respond(204);
    expect(await promise).toBeUndefined();
  });

  it('throws BffError with the status and the problem details', async () => {
    const promise = bff('/bff/transfers', { method: 'POST' });
    fake
      .only('/bff/transfers')
      .respond(422, { title: 'Refused', detail: 'No balance', status: 422 });
    const error = await promise.catch((e: unknown) => e);
    expect(error).toBeInstanceOf(BffError);
    expect((error as BffError).status).toBe(422);
    expect((error as BffError).problem).toEqual({
      title: 'Refused',
      detail: 'No balance',
      status: 422,
    });
    expect(sessionLost).not.toHaveBeenCalled();
    expect(unavailable).not.toHaveBeenCalled();
  });

  it('throws BffError with a null problem when the body is not JSON', async () => {
    const promise = bff('/bff/home');
    const call = fake.only('/bff/home');
    call.respond(404, undefined);
    const error = (await promise.catch((e: unknown) => e)) as BffError;
    expect(error).toBeInstanceOf(BffError);
    expect(error.status).toBe(404);
    expect(error.problem).toBeNull();
  });

  it('401 outside /bff/auth notifies session lost once', async () => {
    const promise = bff('/bff/home');
    fake.only('/bff/home').respond(401, {});
    await expect(promise).rejects.toBeInstanceOf(BffError);
    expect(sessionLost).toHaveBeenCalledTimes(1);
    expect(unavailable).not.toHaveBeenCalled();
  });

  it('401 on /bff/auth/sign-in does not', async () => {
    const promise = bff('/bff/auth/sign-in', { method: 'POST', body: {} });
    fake.only('/bff/auth/sign-in').respond(401, {});
    const error = (await promise.catch((e: unknown) => e)) as BffError;
    expect(error.status).toBe(401);
    expect(sessionLost).not.toHaveBeenCalled();
  });

  it('network error notifies unavailable', async () => {
    const promise = bff('/bff/home');
    fake.only('/bff/home').fail();
    const error = (await promise.catch((e: unknown) => e)) as BffError;
    expect(error).toBeInstanceOf(BffError);
    expect(error.status).toBe(0);
    expect(unavailable).toHaveBeenCalledTimes(1);
    expect(sessionLost).not.toHaveBeenCalled();
  });

  it.each([502, 503, 504])('%d notifies unavailable', async (status) => {
    const promise = bff('/bff/home');
    fake.only('/bff/home').respond(status, {});
    await expect(promise).rejects.toMatchObject({ status });
    expect(unavailable).toHaveBeenCalledTimes(1);
  });

  it('500 does not notify unavailable', async () => {
    const promise = bff('/bff/home');
    fake.only('/bff/home').respond(500, {});
    await expect(promise).rejects.toMatchObject({ status: 500 });
    expect(unavailable).not.toHaveBeenCalled();
  });

  it('stops notifying after the listener is removed', async () => {
    offLost();
    const promise = bff('/bff/home');
    fake.only('/bff/home').respond(401, {});
    await expect(promise).rejects.toBeInstanceOf(BffError);
    expect(sessionLost).not.toHaveBeenCalled();
  });
});
