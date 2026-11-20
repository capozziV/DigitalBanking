import { expect, type Page } from '@playwright/test';

/** Demo user created by infra/local/seed.sh (DEMO_EMAIL, DEMO_PASSWORD and DEMO_PHONE of infra/local/.env.example). */
export const DEMO = {
  email: process.env['DEMO_EMAIL'] ?? 'demo@example.test',
  password: process.env['DEMO_PASSWORD'] ?? '123456',
  phone: process.env['DEMO_PHONE'] ?? '+5511999990001',
  fullName: 'Demo capzzPay',
};

/** The demo phone as typed in the masked field: DDD and number, without +55. */
export const DEMO_PHONE_DIGITS = DEMO.phone.replace(/^\+55/, '');

const unique = () => `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`;

/** A new e-mail per call, so parallel specs and repeated runs never collide. */
export function uniqueEmail(): string {
  return `e2e-${unique()}@example.test`;
}

/**
 * A new Brazilian mobile number per call, as the 11 digits the form masks: DDD 11, a 9, then 8 random digits. The first
 * of the 8 is never 9, so it cannot hit the seed numbers (+55 11 9999-9000x).
 */
export function uniquePhoneDigits(): string {
  const first = 1 + Math.floor(Math.random() * 8);
  const rest = Math.floor(Math.random() * 10_000_000)
    .toString()
    .padStart(7, '0');
  return `119${first}${rest}`;
}

/** Creates the account on "Criar conta" (local identity needs no e-mail code) and lands on "Completar cadastro". */
export async function signUp(page: Page, email: string, password = '246810'): Promise<void> {
  await page.goto('/criar-conta');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha (6 dígitos)').fill(password);
  await page.getByLabel('Confirme a senha').fill(password);
  await page.getByRole('button', { name: 'Continuar' }).click();
  await expect(page).toHaveURL(/\/completar-cadastro$/);
}

/** Fills "Completar cadastro" and lands on "Início". */
export async function completeProfile(
  page: Page,
  name: string,
  phoneDigits: string,
): Promise<void> {
  await page.getByLabel('Nome completo').fill(name);
  await page.getByLabel('Celular').fill(phoneDigits);
  await page.getByRole('button', { name: 'Concluir' }).click();
  await expect(page).toHaveURL(/\/inicio$/);
}

export async function signIn(page: Page, email: string, password: string): Promise<void> {
  await page.goto('/entrar');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha (6 dígitos)').fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/inicio$/);
}

/** Opens the account on "Início" and waits for the welcome bonus balance. */
export async function openAccount(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Abrir conta' }).click();
  await expect(page.getByText('Saldo disponível')).toBeVisible();
}

/**
 * Statement and notices are projected from events, so they may lag the screen that caused them: reloads the page until
 * the check passes.
 */
export async function eventually(page: Page, check: () => Promise<void>): Promise<void> {
  let attempts = 0;
  await expect(async () => {
    // The first attempt looks at the page as it is; a reload before it would cut a pending navigation short.
    if (attempts++ > 0) await page.reload();
    await check();
  }).toPass({ timeout: 15_000, intervals: [500, 1_000, 2_000, 5_000] });
}

/** Runs the first three steps of "Transferir" (phone, amount, confirm); the caller asserts the status step. */
export async function transfer(
  page: Page,
  phone: string,
  amount: string,
  expectedRecipient: string,
): Promise<void> {
  await page.getByRole('link', { name: 'Transferir', exact: true }).first().click();
  await page.getByLabel('Celular do destinatário').fill(phone);
  await page.getByRole('button', { name: 'Próximo' }).click();
  await expect(page.getByText(expectedRecipient)).toBeVisible();
  await page.getByLabel('Valor (R$)').fill(amount);
  await page.getByRole('button', { name: 'Continuar' }).click();
  await page.getByRole('button', { name: 'Confirmar' }).click();
}
