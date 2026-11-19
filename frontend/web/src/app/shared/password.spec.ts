import { FormControl, FormGroup } from '@angular/forms';
import { passwordsMatch, sixDigitPassword } from './password';

describe('sixDigitPassword', () => {
  const validate = (value: string) => sixDigitPassword(new FormControl(value));

  it('accepts exactly six digits', () => {
    expect(validate('123456')).toBeNull();
  });

  it('rejects five digits', () => {
    expect(validate('12345')).toEqual({ sixDigits: true });
  });

  it('rejects seven digits', () => {
    expect(validate('1234567')).toEqual({ sixDigits: true });
  });

  it('rejects letters', () => {
    expect(validate('12a456')).toEqual({ sixDigits: true });
  });

  it('rejects empty', () => {
    expect(validate('')).toEqual({ sixDigits: true });
  });
});

describe('passwordsMatch', () => {
  const group = (password: string, confirmation: string) =>
    new FormGroup({
      password: new FormControl(password),
      confirmation: new FormControl(confirmation),
    });

  it('passes when equal', () => {
    expect(passwordsMatch(group('123456', '123456'))).toBeNull();
  });

  it('fails when different', () => {
    expect(passwordsMatch(group('123456', '654321'))).toEqual({ mismatch: true });
  });
});
