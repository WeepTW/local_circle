import { login } from "../test-support/login";
import { test, expect } from "@playwright/test";
test("catalog uses reference prices without market-data requests", async ({ page }) => {
  const dataRequests: string[] = [];
  page.on("request", request => {
    if (["fetch", "xhr", "websocket"].includes(request.resourceType())) dataRequests.push(request.url());
  });
  await page.goto("./#/products");
  await login(page);
  await expect(page.locator(".product-card")).toHaveCount(3);
  await expect(page.locator(".product-card").filter({ hasText: "0050" })).toContainText("NT$ 60");
  await expect(page.getByRole("region", { name: "行情來源" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "更新行情" })).toHaveCount(0);
  await page.clock.install();
  await page.clock.fastForward(61000);
  expect(dataRequests).toEqual([]);
});
test("Pages survives subpath routing and supports isolated CRUD without backend requests", async ({
  page,
}) => {
  const errors: string[] = [],
    apiCalls: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  page.on("request", (r) => {
    if (r.url().includes("/api/")) apiCalls.push(r.url());
  });
  await page.goto("./#/preferences");
  await login(page);
  await expect(page.getByRole("note")).toContainText("互動展示");
  await page.getByRole("button", { name: "＋ 保存喜好" }).click();
  await page.getByLabel("預計數量", { exact: true }).fill("5");
  await page.getByRole("button", { name: "確認保存", exact: true }).click();
  await expect(page.locator("tbody")).toContainText("300.3");
  await expect(page.locator("tbody")).not.toContainText("DEMO");
  await page.getByRole("button", { name: "編輯", exact: true }).click();
  await page
    .getByRole("combobox", { name: "商品", exact: true })
    .selectOption("2");
  await page.getByRole("button", { name: "確認保存", exact: true }).click();
  await expect(page.locator("tbody")).toContainText("0052");
  await login(page, "2");
  await expect(page.getByText("從第一個喜好開始")).toBeVisible();
  await login(page, "1");
  await expect(page.locator("tbody tr")).toHaveCount(1);
  await page.getByRole("button", { name: "刪除", exact: true }).click();
  await page.getByRole("button", { name: "確認刪除", exact: true }).click();
  await expect(page.locator("tbody tr")).toHaveCount(0);
  await page.getByRole("link", { name: "商品目錄", exact: true }).click();
  await page.reload();
  await login(page);
  await expect(
    page.getByRole("heading", { name: "商品目錄", exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: "◌ local_circle", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "我的喜好清單", exact: true }),
  ).toBeVisible();
  expect(apiCalls).toEqual([]);
  expect(errors).toEqual([]);
  await page.screenshot({
    path: "../tmp/refactor/pages-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "../tmp/refactor/pages-mobile.png",
    fullPage: true,
  });
});

test("login requires valid credentials and logout hides the registry", async ({ page }) => {
  await page.goto("./#/admin");
  await expect(page.getByRole("heading", { name: "登入", exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "商品管理", exact: true })).toHaveCount(0);
  await page.getByLabel("示範帳密選項").selectOption("3");
  await expect(page.getByLabel("帳號", { exact: true })).toHaveValue("etf-admin@example.test");
  await page.getByLabel("密碼", { exact: true }).fill("wrong");
  await page.getByRole("button", { name: "登入", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("帳號或密碼不正確");
  await login(page, "1");
  await expect(page.getByRole("button", { name: "編輯商品", exact: true })).toHaveCount(0);
  await login(page, "3");
  await expect(page.getByRole("button", { name: "編輯商品", exact: true })).toHaveCount(2);
  await page.getByRole("button", { name: "登出", exact: true }).click();
  await expect(page.getByRole("heading", { name: "登入", exact: true })).toBeVisible();
  await expect(page.getByLabel("密碼", { exact: true })).toHaveValue("");
  await expect(page.locator(".product-card")).toHaveCount(0);
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: "../tmp/test-results/login-mobile.png", fullPage: true });
});
