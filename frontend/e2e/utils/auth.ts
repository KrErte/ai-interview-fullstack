import { Page, expect } from '@playwright/test';

const ADMIN_EMAIL = 'admin@local.test';
// Assumption: same password as used in backend integration tests.
const ADMIN_PASSWORD = 'Password1!';

export async function loginAsAdmin(page: Page, opts?: { email?: string; password?: string }) {
  const email = opts?.email ?? ADMIN_EMAIL;
  const password = opts?.password ?? ADMIN_PASSWORD;

  await page.goto('/login');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: /log in/i }).click();

  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByText('Welcome back')).toBeVisible();
}






