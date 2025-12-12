import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './utils/auth';

test.describe('Auth flow', () => {
  test('Login → Dashboard → Logout', async ({ page }) => {
    await page.goto('/login');

    await loginAsAdmin(page);

    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.getByText(/Welcome back/i)).toBeVisible();
    await expect(page.getByRole('link', { name: /Upload CV/i })).toBeVisible();

    await page.getByRole('button', { name: /Logout/i }).click();

    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByRole('button', { name: /Log in/i })).toBeVisible();
    await expect(page.getByText(/Welcome back/i)).not.toBeVisible({ timeout: 2000 });
  });
});





























