import { it, expect, vi } from "vitest";
it("isolates ownership, preserves snapshots and enforces product ACL/version", async () => {
  vi.resetModules();
  const { showcaseApi: api, setShowcaseIdentity: set } = await import(
    "./showcaseApi"
  );
  set("1");
  const p = await api.save({ productId: 1, accountId: 10, plannedQuantity: 5 });
  expect(p.totalAmount).toBe(300.3);
  await expect(
    api.save({ productId: 1, accountId: 20, plannedQuantity: 5 }),
  ).rejects.toThrow();
  set("2");
  expect(await api.preferences()).toEqual([]);
  await expect(api.remove(p.preferenceId, 0)).rejects.toThrow();
  set("4");
  await expect(
    api.product(1, {
      productName: "x",
      price: 61,
      feeRate: 0.001,
      active: true,
      version: 0,
    }),
  ).rejects.toThrow();
  set("3");
  await api.product(1, {
    productName: "更新名稱",
    price: 61,
    feeRate: 0.001,
    active: true,
    version: 0,
  });
  set("1");
  expect((await api.preferences())[0].priceSnapshot).toBe(60);
  const updated = await api.update(p.preferenceId, {
    productId: 1,
    accountId: 11,
    plannedQuantity: 2,
    version: 0,
  });
  expect(updated.priceSnapshot).toBe(61);
  await expect(api.remove(p.preferenceId, 0)).rejects.toThrow();
  await api.remove(p.preferenceId, 1);
  expect(await api.preferences()).toEqual([]);
});
