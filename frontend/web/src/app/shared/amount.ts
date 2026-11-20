const MAX_CENTS = Number.MAX_SAFE_INTEGER;

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
