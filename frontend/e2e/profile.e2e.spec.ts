import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './utils/auth';

test.describe('Profile edit persists', () => {
  test('updates profile and sees persisted values after reload', async ({ page }) => {
    await loginAsAdmin(page);

    await page.goto('/profile');
    await expect(page.locator('text=Profile')).toBeVisible();

    const newName = `Admin QA ${Date.now()}`;
    const newSkills = 'Playwright, QA, Automation';

    await page.getByPlaceholder(/Your name/i).fill(newName);
    await page.getByPlaceholder(/Java, Spring, Angular, AWS/i).fill(newSkills);
    await page.getByRole('button', { name: /Save profile/i }).click();

    await expect(page.locator('text=Saving…')).not.toBeVisible({ timeout: 10000 });
    await expect(page.locator('text=Save profile')).toBeVisible();

    await page.reload();
    await expect(page.getByPlaceholder(/Your name/i)).toHaveValue(newName);
    await expect(page.getByPlaceholder(/Java, Spring, Angular, AWS/i)).toHaveValue(newSkills);
  });
});






