import { parseAmountToCents } from './amount';

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
