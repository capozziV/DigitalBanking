/** The bank password is exactly six digits (a deliberate, documented weak choice: simulated bank). */
export const isSixDigitPassword = (value: string): boolean => /^\d{6}$/.test(value);

export const passwordsMatch = (a: string, b: string): boolean => a === b;
