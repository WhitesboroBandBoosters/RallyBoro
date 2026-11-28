import { defineConfig } from "@playwright/test";
const baseURL = process.env.RALLYBORO_TEST_BASE_URL ?? "http://127.0.0.1:18080";
if (baseURL !== "http://127.0.0.1:18080") {
  throw new Error(
    "Browser tests require the disposable local host. Run scripts/test-ui.sh.",
  );
}
export default defineConfig({
  testDir: "./e2e",
  timeout: 90000,
  workers: 1,
  retries: 0,
  reporter: "list",
  use: {
    baseURL,
    trace: "off",
    video: "off",
    screenshot: "off",
  },
  projects: [
    { name: "desktop", use: { viewport: { width: 1440, height: 1000 } } },
    {
      name: "mobile",
      use: {
        viewport: { width: 375, height: 800 },
        isMobile: true,
        hasTouch: true,
      },
    },
  ],
});
