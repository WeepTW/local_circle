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
async function app() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/:pathMatch(.*)*", component: { render: () => null } }],
  });
  await router.push("/preferences");
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
