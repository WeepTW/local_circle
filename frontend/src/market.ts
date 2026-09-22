export interface Quote { symbol: string; price: number; asOf: string }
export interface MarketFeed { source: "sample" | "esun"; quotes: Quote[] }
export function parseFeed(input: unknown): MarketFeed {
  const f = input as MarketFeed;
  if (!f || !["sample", "esun"].includes(f.source) || !Array.isArray(f.quotes) || f.quotes.length > 2)
    throw new Error("行情格式無效");
  const seen = new Set<string>();
  for (const q of f.quotes) {
    if (!q || !["0050", "0052"].includes(q.symbol) || seen.has(q.symbol) ||
      typeof q.price !== "number" || !Number.isFinite(q.price) || q.price <= 0 || q.price > 999999999999 ||
      typeof q.asOf !== "string" || !Number.isFinite(Date.parse(q.asOf)) || Date.parse(q.asOf) > Date.now() + 60000)
      throw new Error("行情內容無效");
    seen.add(q.symbol);
  }
  return { source: f.source, quotes: f.quotes.map(q => ({symbol: q.symbol, price: q.price, asOf: q.asOf})) };
}
export function isFresh(q: Quote, now = Date.now()) {
  return now - Date.parse(q.asOf) <= 120000 && Date.parse(q.asOf) <= now + 60000;
}
