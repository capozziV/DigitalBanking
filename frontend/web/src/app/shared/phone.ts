import { AbstractControl, ValidationErrors } from '@angular/forms';

export const phoneDigits = (value: string): string => value.replace(/\D/g, '');

/** Applies the mobile mask `(11) 98765-4321` progressively while the user types. */
export function formatPhone(value: string): string {
  const digits = phoneDigits(value).slice(0, 11);
  if (digits.length === 0) return '';
  if (digits.length <= 2) return `(${digits}`;
  if (digits.length <= 7) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
  return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
}

/** Shows the phone the BFF stores (`+5511987654321`) with the mask `(11) 98765-4321`. */
export function displayPhone(stored: string): string {
  const digits = phoneDigits(stored);
  return formatPhone(digits.length === 13 && digits.startsWith('55') ? digits.slice(2) : digits);
}

/** Brazilian mobile: two-digit area code, then a 9 and eight more digits. */
export const mobilePhone = (control: AbstractControl): ValidationErrors | null =>
  /^[1-9]\d9\d{8}$/.test(phoneDigits(String(control.value ?? ''))) ? null : { mobilePhone: true };
