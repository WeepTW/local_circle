import { it, expect } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import LoginView from "./LoginView.vue";
import { productionProvider } from "./productionProvider";
it("production provider has no hints and never falls back to demo login", async () => {
  const w = mount(LoginView, { props: { provider: productionProvider } });
  expect(w.find("select").exists()).toBe(false);
  await w.get('input[name=username]').setValue("user@example.test");
  await w.get('input[name=password]').setValue("Preview-User-1");
  await w.get("form").trigger("submit"); await flushPromises();
  expect(w.emitted("authenticated")).toBeUndefined();
  expect(w.get('[role=alert]').text()).toContain("登入服務尚未設定");
});
