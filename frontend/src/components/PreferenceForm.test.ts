import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import PreferenceForm from "./PreferenceForm.vue";
const products = [
  {
    productId: 1,
    productCode: "0050",
    productName: "<script>alert(1)</script>",
    price: 60,
    feeRate: 0.001,
    currency: "TWD",
    ownerLabel: "ETF",
    active: true,
    version: 0,
  },
];
const accounts = [
  { accountId: 10, maskedAccount: "******9666", currency: "TWD", version: 0 },
];
describe("preference form", () => {
  it("escapes XSS, masks accounts and sends only allowed fields", async () => {
    const w = mount(PreferenceForm, {
      props: { products, accounts, busy: false },
    });
    expect(w.find("script").exists()).toBe(false);
    expect(w.text()).toContain("<script>alert(1)</script>");
    expect(w.text()).toContain("******9666");
    await w.find("input").setValue(5);
    await w.find("form").trigger("submit");
    expect(w.emitted("save")?.[0]).toEqual([
      { productId: 1, accountId: 10, plannedQuantity: 5 },
    ]);
  });
  it("blocks invalid quantity, absent account and duplicate busy submit", async () => {
    const w = mount(PreferenceForm, {
      props: { products, accounts, busy: false },
    });
    await w.find("input").setValue(0);
    await w.find("form").trigger("submit");
    expect(w.emitted("save")).toBeUndefined();
    await w.find("input").setValue(5);
    await w.setProps({ busy: true });
    await w.find("form").trigger("submit");
    expect(w.emitted("save")).toBeUndefined();
    await w.setProps({ busy: false, accounts: [] });
    expect(w.text()).toContain("沒有可選的有效帳戶");
  });
  it("cancels without saving", async () => {
    const w = mount(PreferenceForm, {
      props: { products, accounts, busy: false },
    });
    await w.find("button[type=button]").trigger("click");
    expect(w.emitted("cancel")).toHaveLength(1);
    expect(w.emitted("save")).toBeUndefined();
  });
});
