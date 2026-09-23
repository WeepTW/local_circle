import { afterEach, expect, it, vi } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";
import AccountNumber from "./AccountNumber.vue";
import { api } from "../api";
vi.mock("../api", async () => ({
  ...(await vi.importActual("../api")),
  api: { accountNumber: vi.fn() },
}));
afterEach(() => { vi.useRealTimers(); vi.clearAllMocks(); });

it("reveals only on demand and clears the number after 30 seconds", async () => {
  vi.useFakeTimers();
  vi.mocked(api.accountNumber).mockResolvedValue({ accountNumber: "001234567890" });
  const wrapper = mount(AccountNumber, { props: { accountId: 10, masked: "******7890" } });
  expect(api.accountNumber).not.toHaveBeenCalled();
  expect(wrapper.text()).not.toContain("001234567890");
  await wrapper.get("button").trigger("click"); await flushPromises();
  expect(wrapper.text()).toContain("001234567890");
  await vi.advanceTimersByTimeAsync(30000);
  expect(wrapper.text()).not.toContain("001234567890");
  wrapper.unmount();
});

it("does not render a late response after the identity view is unmounted", async () => {
  let resolve!: (value: { accountNumber: string }) => void;
  vi.mocked(api.accountNumber).mockReturnValue(new Promise(r => { resolve = r; }));
  const host = document.createElement("div"); document.body.appendChild(host);
  const wrapper = mount(AccountNumber, { attachTo: host, props: { accountId: 10, masked: "******7890" } });
  await wrapper.get("button").trigger("click"); wrapper.unmount();
  resolve({ accountNumber: "001234567890" }); await flushPromises();
  expect(host.textContent).not.toContain("001234567890"); host.remove();
});
