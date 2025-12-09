import { test, expect, Page } from '@playwright/test';
import path from 'path';
import { loginAsAdmin } from './utils/auth';

const cvFixture = path.join(process.cwd(), 'e2e', 'fixtures', 'cv-sample.pdf');

async function uploadCv(page: Page) {
  await page.goto('/upload-cv');
  await page.setInputFiles('input[type="file"]', cvFixture);
  await page.getByRole('button', { name: /Upload & Parse/i }).click();
  await expect(page.getByPlaceholder(/Parsed CV text will appear here/i)).toBeVisible();
  await expect(page.getByPlaceholder(/Parsed CV text will appear here/i)).toContainText(/Sample CV Text/i);
}

test.describe('Upload CV → Job Match', () => {
  test('uploads CV then runs job match without errors', async ({ page }) => {
    await loginAsAdmin(page);

    await uploadCv(page);

    await page.goto('/job-match');
    await page.getByPlaceholder(/Senior Full-Stack Engineer/i).fill('Backend Engineer');
    await page.getByPlaceholder(/Remote \/ Hybrid/i).fill('Remote');
    await page.getByPlaceholder(/Paste or upload your CV text/i).fill('Sample CV Text for matching purposes.');
    await page.getByPlaceholder(/Paste the job description you want to match/i).fill('Looking for backend engineer with Java and Spring.');

    await page.getByRole('button', { name: /Run Match/i }).click();

    await page.waitForResponse(resp => resp.url().includes('/api/job-match'));
    await expect(page.locator('text=Matches')).toBeVisible();
    await expect(page.locator('p', { hasText: /error/i })).toHaveCount(0);

    const cardsCount = await page.locator('.rounded-xl').count();
    const emptyState = await page.locator('text=Run a match to see results.').count();
    expect(cardsCount > 0 || emptyState > 0).toBeTruthy();
  });
});

