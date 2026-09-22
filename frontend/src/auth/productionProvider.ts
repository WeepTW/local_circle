import type { LoginProvider } from "./contracts";
// Replace this provider with server-verified login. Never fall back to demonstration identities.
export const productionProvider: LoginProvider = {
  async authenticate() {
    throw new Error("登入服務尚未設定，請聯絡系統管理員。");
  },
};

export const loginProvider = productionProvider;
