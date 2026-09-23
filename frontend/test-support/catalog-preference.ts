import { expect, type Page } from "@playwright/test";
export async function verifyCatalogPreferences(page: Page) {
  await expect(page.getByRole("button", { name: "＋ 加入喜好", exact: true })).toHaveCount(3);
  const dialog = page.getByRole("dialog");
  for (const [code, id] of [["0050", "1"], ["0052", "2"], ["GLOBAL_TOP10", "3"]]) {
    const card = page.locator(".product-card").filter({ hasText: code });
    await card.getByRole("button", { name: "＋ 加入喜好", exact: true }).click();
    await expect(dialog.getByRole("combobox", { name: "商品", exact: true })).toHaveValue(id);
    await dialog.getByRole("button", { name: "取消", exact: true }).click();
    await expect(dialog).toHaveCount(0);
    await card.getByRole("button", { name: "＋ 加入喜好", exact: true }).click();
    await dialog.getByLabel("預計數量", { exact: true }).fill("2");
    await dialog.getByRole("button", { name: "確認保存", exact: true }).click();
    await expect(dialog).toHaveCount(0);
    await expect(page.getByRole("status").filter({ hasText: "喜好已保存" })).toBeVisible();
  }
  await page.getByRole("link", { name: "我的喜好", exact: true }).click();
  await expect(page.locator("tbody tr")).toHaveCount(3);
  for (const code of ["0050", "0052", "GLOBAL_TOP10"]) {
    await expect(page.locator("tbody tr").filter({ hasText: code })).toHaveCount(1);
  }
  // The generic entry point resets the last catalog choice.
  await page.getByRole("button", { name: "＋ 保存喜好", exact: true }).click();
  await expect(dialog.getByRole("combobox", { name: "商品", exact: true })).toHaveValue("1");
  await dialog.getByRole("button", { name: "取消", exact: true }).click();
  await expect(page.locator("tbody tr")).toHaveCount(3);
}
