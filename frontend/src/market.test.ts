import { it, expect, vi } from "vitest";
import { parseFeed, isFresh } from "./market";
it("rejects unsupported symbols, nonfinite prices, duplicates and future data", () => {
  const q = { symbol: "0050", price: 61, asOf: new Date().toISOString() };
  for (const quotes of [[{...q, symbol: "GLOBAL_TOP10"}], [{...q, price: Infinity}], [q, q], [{...q, asOf: "2099-01-01"}]]) {
    expect(() => parseFeed({source: "esun", quotes})).toThrow();
  }
  expect(isFresh({...q, asOf: "2020-01-01"})).toBe(false);
});
it("updates only eligible prices and preserves saved snapshots", async () => {
  vi.resetModules();
  const { showcaseApi: api, applyShowcaseQuotes } = await import("./showcaseApi");
  await api.save({productId: 1, accountId: 10, plannedQuantity: 5});
  applyShowcaseQuotes({source: "esun", quotes: [{symbol: "0050", price: 61, asOf: "2020-01-01"}]});
  expect((await api.products())[0].price).toBe(60);
  applyShowcaseQuotes({source: "sample", quotes: [{symbol: "0050", price: 61, asOf: "2020-01-01"}]});
  expect((await api.products())[0].price).toBe(61);
  expect((await api.preferences())[0].priceSnapshot).toBe(60);
  expect((await api.products())[2].price).toBe(1000);
});
