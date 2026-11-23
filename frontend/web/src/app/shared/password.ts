import { AbstractControl, ValidationErrors } from '@angular/forms';

/** The bank password is exactly six digits (a deliberate, documented weak choice: simulated bank). */
export const sixDigitPassword = (control: AbstractControl): ValidationErrors | null =>
  /^\d{6}$/.test(String(control.value ?? '')) ? null : { sixDigits: true };

/** Group validator: `password` and `confirmation` must be equal. */
export const passwordsMatch = (group: AbstractControl): ValidationErrors | null =>
  group.get('password')?.value === group.get('confirmation')?.value ? null : { mismatch: true };
