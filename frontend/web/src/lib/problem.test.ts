import { describe, expect, it } from 'vitest';
import { BffError } from './bff-client';
import { problemMessage } from './problem';

describe('problemMessage', () => {
  it('uses the Problem Details detail', () => {
    const error = new BffError(422, { detail: 'Saldo insuficiente', title: 'Recusada' });
    expect(problemMessage(error, 'fallback')).toBe('Saldo insuficiente');
  });

  it('uses the BFF own message when there is no detail', () => {
    const error = new BffError(409, { code: 'CUSTOMER_EXISTS', message: 'Já cadastrado' } as never);
    expect(problemMessage(error, 'fallback')).toBe('Já cadastrado');
  });

  it('prefers the detail over the message', () => {
    const error = new BffError(409, { detail: 'D', message: 'M' } as never);
    expect(problemMessage(error, 'fallback')).toBe('D');
  });

  it('falls back when the body has neither', () => {
    expect(problemMessage(new BffError(500, { title: 'Erro' }), 'fallback')).toBe('fallback');
    expect(problemMessage(new BffError(500, null), 'fallback')).toBe('fallback');
    expect(problemMessage(new BffError(500, { detail: '' }), 'fallback')).toBe('fallback');
  });

  it('falls back for an error that is not from the BFF', () => {
    expect(problemMessage(new Error('boom'), 'fallback')).toBe('fallback');
    expect(problemMessage(undefined, 'fallback')).toBe('fallback');
  });
});
