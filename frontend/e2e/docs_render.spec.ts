import { test, expect } from '@playwright/test'

/**
 * Playwright E2E: a generated DocSet renders an endpoint reference page
 * with at least one working code example.
 *
 * Assumes a published DocSet exists in staging at /v1/ with the
 * endpoint GET /users. The test verifies rendering, not the
 * generation pipeline (covered by backend tests).
 */
test('generated doc page renders with code example', async ({ page }) => {
  await page.goto('/v1/')

  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()

  // The version selector should be populated.
  await expect(page.getByLabel(/version/i)).toBeVisible()

  // At least one code example block should be present.
  await expect(page.locator('.code-example-block').first()).toBeVisible()

  // The example should have a "Copy" button.
  await expect(page.getByRole('button', { name: /copy/i }).first()).toBeVisible()
})
