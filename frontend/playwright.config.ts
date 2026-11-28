import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  timeout: 90000,
  workers: 1,
  retries: 0,
  reporter: "list",
  use: {
    baseURL: process.env.RALLYBORO_TEST_BASE_URL ?? "http://127.0.0.1:18080",
    trace: "off",
    video: "off",
    screenshot: "off",
  },
  projects: [
    { name: "desktop", use: { viewport: { width: 1440, height: 1000 } } },
    {
      name: "mobile",
      use: {
        viewport: { width: 390, height: 844 },
        isMobile: true,
        hasTouch: true,
      },
    },
  ],
});
