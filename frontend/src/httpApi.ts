import type {
  Actor,
  Account,
  Product,
  Preference,
  Save,
  ProductUpdate,
  Problem,
} from "./types";
import { ApiError } from "./errors";
let identity = "";
export const setHttpIdentity = (id: string) => {
  identity = id;
};
export async function request<T>(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  let response: Response;
  try {
    response = await fetch("/api/v1" + path, {
      method,
      headers: {
        "X-Demo-User-Id": identity,
        ...(body ? { "Content-Type": "application/json" } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(15000),
    });
  } catch {
    throw new Error("無法連線，請確認服務後重試。");
  }
  if (!response.ok) {
    let p: Problem;
    try {
      p = await response.json();
    } catch {
      p = {
        status: response.status,
        code: "HTTP_ERROR",
        detail: "服務暫時無法使用，請稍後重試。",
        traceId: "",
      };
    }
    throw new ApiError(p);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}
export const httpApi = {
  accountNumber: (id: number) =>
    request<{ accountNumber: string }>("/accounts/" + id + "/number"),
  me: () => request<Actor>("/me"),
  products: () => request<Product[]>("/products"),
  adminProducts: () => request<Product[]>("/admin/products"),
  accounts: () => request<Account[]>("/accounts"),
  preferences: () => request<Preference[]>("/preferences"),
  save: (body: Save) => request<Preference>("/preferences", "POST", body),
  update: (id: number, body: Save & { version: number }) =>
    request<Preference>("/preferences/" + id, "PUT", body),
  remove: (id: number, version: number) =>
    request<void>("/preferences/" + id + "?version=" + version, "DELETE"),
  product: (id: number, body: ProductUpdate) =>
    request<Product>("/admin/products/" + id, "PATCH", body),
};
export const money = (value: number) =>
  new Intl.NumberFormat("zh-TW", { maximumFractionDigits: 8 }).format(value);
