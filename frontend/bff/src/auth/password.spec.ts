import { isValidPassword } from './password.js';

describe('isValidPassword', () => {
  it.each(['123456', '000000', '987654'])('accepts %s', (password) => {
    expect(isValidPassword(password)).toBe(true);
  });

  it.each(['12345', '1234567', '12a456', '', ' 123456', '123456 ', '123456\n', '１２３４５６'])(
    'rejects %j',
    (password) => {
      expect(isValidPassword(password)).toBe(false);
    },
  );

  it.each([123456, null, undefined, {}, ['123456']])('rejects non-string %j', (password) => {
    expect(isValidPassword(password)).toBe(false);
  });
});
