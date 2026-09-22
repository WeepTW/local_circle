import { expect, type Page } from "@playwright/test";
export async function login(page: Page, identity = "1") {
  const logout = page.getByRole("button", { name: "登出", exact: true });
  if (await logout.isVisible()) await logout.click();
  await page.getByLabel("示範帳密選項").selectOption(identity);
  await page.getByRole("button", { name: "登入", exact: true }).click();
  await expect(logout).toBeVisible();
}
