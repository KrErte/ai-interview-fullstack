import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './utils/auth';

const SAMPLE_CV_TEXT = 'This is a long enough CV text to generate AI questions for testing purposes.';

test.describe('Interviewer answer evaluation', () => {
  test('generates questions and evaluates an answer', async ({ page }) => {
    await loginAsAdmin(page);

    await page.goto('/interviewer');
    await page.getByPlaceholder(/CV tekst/i).fill(SAMPLE_CV_TEXT);
    await page.getByRole('button', { name: /Genereeri CV põhised küsimused/i }).click();

    await expect(page.locator('.question-list li').first()).toBeVisible({ timeout: 10000 });
    await page.locator('.question-list li').first().click();

    await page.getByPlaceholder(/Kandidaadi vastus/i).fill('Here is a thoughtful STAR-style answer for testing.');
    await page.getByRole('button', { name: /Saada AI-le hindamiseks/i }).click();

    await expect(page.locator('text=AI hinnang')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('text=Üldskoor')).toBeVisible();
  });
});






