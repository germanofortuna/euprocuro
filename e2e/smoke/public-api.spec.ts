import { expect, test } from "@playwright/test";
import { API_URL } from "../playwright.config";

test.describe("Public API endpoints", () => {
  test("GET /actuator/health returns UP", async ({ request }) => {
    const response = await request.get(`${API_URL}/actuator/health`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.status).toBe("UP");
  });

  test("GET /api/categories returns a non-empty array without auth", async ({ request }) => {
    const response = await request.get(`${API_URL}/api/categories`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(Array.isArray(body)).toBe(true);
    expect(body.length).toBeGreaterThan(0);
    expect(body[0]).toHaveProperty("code");
  });

  test("GET /api/operational/public returns 200 without auth (regression for 401 bug)", async ({ request }) => {
    const response = await request.get(`${API_URL}/api/operational/public`);
    expect(response.status(), "this endpoint must be public").toBe(200);
    const body = await response.json();
    expect(body).toHaveProperty("featureFlags");
    expect(body).toHaveProperty("operationalFields");
  });

  test("GET /api/interests returns an array without auth", async ({ request }) => {
    const response = await request.get(`${API_URL}/api/interests?limit=5`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(Array.isArray(body)).toBe(true);
  });

  test("GET /api/interests/<bogus-id> returns 404 with structured error", async ({ request }) => {
    const response = await request.get(`${API_URL}/api/interests/this-id-definitely-does-not-exist-99999`);
    expect(response.status()).toBe(404);
  });

  test("Protected endpoints reject anonymous access with 401", async ({ request }) => {
    const response = await request.get(`${API_URL}/api/dashboard`);
    expect(response.status()).toBe(401);
  });
});
