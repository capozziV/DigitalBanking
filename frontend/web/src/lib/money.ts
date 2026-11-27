const MAX_CENTS = Number.MAX_SAFE_INTEGER;
/** The numeric keypad stops growing here (R$ 999.999.999,99). */
const MAX_KEYPAD_CENTS = 99_999_999_999;

const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/**
 * Reads what the user typed (`10`, `10,5`, `1.234,56`, `R$ 25,00`) as an integer number of cents.
 * Works on the string only, never through floating point. Returns `null` when it is not a
 * positive amount with at most two decimal places.
 */
export function parseAmountToCents(text: string): number | null {
  const cleaned = text.replace(/^\s*R\$/, '').trim();
  const match = /^(\d{1,3}(?:\.\d{3})+|\d+)(?:,(\d{1,2}))?$/.exec(cleaned);
  if (!match) return null;
  const reais = match[1].replaceAll('.', '');
  const centsPart = (match[2] ?? '').padEnd(2, '0');
  const cents = Number(reais + centsPart);
  if (!Number.isSafeInteger(cents) || cents <= 0 || cents > MAX_CENTS) return null;
  return cents;
}

/** Display only: `123456` -> `R$ 1.234,56`. A missing or fractional value shows a dash. */
export function formatCents(cents: number | null | undefined): string {
  if (cents === null || cents === undefined || !Number.isSafeInteger(cents)) return '—';
  const sign = cents < 0 ? '-' : '';
  const absolute = Math.abs(cents);
  const reais = Math.floor(absolute / 100);
  const rest = (absolute % 100).toString().padStart(2, '0');
  // A decimal string keeps every cent exact, where dividing by 100 would not.
  return BRL.format(`${sign}${reais}.${rest}` as `${number}`);
}

/** Numeric keypad: the amount grows from the right (0 -> 1 -> 10 -> 100 cents). */
export function appendDigit(cents: number, digit: number): number {
  if (!Number.isInteger(digit) || digit < 0 || digit > 9) return cents;
  const next = cents * 10 + digit;
  return next > MAX_KEYPAD_CENTS ? cents : next;
}

export function dropDigit(cents: number): number {
  return Math.floor(cents / 10);
}
