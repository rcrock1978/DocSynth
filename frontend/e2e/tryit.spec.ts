import { test, expect } from '@playwright/test'

/**
 * Playwright E2E: Try It round-trip with a stub API.
 *
 * The stub target API runs on localhost:9090 (configured in the test
 * environment). The test:
 *  1. Adds the host to the allowlist
 *  2. Navigates to the docs page
 *  3. Clicks "Send request" on the Try It console
 *  4. Verifies the stub response is displayed
 */
test('try-it round-trip with a stub API', async ({ page }) => {
  // Add the stub host to the allowlist.
  await page.goto('/tryit')
  await page.getByPlaceholder('api.example.com').fill('localhost:9090')
  await page.getByRole('button', { name: /add host/i }).click()

  // Navigate to a docs page (assumes /v1/ is the current version).
  await page.goto('/v1/')

  // The Try It console fetches a proxy token; this requires a valid OIDC
  // session. The dev Keycloak supplies one for the test user.
  const sendButton = page.getByRole('button', { name: /send request/i }).first()
  await sendButton.click()

  // Verify the response is shown.
  await expect(page.getByText(/Status:/)).toBeVisible({ timeout: 10_000 })
  await expect(page.getByText(/Request ID:/)).toBeVisible()
})
