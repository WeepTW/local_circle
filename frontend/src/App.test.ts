import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { createRouter, createMemoryHistory } from "vue-router";
import App from "./App.vue";
import { api } from "./api";
vi.mock("./api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./api")>();
  return {
    ...original,
    api: {
      me: vi.fn(),
      products: vi.fn(),
      adminProducts: vi.fn(),
      accounts: vi.fn(),
      preferences: vi.fn(),
      save: vi.fn(),
      remove: vi.fn(),
      update: vi.fn(),
      product: vi.fn(),
    },
  };
});
async function app(path = "/preferences") {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/:pathMatch(.*)*", component: { render: () => null } }],
  });
  await router.push(path);
  await router.isReady();
  const w = mount(App, { global: { plugins: [router] } });
  await flushPromises();
  return w;
}
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(api.me).mockResolvedValue({
    userId: 1,
    userName: "Demo",
    email: "user@example.test",
    role: "USER",
    labels: [],
  });
  vi.mocked(api.products).mockResolvedValue([]);
  vi.mocked(api.accounts).mockResolvedValue([]);
  vi.mocked(api.preferences).mockResolvedValue([]);
});
describe("application states", () => {
  it("shows empty state and hides admin for a regular user", async () => {
    const w = await app();
    expect(w.text()).toContain("從第一個喜好開始");
    expect(w.find('a[href="/admin"]').exists()).toBe(false);
  });
  it("offers retry after API failure", async () => {
    vi.mocked(api.preferences).mockRejectedValue(
      new Error("network unavailable"),
    );
    const w = await app();
    expect(w.find("[role=alert]").text()).toContain("network unavailable");
    expect(w.text()).toContain("重新整理");
  });
  it("delete dialog supports cancel and confirm", async () => {
    vi.mocked(api.preferences).mockResolvedValue([
      {
        preferenceId: 9,
        productId: 1,
        productCode: "0050",
        productName: "DEMO",
        accountId: 10,
        maskedAccount: "******9666",
        userEmail: "user@example.test",
        plannedQuantity: 5,
        priceSnapshot: 60,
        feeRateSnapshot: 0.001,
        baseAmount: 300,
        totalFee: 0.3,
        totalAmount: 300.3,
        version: 2,
        savedAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
    ]);
    const w = await app();
    const click = async (text: string) => {
      await w
        .findAll("button")
        .find((b) => b.text() === text)!
        .trigger("click");
      await flushPromises();
    };
    await click("刪除");
    await click("取消");
    expect(api.remove).not.toHaveBeenCalled();
    await click("刪除");
    await click("確認刪除");
    expect(api.remove).toHaveBeenCalledWith(9, 2);
  });
});

it("opens the catalog item's preference form and cancels without saving", async () => {
  vi.mocked(api.products).mockResolvedValue([
    {
      productId: 1,
      productCode: "0050",
      productName: "台灣50",
      price: 60,
      feeRate: 0.001,
      currency: "TWD",
      ownerLabel: "TW_ETF_OWNER",
      active: true,
      version: 0,
    },
    {
      productId: 2,
      productCode: "0052",
      productName: "科技",
      price: 180,
      feeRate: 0.001,
      currency: "TWD",
      ownerLabel: "TW_ETF_OWNER",
      active: true,
      version: 0,
    },
  ]);
  vi.mocked(api.accounts).mockResolvedValue([
    { accountId: 10, maskedAccount: "******9666", currency: "TWD", version: 0 },
  ]);
  const w = await app("/products");
  const buttons = w.findAll(".product-card button");
  expect(buttons).toHaveLength(2);
  await buttons[1].trigger("click");
  const dialog = w.get("[role=dialog]");
  expect((dialog.get("select").element as HTMLSelectElement).value).toBe("2");
  expect(dialog.text()).toContain("180.18");
  await dialog
    .findAll("button")
    .find((b) => b.text() === "取消")!
    .trigger("click");
  expect(w.find("[role=dialog]").exists()).toBe(false);
  expect(api.save).not.toHaveBeenCalled();
  await buttons[0].trigger("click");
  expect(
    (w.get("[role=dialog] select").element as HTMLSelectElement).value,
  ).toBe("1");
  await w.get("[role=dialog] form").trigger("submit");
  await flushPromises();
  expect(api.save).toHaveBeenCalledWith({
    productId: 1,
    productName: "台灣50",
    price: 60,
    feeRate: 0.001,
    accountId: 10,
    plannedQuantity: 1,
  });
});
