import type { Actor, Product, Account, Preference, Save } from "./types";
import type { httpApi } from "./httpApi";
import { ApiError } from "./errors";

// Isolated, synthetic browser state. No credentials, network access or bank integration.
let identity = 0,
  sequence = 1;
const actors: Actor[] = [
  {
    userId: 1,
    userName: "一般使用者",
    email: "user@example.test",
    role: "USER",
    labels: ["TW_ETF_OWNER"],
  },
  {
    userId: 2,
    userName: "其他使用者",
    email: "other@example.test",
    role: "USER",
    labels: [],
  },
  {
    userId: 3,
    userName: "台股商品管理員",
    email: "etf-admin@example.test",
    role: "ADMIN",
    labels: ["TW_ETF_OWNER"],
  },
  {
    userId: 4,
    userName: "全球商品管理員",
    email: "global-admin@example.test",
    role: "ADMIN",
    labels: ["GLOBAL_DEMO_OWNER"],
  },
];
const products: Product[] = [
  {
    productId: 1,
    productCode: "0050",
    productName: "元大台灣50 ETF",
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
    productName: "富邦科技 ETF",
    price: 180,
    feeRate: 0.001,
    currency: "TWD",
    ownerLabel: "TW_ETF_OWNER",
    active: true,
    version: 0,
  },
  {
    productId: 3,
    productCode: "GLOBAL_TOP10",
    productName: "全球前十大股票組合",
    price: 1000,
    feeRate: 0.0015,
    currency: "TWD",
    ownerLabel: "GLOBAL_DEMO_OWNER",
    active: true,
    version: 0,
  },
];
const accountOwners = [
  { id: 10, owner: 1, last4: "9666" },
  { id: 11, owner: 1, last4: "1122" },
  { id: 20, owner: 2, last4: "2222" },
  { id: 30, owner: 3, last4: "3333" },
  { id: 40, owner: 4, last4: "4444" },
];
const rows = new Map<number, { owner: number; value: Preference }>();
const fail = (status: number, detail: string): never => {
  throw new ApiError({ status, code: "SHOWCASE_ERROR", detail, traceId: "" });
};
const actor = () =>
  actors.find((a) => a.userId === identity) ?? fail(401, "請選擇有效角色。");
const copy = <T>(value: T): T => structuredClone(value);
export const setShowcaseIdentity = (id: string) => {
  identity = Number(id);
};
function owned(id: number) {
  const row = rows.get(id);
  if (!row || row.owner !== identity) fail(404, "找不到此喜好。");
  return row!;
}
function snapshot(
  c: Save,
): Omit<Preference, "preferenceId" | "version" | "savedAt" | "updatedAt"> {
  const p =
    products.find((p) => p.productId === c.productId && p.active) ??
    fail(404, "商品不存在或已停用。");
  const account =
    accountOwners.find((a) => a.id === c.accountId && a.owner === identity) ??
    fail(404, "找不到此帳戶。");
  if (
    !Number.isInteger(c.plannedQuantity) ||
    c.plannedQuantity < 1 ||
    c.plannedQuantity > 1000000
  )
    fail(400, "請輸入有效整數數量。");
  const base = p.price * c.plannedQuantity,
    fee = base * p.feeRate,
    total = base + fee;
  if (!Number.isFinite(total) || total >= 1e16)
    fail(400, "預計金額超出支援範圍。");
  return {
    productId: p.productId,
    productCode: p.productCode,
    productName: p.productName,
    accountId: account.id,
    maskedAccount: "******" + account.last4,
    userEmail: actor().email,
    plannedQuantity: c.plannedQuantity,
    priceSnapshot: p.price,
    feeRateSnapshot: p.feeRate,
    baseAmount: base,
    totalFee: fee,
    totalAmount: total,
  };
}
export const showcaseApi: typeof httpApi = {
  async me() {
    return copy(actor());
  },
  async products() {
    return copy(products.filter((p) => p.active));
  },
  async adminProducts() {
    if (actor().role !== "ADMIN") fail(403, "沒有管理權限。");
    return copy(products);
  },
  async accounts() {
    return accountOwners
      .filter((a) => a.owner === identity)
      .map(
        (a) =>
          ({
            accountId: a.id,
            maskedAccount: "******" + a.last4,
            currency: "TWD",
            version: 0,
          }) satisfies Account,
      );
  },
  async preferences() {
    return copy(
      [...rows.values()]
        .filter((r) => r.owner === identity)
        .map((r) => ({
          ...r.value,
          productName: products.find((p) => p.productId === r.value.productId)!
            .productName,
        }))
        .sort(
          (a, b) =>
            b.updatedAt.localeCompare(a.updatedAt) ||
            b.preferenceId - a.preferenceId,
        ),
    );
  },
  async save(c) {
    const now = new Date().toISOString();
    const value = {
      ...snapshot(c),
      preferenceId: sequence++,
      version: 0,
      savedAt: now,
      updatedAt: now,
    };
    rows.set(value.preferenceId, { owner: identity, value });
    return copy(value);
  },
  async update(id, c) {
    const row = owned(id);
    if (c.version !== row.value.version) fail(409, "資料已變更。");
    row.value = {
      ...snapshot(c),
      preferenceId: id,
      version: c.version + 1,
      savedAt: row.value.savedAt,
      updatedAt: new Date().toISOString(),
    };
    return copy(row.value);
  },
  async remove(id, version) {
    if (owned(id).value.version !== version) fail(409, "資料已變更。");
    rows.delete(id);
  },
  async product(id, c) {
    const p =
      products.find((p) => p.productId === id) ?? fail(404, "找不到商品。");
    const a = actor();
    if (a.role !== "ADMIN" || !a.labels.includes(p.ownerLabel))
      fail(403, "沒有管理權限。");
    if (c.version !== p.version) fail(409, "資料已變更。");
    if (
      !c.productName.trim() ||
      c.productName.length > 160 ||
      !Number.isFinite(c.price) ||
      c.price < 0 ||
      c.price > 999999999999 ||
      !Number.isFinite(c.feeRate) ||
      c.feeRate < 0 ||
      c.feeRate > 1
    )
      fail(400, "商品資料無效。");
    Object.assign(p, {
      productName: c.productName,
      price: c.price,
      feeRate: c.feeRate,
      active: c.active,
      version: p.version + 1,
    });
    return copy(p);
  },
};
