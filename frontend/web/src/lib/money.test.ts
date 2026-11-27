import { describe, expect, it } from 'vitest';
import { appendDigit, dropDigit, formatCents, parseAmountToCents } from './money';

const NBSP = ' ';

describe('parseAmountToCents', () => {
  it.each([
    ['10', 1000],
    ['10,5', 1050],
    ['10,50', 1050],
    ['1.234,56', 123456],
    ['0,01', 1],
    ['R$ 25,00', 2500],
    ['  7  ', 700],
  ])('reads %s as %d cents', (text, cents) => {
    expect(parseAmountToCents(text)).toBe(cents);
  });

  it.each([
    '',
    'abc',
    '0',
    '0,00',
    '-5',
    '1,234',
    '1,2,3',
    '10,',
    ',50',
    '1.23',
    '99999999999999999',
  ])('refuses %s', (text) => {
    expect(parseAmountToCents(text)).toBeNull();
  });

  it('never goes through floating point', () => {
    expect(parseAmountToCents('1.005,99')).toBe(100599);
    expect(parseAmountToCents('0,29')).toBe(29);
  });
});

describe('formatCents', () => {
  const brl = (text: string) => text.replace(' ', NBSP);

  it('formatCents 1005 is "R$ 10,05"', () => {
    expect(formatCents(1005)).toBe(brl('R$ 10,05'));
  });

  it('formats cents as reais with thousands separator', () => {
    expect(formatCents(123456)).toBe(brl('R$ 1.234,56'));
  });

  it('puts the sign before the currency symbol', () => {
    expect(formatCents(-500)).toBe(brl('-R$ 5,00'));
  });

  it('pads cents below one real', () => {
    expect(formatCents(5)).toBe(brl('R$ 0,05'));
    expect(formatCents(0)).toBe(brl('R$ 0,00'));
  });

  it('groups millions', () => {
    expect(formatCents(100000000)).toBe(brl('R$ 1.000.000,00'));
  });

  it('does not lose cents on a very large amount', () => {
    expect(formatCents(Number.MAX_SAFE_INTEGER)).toBe(brl('R$ 90.071.992.547.409,91'));
  });

  it('shows a dash for missing or fractional values', () => {
    expect(formatCents(null)).toBe('—');
    expect(formatCents(undefined)).toBe('—');
    expect(formatCents(10.5)).toBe('—');
  });
});

describe('numeric keypad', () => {
  it('appendDigit 1,0,0,5 gives 1005', () => {
    const cents = [1, 0, 0, 5].reduce(appendDigit, 0);
    expect(cents).toBe(1005);
  });

  it('grows from the right: 0 -> 1 -> 10 -> 100', () => {
    expect(appendDigit(0, 1)).toBe(1);
    expect(appendDigit(1, 0)).toBe(10);
    expect(appendDigit(10, 0)).toBe(100);
  });

  it('keeps zero when only zeros are typed', () => {
    expect(appendDigit(0, 0)).toBe(0);
  });

  it('dropDigit 1005 gives 100', () => {
    expect(dropDigit(1005)).toBe(100);
    expect(dropDigit(5)).toBe(0);
    expect(dropDigit(0)).toBe(0);
  });

  it('does not grow past 99 999 999 999 cents', () => {
    expect(appendDigit(9_999_999_999, 9)).toBe(99_999_999_999);
    expect(appendDigit(99_999_999_999, 1)).toBe(99_999_999_999);
    expect(appendDigit(Number.MAX_SAFE_INTEGER, 1)).toBe(Number.MAX_SAFE_INTEGER);
  });

  it('ignores something that is not a digit', () => {
    expect(appendDigit(10, 12)).toBe(10);
    expect(appendDigit(10, -1)).toBe(10);
    expect(appendDigit(10, 1.5)).toBe(10);
  });
});
