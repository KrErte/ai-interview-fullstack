import { test, expect, Page } from '@playwright/test';
import path from 'path';
import { loginAsAdmin } from './utils/auth';

const cvFixture = path.join(process.cwd(), 'e2e', 'fixtures', 'cv-sample.pdf');

async function ensureCvUploaded(page: Page) {
  await page.goto('/upload-cv');
  await page.setInputFiles('input[type="file"]', cvFixture);
  await page.getByRole('button', { name: /Upload & Parse/i }).click();
  await expect(page.getByPlaceholder(/Parsed CV text will appear here/i)).toContainText(/Sample CV Text/i);
}

test.describe('Upload CV → Job Analysis → Training', () => {
  test('runs analysis and sees training data without errors', async ({ page }) => {
    await loginAsAdmin(page);
    await ensureCvUploaded(page);

    await page.goto('/job-analysis');
    await page.getByPlaceholder(/Parsed CV text/i).fill('Sample CV Text for analysis.');
    await page.getByPlaceholder(/Paste the role you want to analyze/i).fill('Backend engineer role description for analysis.');
    await page.getByRole('button', { name: /Analyze/i }).click();

    await page.waitForResponse(resp => resp.url().includes('/api/job-analysis'));
    await expect(page.locator('text=Match score')).toBeVisible();
    await expect(page.locator('p', { hasText: /error/i })).toHaveCount(0);

    await page.goto('/training');
    await expect(page.locator('text=Training roadmap')).toBeVisible();
    await expect(page.locator('text=Roadmap tasks')).toBeVisible();
    await expect(page.locator('p', { hasText: /error/i })).toHaveCount(0);
  });
});

