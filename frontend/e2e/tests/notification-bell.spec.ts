import { expect, test } from '@playwright/test';
import {
  completeProfile,
  eventually,
  openAccount,
  signUp,
  uniqueEmail,
  uniquePhoneDigits,
} from './support';

const UUID = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i;

test('the bell shows the unread counter, zeroes it when opened and no text carries an id', async ({
  page,
}) => {
  await signUp(page, uniqueEmail());
  await completeProfile(page, 'Cliente Sininho E2E', uniquePhoneDigits());
  await openAccount(page);

  const bell = page.locator('app-notification-bell button.bell');
  // The welcome notification is projected from events: reload until the counter shows it.
  await eventually(page, () => expect(bell.locator('.bell-count')).toBeVisible({ timeout: 2_000 }));

  await bell.click();
  const panel = page.getByRole('dialog', { name: 'Notificações' });
  await expect(panel.locator('li').first()).toBeVisible();
  await expect(bell.locator('.bell-count')).toHaveCount(0);

  const texts = await panel.locator('li').allTextContents();
  expect(texts.length).toBeGreaterThan(0);
  for (const text of texts) expect(text).not.toMatch(UUID);

  await page.keyboard.press('Escape');
  await expect(panel).toHaveCount(0);
  await expect(bell).toBeFocused();
});
