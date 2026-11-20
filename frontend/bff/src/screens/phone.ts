import { BadRequestException } from '@nestjs/common';

/** Brazilian mobile number in E.164, the format the customer service validates and stores. */
const E164_MOBILE = /^\+55[1-9][0-9]9[0-9]{8}$/;
/** Only what a person types in a phone field: digits, spaces, parentheses, dashes, dots and a leading plus. */
const TYPED = /^\+?[\d\s().-]+$/;

export function invalidPhone(): BadRequestException {
  return new BadRequestException({
    code: 'invalid_phone',
    message: 'Informe um celular válido com DDD, por exemplo (11) 98765-4321.',
  });
}

/** Turns what the person typed (`(11) 98765-4321`, `11987654321`, `+55 11 98765-4321`) into E.164, or throws 400. */
export function normalizePhone(input: unknown): string {
  if (typeof input !== 'string' || input.length > 40 || !TYPED.test(input.trim())) {
    throw invalidPhone();
  }
  const digits = input.replace(/\D/g, '');
  // 13 digits starting with 55 carry the country code; 11 digits are DDD + number (55 is also a valid DDD).
  const e164 =
    digits.length === 13 && digits.startsWith('55')
      ? `+${digits}`
      : digits.length === 11
        ? `+55${digits}`
        : '';
  if (!E164_MOBILE.test(e164)) throw invalidPhone();
  return e164;
}
