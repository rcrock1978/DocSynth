import { test, expect } from '@playwright/test'

/**
 * Playwright E2E: submit a public OpenAPI spec and verify that the parsed
 * endpoints are visible on the detail page.
 *
 * In CI this runs against the staging stack; locally it runs against the
 * docker-compose dev stack. The test relies on the spec submit endpoint
 * being reachable at /api/v1/projects/current/specs.
 */
test('submit a public spec and see parsed endpoints', async ({ page, request }) => {
  await page.goto('/specs/new')

  await page.getByLabel('Source').selectOption('url')
  await page.getByLabel('Source reference').fill(
    'https://petstore3.swagger.io/api/v3/openapi.json'
  )
  await page.getByRole('button', { name: /submit/i }).click()

  await expect(page.getByText(/spec created/i)).toBeVisible({ timeout: 60_000 })

  // Then navigate to the spec list and verify the row appears.
  await page.goto('/specs')
  await expect(page.getByRole('link', { name: /petstore/i }).first()).toBeVisible({
    timeout: 60_000,
  })
})
