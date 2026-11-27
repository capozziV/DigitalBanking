import { describe, expect, it } from 'vitest';
import { isSixDigitPassword, passwordsMatch } from './password';

describe('isSixDigitPassword', () => {
  it('accepts exactly six digits', () => {
    expect(isSixDigitPassword('123456')).toBe(true);
  });

  it('rejects five digits', () => {
    expect(isSixDigitPassword('12345')).toBe(false);
  });

  it('rejects seven digits', () => {
    expect(isSixDigitPassword('1234567')).toBe(false);
  });

  it('rejects letters', () => {
    expect(isSixDigitPassword('12a456')).toBe(false);
  });

  it('rejects empty', () => {
    expect(isSixDigitPassword('')).toBe(false);
  });

  it('rejects a trailing line break', () => {
    expect(isSixDigitPassword('123456\n')).toBe(false);
  });
});

describe('passwordsMatch', () => {
  it('passes when equal', () => {
    expect(passwordsMatch('123456', '123456')).toBe(true);
  });

  it('fails when different', () => {
    expect(passwordsMatch('123456', '654321')).toBe(false);
  });
});
