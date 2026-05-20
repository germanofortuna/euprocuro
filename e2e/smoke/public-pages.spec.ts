import { expect, test } from "@playwright/test";

test.describe("Public pages render", () => {
  test("home page loads with brand and main heading", async ({ page }) => {
    const response = await page.goto("/");
    expect(response?.status(), "home should respond 200").toBeLessThan(400);
    await expect(page).toHaveTitle(/Eu Procuro|EU PROCURO/i);
    await expect(page.locator("body")).toContainText(/procuro|procura/i);
  });

  test("categorias index page loads", async ({ page }) => {
    const response = await page.goto("/categorias");
    expect(response?.status()).toBeLessThan(400);
    await expect(page.locator("h1, h2")).not.toHaveCount(0);
  });

  test("como-funciona page loads", async ({ page }) => {
    const response = await page.goto("/como-funciona");
    expect(response?.status()).toBeLessThan(400);
  });

  test("ouvidoria page loads", async ({ page }) => {
    const response = await page.goto("/ouvidoria");
    expect(response?.status()).toBeLessThan(400);
  });

  test("legal page termos-de-uso loads", async ({ page }) => {
    const response = await page.goto("/legal/termos-de-uso");
    expect(response?.status()).toBeLessThan(400);
    await expect(page.locator("body")).toContainText(/termos|uso/i);
  });

  test("robots.txt is served", async ({ request }) => {
    const response = await request.get("/robots.txt");
    expect(response.status()).toBe(200);
    expect(await response.text()).toMatch(/User-agent/i);
  });

  test("sitemap.xml is served", async ({ request }) => {
    const response = await request.get("/sitemap.xml");
    expect(response.status()).toBe(200);
    expect(await response.text()).toContain("<urlset");
  });

  test("non-existent interest detail shows 'not found' message, not crash", async ({ page }) => {
    await page.goto("/interesses/this-id-definitely-does-not-exist-99999");
    await expect(page.locator("body")).toContainText(/n[aã]o encontrad/i);
  });

  test("listing does not link to mock sample-N ids (regression for fallback bug)", async ({ page }) => {
    await page.goto("/");
    const sampleLinks = await page.locator('a[href*="/interesses/sample-"]').count();
    expect(sampleLinks, "no sample-N placeholder links should leak to UI").toBe(0);
  });
});
