import { FormControl } from '@angular/forms';
import { formatPhone, mobilePhone, phoneDigits } from './phone';

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
    expect(mobilePhone(new FormControl('(11) 98765-4321'))).toBeNull();
    expect(mobilePhone(new FormControl('(11) 8765-4321'))).toEqual({ mobilePhone: true });
    expect(mobilePhone(new FormControl('(11) 88765-4321'))).toEqual({ mobilePhone: true });
    expect(mobilePhone(new FormControl('(11) 98765-432'))).toEqual({ mobilePhone: true });
  });
});
