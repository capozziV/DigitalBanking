const PASSWORD = /^\d{6}$/;

export function isValidPassword(value: unknown): value is string {
  return typeof value === 'string' && PASSWORD.test(value);
}
