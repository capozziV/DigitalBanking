import { describe, expect, it } from 'vitest';
import { displayPhone, formatPhone, isMobilePhone, phoneDigits } from './phone';

describe('phone', () => {
  it('masks progressively', () => {
    expect(formatPhone('')).toBe('');
    expect(formatPhone('1')).toBe('(1');
    expect(formatPhone('119')).toBe('(11) 9');
    expect(formatPhone('1198765')).toBe('(11) 98765');
    expect(formatPhone('11987654321')).toBe('(11) 98765-4321');
  });

  it('ignores non digits and extra digits', () => {
    expect(formatPhone('(11) 98765-4321999')).toBe('(11) 98765-4321');
    expect(phoneDigits('(11) 98765-4321')).toBe('11987654321');
  });

  it('validates a Brazilian mobile number', () => {
    expect(isMobilePhone('(11) 98765-4321')).toBe(true);
    expect(isMobilePhone('(11) 8765-4321')).toBe(false);
    expect(isMobilePhone('(11) 88765-4321')).toBe(false);
    expect(isMobilePhone('(11) 98765-432')).toBe(false);
    expect(isMobilePhone('')).toBe(false);
  });

  it('shows the stored phone with the mask', () => {
    expect(displayPhone('+5511987654321')).toBe('(11) 98765-4321');
    expect(displayPhone('11987654321')).toBe('(11) 98765-4321');
  });
});
