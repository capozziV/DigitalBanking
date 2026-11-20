import { normalizePhone } from './phone.js';

describe('normalizePhone', () => {
  it.each([
    '(11) 98765-4321',
    '11987654321',
    '+55 11 98765-4321',
    '+5511987654321',
    '55 (11) 98765-4321',
    ' 11 9 8765 4321 ',
  ])('turns %s into E.164', (input) => {
    expect(normalizePhone(input)).toBe('+5511987654321');
  });

  it.each([
    '1234',
    '',
    '1198765432', // landline-length
    '11887654321', // does not start with 9 after the area code
    '0198765-4321', // area code starting with 0
    '+1 415 555 2671', // another country
    '11987654321999',
    'abc',
  ])('rejects %j', (input) => {
    expect(() => normalizePhone(input)).toThrow(/celular/);
  });

  it('rejects a value that is not a string', () => {
    expect(() => normalizePhone(11987654321)).toThrow(/celular/);
    expect(() => normalizePhone(undefined)).toThrow(/celular/);
  });
});
