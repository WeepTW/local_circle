import type { LoginProvider, LoginSuggestion } from "../contracts";
// Public demonstration values only. Never use these accounts in a real identity service.
const suggestions: readonly LoginSuggestion[] = [
  { id: "1", label: "一般使用者", username: "user@example.test", password: "Preview-User-1" },
  { id: "2", label: "其他使用者", username: "other@example.test", password: "Preview-User-2" },
  { id: "3", label: "台股商品管理員", username: "etf-admin@example.test", password: "Preview-Etf-3" },
  { id: "4", label: "全球商品管理員", username: "global-admin@example.test", password: "Preview-Global-4" },
];
export const demoProvider: LoginProvider = {
  suggestions,
  async authenticate({ username, password }) {
    const account = suggestions.find(a => a.username === username.trim() && a.password === password);
    if (!account) throw new Error("帳號或密碼不正確，請重新輸入。");
    return { userId: account.id };
  },
};

export const loginProvider = demoProvider;
