import { test, expect, type Page } from "@playwright/test";
import fs from "node:fs";
const file = process.env.RALLYBORO_TEST_CREDENTIALS_FILE;
if (!file)
  throw new Error(
    "Set RALLYBORO_TEST_CREDENTIALS_FILE to a protected synthetic-test credential file.",
  );
const credentials = Object.fromEntries(
  fs
    .readFileSync(file, "utf8")
    .trim()
    .split("\n")
    .map((line) => {
      const i = line.indexOf("=");
      return [line.slice(0, i), line.slice(i + 1)];
    }),
);
async function login(page: Page, user: string) {
  await page.goto("/login");
  await page.getByLabel("Pilot username").fill(user);
  await page.getByLabel("Password", { exact: true }).fill(credentials[user]);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(
    page.getByRole("heading", {
      name: user === "organizer" ? "Your events" : "Find your next shift",
      exact: true,
    }),
  ).toBeVisible();
}
test("organizer creates staffing; volunteer signs up and cancels; coverage persists", async ({
  browser,
}, info) => {
  const organizer = await browser.newContext({
    viewport: info.project.use.viewport,
  });
  const volunteer = await browser.newContext({
    viewport: info.project.use.viewport,
  });
  const page = await organizer.newPage();
  const second = await volunteer.newPage();
  const date = new Date(Date.now()+14*86400000).toISOString().slice(0,10);
  const title = `Community supper ${info.project.name} ${Date.now()}`;
  await login(page, "organizer");
  await page.getByRole("link", { name: "Create event" }).click();
  await page.getByLabel("Event title").fill(title);
  await page
    .getByLabel("Description", { exact: true })
    .fill("Help our community share a welcoming evening.");
  await page.getByLabel("Date", { exact: true }).fill(date);
  await page.getByLabel("Timezone", { exact: true }).fill("UTC");
  await page.getByLabel("Location", { exact: true }).fill("Community hall");
  await page.getByLabel("Visibility").selectOption("PRIVATE");
  await page.getByRole("button", { name: "Save event", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Edit event", exact: true }),
  ).toBeVisible();
  for (const [name, start, end] of [
    ["Welcome team", "09:00", "10:00"],
    ["Food service", "10:00", "11:00"],
  ]) {
    const add = page
      .locator("form")
      .filter({
        has: page.getByRole("button", { name: "Add job", exact: true }),
      });
    await add.getByLabel("Job name").fill(name);
    await add.getByRole("button", { name: "Add job", exact: true }).click();
    const panel = page
      .locator("section.panel")
      .filter({ has: page.locator(`input[value="${name}"]`) });
    const form = panel
      .locator("form")
      .filter({
        has: page.getByRole("button", { name: "Add shift", exact: true }),
      });
    await form.getByLabel("Start (UTC)").fill(date+"T" + start);
    await form.getByLabel("End (UTC)").fill(date+"T" + end);
    await form.getByLabel("Capacity").fill("2");
    await form.getByRole("button", { name: "Add shift", exact: true }).click();
    await expect(panel.getByRole("button", {name:"Save shift",exact:true})).toBeVisible();
  }
  await page.reload();
  await expect(page.locator('input[value="Welcome team"]')).toBeVisible();
  await expect(page.locator('input[value="Food service"]')).toBeVisible();
  await page.getByLabel("Open volunteer signup").check();
  await page.getByRole("button", { name: "Save event", exact: true }).click();
  await expect(page.getByRole("status").filter({hasText:"Event saved."})).toBeVisible();
  await page.getByRole("link", { name: /View event/ }).click();
  await expect(
    page.getByRole("heading", { name: title, exact: true }),
  ).toBeVisible();
  const eventUrl = page.url();
  await login(second, "volunteer");
  await second.goto(eventUrl);
  await second
    .getByRole("button", { name: "Sign up", exact: true })
    .first()
    .click();
  await expect(
    second.getByRole("status").filter({ hasText: "You’re confirmed" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Refresh availability" }).click();
  await expect(page.locator(".coverage .shift-row")).toHaveCount(1);
  await second
    .getByRole("link", { name: "My registrations", exact: true })
    .click();
  const card = second.locator(".registration-card").filter({ hasText: title });
  await expect(card).toContainText("Confirmed");
  second.once("dialog", (dialog) => dialog.accept());
  await card.getByRole("button", { name: "Cancel registration" }).click();
  await expect(
    second.locator(".registration-card").filter({ hasText: title }),
  ).toContainText("Cancelled");
  await page.getByRole("button", { name: "Refresh availability" }).click();
  await expect(page.locator(".coverage")).toContainText(
    "No confirmed assignments yet.",
  );
  await expect(page.locator(".stats")).toContainText("0/4");
  await expect(
    page.getByRole("heading", { name: title, exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  if (process.env.RALLYBORO_QA_SCREENSHOTS) {
    await page.screenshot({
      path: `../tmp/pilot-${info.project.name}.png`,
      fullPage: true,
    });
  }
  await organizer.close();
  await volunteer.close();
});
