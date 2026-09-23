import { expect, test, type Page } from "@playwright/test";

export async function verifyPersonalPreference(page: Page) {
  await page.getByRole("link", { name: "我的喜好", exact: true }).click();
  await page.getByRole("button", { name: "＋ 保存喜好" }).click();
  await page
    .getByRole("combobox", { name: "商品", exact: true })
    .selectOption("0");
  const name = '<img src=x onerror="window.__personalXss=1">';
  await page.getByLabel("商品名稱", { exact: true }).fill(name);
  await page.getByLabel("參考單價", { exact: true }).fill("12.5");
  await page.getByRole("spinbutton", { name: "手續費率" }).fill("0.02");
  await page
    .getByRole("combobox", { name: "帳戶來源", exact: true })
    .selectOption({ label: "輸入完整帳號" });
  await page
    .getByRole("textbox", { name: "完整扣款帳號" })
    .fill("001234567890");
  await page.getByLabel("預計數量", { exact: true }).fill("2");
  await page.screenshot({path: test.info().outputPath("personal-form.png"), fullPage: true});
  await page.getByRole("button", { name: "確認保存", exact: true }).click();
  const row = page.locator("tbody tr").filter({ hasText: name });
  await expect(row).toContainText("25.5");
  await expect(row).toContainText("******7890");
  await expect(row.locator("img")).toHaveCount(0);
  expect(
    await page.evaluate(() => Object.hasOwn(window, "__personalXss")),
  ).toBe(false);
  await row.getByRole("button", { name: "查看完整帳號" }).click();
  await expect(row).toContainText("001234567890");
  await row.getByRole("button", { name: "隱藏帳號" }).click();
  await expect(row).not.toContainText("001234567890");
  await row.getByRole("button", { name: "編輯", exact: true }).click();
  await expect(page.getByLabel("商品名稱", { exact: true })).toHaveValue(name);
  await page.getByLabel("商品名稱", { exact: true }).fill("個人修改後商品");
  await page.getByLabel("參考單價", { exact: true }).fill("20");
  await page.getByRole("spinbutton", { name: "手續費率" }).fill("0.01");
  await page.getByLabel("預計數量", { exact: true }).fill("3");
  await page.getByRole("button", { name: "確認保存", exact: true }).click();
  const updated = page
    .locator("tbody tr")
    .filter({ hasText: "個人修改後商品" });
  await expect(updated).toContainText("60.6");
  await updated.getByRole("button", { name: "刪除", exact: true }).click();
  await page.getByRole("button", { name: "確認刪除", exact: true }).click();
  await expect(updated).toHaveCount(0);
  expect(
    await page.evaluate(() => JSON.stringify([localStorage, sessionStorage])),
  ).not.toContain("001234567890");
}
