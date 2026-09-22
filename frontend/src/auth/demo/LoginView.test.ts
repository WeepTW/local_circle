import { it, expect } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import LoginView from "../LoginView.vue";
import { demoProvider } from "./provider";

it("fills suggested credentials, rejects edits and accepts an explicit login", async () => {
  const w = mount(LoginView, { props: { provider: demoProvider } });
  await w.get("select").setValue("3");
  expect((w.get('input[name=username]').element as HTMLInputElement).value).toBe("etf-admin@example.test");
  expect(w.emitted("authenticated")).toBeUndefined();
  await w.get('input[name=password]').setValue("incorrect");
  await w.get("form").trigger("submit"); await flushPromises();
  expect(w.get('[role=alert]').text()).toContain("帳號或密碼不正確");
  expect(w.emitted("authenticated")).toBeUndefined();
  await w.get("select").setValue("1");
  await w.get("form").trigger("submit"); await flushPromises();
  expect(w.emitted("authenticated")?.[0]).toEqual([{ userId: "1" }]);
});
