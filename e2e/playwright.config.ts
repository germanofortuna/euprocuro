import { defineConfig, devices } from "@playwright/test";

const FRONTEND_URL = process.env.E2E_FRONTEND_URL ?? "http://localhost:5173";
const API_URL = process.env.E2E_API_URL ?? "http://localhost:8080";

export default defineConfig({
  testDir: "./",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [
    ["list"],
    ["html", { open: "never", outputFolder: "playwright-report" }],
    ["json", { outputFile: "playwright-report/results.json" }]
  ],
  timeout: 30_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL: FRONTEND_URL,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
    extraHTTPHeaders: {
      "x-e2e-source": "playwright"
    }
  },
  projects: [
    {
      name: "smoke",
      testMatch: /smoke\/.*\.spec\.ts/,
      use: { ...devices["Desktop Chrome"] }
    }
  ],
  metadata: {
    frontendUrl: FRONTEND_URL,
    apiUrl: API_URL,
    env: process.env.E2E_ENV ?? "local"
  }
});

export { FRONTEND_URL, API_URL };
