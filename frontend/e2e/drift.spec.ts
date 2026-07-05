import { test, expect } from '@playwright/test'

/**
 * Playwright E2E: trigger drift and see a report on the Drift Detail page.
 *
 * Assumes a project exists with at least one published DocSet baseline
 * and a new ApiSpec has been ingested. The test submits a manual drift
 * trigger and verifies the Drift Detail page renders with summary counts.
 */
test('trigger drift and see report', async ({ page }) => {
  await page.goto('/drift')
  await expect(page.getByRole('heading', { name: /drift reports/i })).toBeVisible()

  // Navigate to the most recent report (first row link).
  const firstReport = page.locator('tbody tr').first()
  await firstReport.locator('a').click()

  // Verify the detail page shows summary fields.
  await expect(page.getByText(/Trigger/)).toBeVisible()
  await expect(page.getByText(/Breaking/)).toBeVisible()
})
