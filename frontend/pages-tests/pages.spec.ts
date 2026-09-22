import { test, expect } from "@playwright/test";
test("sample quotes update the catalog while saved amounts remain unchanged", async ({ page }) => {
  await page.goto("./#/preferences");
  await page.getByRole("button", { name: "＋ 保存喜好" }).click();
  await page.getByLabel("預計數量", { exact: true }).fill("5");
  await page.getByRole("button", { name: "確認保存", exact: true }).click();
  await page.getByRole("link", { name: "商品目錄", exact: true }).click();
  const panel = page.getByRole("region", { name: "行情來源" });
  await expect(panel).toContainText("非即時報價");
  await panel.getByRole("button", { name: "套用範例報價" }).click();
  await expect(page.locator(".product-card").filter({hasText: "0050"})).toContainText("60.25");
  await page.getByRole("link", { name: "我的喜好", exact: true }).click();
  await expect(page.locator("tbody")).toContainText("300.3");
  await page.getByRole("link", { name: "商品目錄", exact: true }).click();
  await page.route("**/data/quotes.sample.json", route => route.fulfill({status: 503, body: "unavailable"}));
  await panel.getByRole("button", { name: "更新行情", exact: true }).click();
  await expect(panel.getByRole("alert")).toBeVisible();
  await expect(panel.getByRole("button", { name: "套用範例報價" })).toBeDisabled();
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
  await page.getByLabel("檢視角色").selectOption("2");
  await expect(page.getByText("從第一個喜好開始")).toBeVisible();
  await page.getByLabel("檢視角色").selectOption("1");
  await expect(page.locator("tbody tr")).toHaveCount(1);
  await page.getByRole("button", { name: "刪除", exact: true }).click();
  await page.getByRole("button", { name: "確認刪除", exact: true }).click();
  await expect(page.locator("tbody tr")).toHaveCount(0);
  await page.getByRole("link", { name: "商品目錄", exact: true }).click();
  await page.reload();
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
