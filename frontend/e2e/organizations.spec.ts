import { test, expect, type Page } from "@playwright/test";
import { randomUUID } from "node:crypto";
const password = "synthetic-browser-password";
async function register(page: Page, username: string, name: string) {
  await page
    .getByRole("button", { name: "Create account", exact: true })
    .click();
  await page.getByLabel("Display name").fill(name);
  await page.getByLabel("Username", { exact: true }).fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page
    .getByRole("button", { name: "Create account", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Save your recovery key" }),
  ).toBeVisible();
  const key = await page
    .getByLabel("Recovery key", { exact: true })
    .inputValue();
  await page.getByLabel("I saved my recovery key").check();
  await page.getByRole("button", { name: "Continue to sign in" }).click();
  await page.getByLabel("Username", { exact: true }).fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  return key;
}
async function accountLink(page: Page, name: string) {
  await page.locator(".account-menu summary").click();
  await page.getByRole("link", { name, exact: true }).click();
}
test("local accounts create organizations, invite with scoped permissions, revoke links, and recover access", async ({
  browser,
}, info) => {
  const options = {
    viewport: info.project.use.viewport,
    isMobile: !!info.project.use.isMobile,
    hasTouch: !!info.project.use.hasTouch,
  };
  const ownerContext = await browser.newContext(options),
    memberContext = await browser.newContext(options);
  const page = await ownerContext.newPage(),
    member = await memberContext.newPage();
  const suffix = randomUUID().replaceAll("-", "").slice(0, 16);
  const ownerUsername = "owner_" + suffix,
    memberUsername = "member_" + suffix;
  const organization = "Neighborhood volunteers (" + info.project.name + ")";
  await page.goto("/login");
  const recoveryKey = await register(
    page,
    ownerUsername,
    "Organization creator",
  );
  await expect(
    page.getByRole("heading", { name: "Create organization", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("link", { name: "My registrations", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "My registrations", exact: true }),
  ).toBeVisible();
  await page.goto("/");
  await page.getByLabel("Organization name").fill(organization);
  await page
    .getByRole("button", { name: "Create organization", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Your events", exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Organization", exact: true }).click();
  await page
    .getByRole("button", { name: "Create invitation", exact: true })
    .click();
  const invitation = await page.getByLabel("Invitation link").inputValue();
  await member.goto(invitation);
  await expect(
    member.getByRole("heading", { name: "Sign in", exact: true }),
  ).toBeVisible();
  await expect(member.getByText(organization, { exact: true })).toHaveCount(0);
  await register(member, memberUsername, "Invited volunteer");
  await expect(
    member.getByRole("heading", { name: "Organization invitation" }),
  ).toBeVisible();
  await expect(
    member.getByRole("heading", { name: organization, exact: true }),
  ).toBeVisible();
  await member
    .getByRole("button", { name: "Accept invitation", exact: true })
    .click();
  await expect(
    member.getByRole("heading", { name: "Find your next shift", exact: true }),
  ).toBeVisible();
  await expect(member.getByRole("link", { name: "Create event" })).toHaveCount(
    0,
  );
  await expect(
    member.getByRole("link", { name: "Organization", exact: true }),
  ).toHaveCount(0);
  // An ordinary member may create their own organization without gaining control of this one.
  await accountLink(member, "Create organization");
  await member
    .getByLabel("Organization name")
    .fill("My separate organization (" + info.project.name + ")");
  await member
    .getByRole("button", { name: "Create organization", exact: true })
    .click();
  await expect(
    member.getByRole("heading", { name: "Your events", exact: true }),
  ).toBeVisible();
  await member
    .getByLabel("Organization", { exact: true })
    .selectOption({ label: organization });
  await expect(
    member.getByRole("heading", { name: "Find your next shift", exact: true }),
  ).toBeVisible();
  await page.reload();
  const memberForm = page.locator("form.member-row").filter({
    has: page.getByRole("heading", {
      name: "Invited volunteer",
      exact: true,
    }),
  });
  await memberForm.getByLabel("Create and manage all events").check();
  await memberForm
    .getByRole("button", { name: "Save permissions", exact: true })
    .click();
  await expect(
    page.getByRole("status").filter({ hasText: "Permissions saved." }),
  ).toBeVisible();
  await member.reload();
  await member
    .getByLabel("Organization", { exact: true })
    .selectOption({ label: organization });
  await expect(
    member.getByRole("heading", { name: "Your events", exact: true }),
  ).toBeVisible();
  await expect(
    member.getByRole("link", { name: "Create event" }),
  ).toBeVisible();
  await expect(
    member.getByRole("link", { name: "Organization", exact: true }),
  ).toHaveCount(0);
  // The last organization manager cannot accidentally remove their own management grant.
  const ownerForm = page
    .locator("form.member-row")
    .filter({ hasText: "Organization creator (you)" });
  await ownerForm.getByLabel("Manage organization and permissions").uncheck();
  await ownerForm
    .getByRole("button", { name: "Save permissions", exact: true })
    .click();
  await expect(page.getByRole("alert")).toContainText(
    "Keep at least one organization manager",
  );
  await page.reload();
  await page
    .getByRole("button", { name: "Create invitation", exact: true })
    .click();
  const revokedLink = await page.getByLabel("Invitation link").inputValue();
  const available = page.locator(".shift-row").filter({ hasText: "AVAILABLE" });
  await available
    .getByRole("button", { name: "Revoke invitation", exact: true })
    .click();
  await expect(
    page.getByRole("status").filter({ hasText: "Invitation revoked." }),
  ).toBeVisible();
  await member.goto(revokedLink);
  await expect(member.getByRole("alert")).toContainText(
    "This invitation is unavailable",
  );
  await accountLink(page, "Account settings");
  await page.getByLabel("Current password", { exact: true }).fill(password);
  await page
    .getByLabel("New password", { exact: true })
    .fill("changed-synthetic-browser-password");
  await page
    .getByRole("button", { name: "Change password and sign out" })
    .click();
  await expect(
    page.getByRole("heading", { name: "Sign in", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Recover account", exact: true })
    .click();
  await page.getByLabel("Username", { exact: true }).fill(ownerUsername);
  await page.getByLabel("Recovery key", { exact: true }).fill(recoveryKey);
  await page
    .getByLabel("New password", { exact: true })
    .fill("recovered-synthetic-browser-password");
  await page
    .getByRole("button", { name: "Reset password", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Save your recovery key" }),
  ).toBeVisible();
  expect(
    await page.getByLabel("Recovery key", { exact: true }).inputValue(),
  ).not.toBe(recoveryKey);
  await page.getByLabel("I saved my recovery key").check();
  await page.getByRole("button", { name: "Continue to sign in" }).click();
  await page
    .getByLabel("Password", { exact: true })
    .fill("recovered-synthetic-browser-password");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Account", exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Events", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Your events", exact: true }),
  ).toBeVisible();
  await expect(page.locator(".organization-name")).toHaveText(organization);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await ownerContext.close();
  await memberContext.close();
});
