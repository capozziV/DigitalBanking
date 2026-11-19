import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('formats cents as reais with thousands separator', () => {
    expect(pipe.transform(123456)).toBe('R$ 1.234,56');
  });

  it('puts the sign before the currency symbol', () => {
    expect(pipe.transform(-500)).toBe('-R$ 5,00');
  });

  it('pads cents below one real', () => {
    expect(pipe.transform(5)).toBe('R$ 0,05');
    expect(pipe.transform(0)).toBe('R$ 0,00');
  });

  it('groups millions', () => {
    expect(pipe.transform(100000000)).toBe('R$ 1.000.000,00');
  });

  it('shows a dash for missing or fractional values', () => {
    expect(pipe.transform(null)).toBe('—');
    expect(pipe.transform(undefined)).toBe('—');
    expect(pipe.transform(10.5)).toBe('—');
  });
});
