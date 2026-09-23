export interface Actor {
  userId: number;
  userName: string;
  email: string;
  role: string;
  labels: string[];
}
export interface Product {
  productId: number;
  productCode: string;
  productName: string;
  price: number;
  feeRate: number;
  currency: string;
  ownerLabel: string;
  active: boolean;
  version: number;
}
export interface Account {
  accountId: number;
  maskedAccount: string;
  currency: string;
  version: number;
}
export interface Preference {
  preferenceId: number;
  productId: number | null;
  productCode: string;
  productName: string;
  accountId: number;
  maskedAccount: string;
  accountNumberAvailable?: boolean;
  userEmail: string;
  plannedQuantity: number;
  priceSnapshot: number;
  feeRateSnapshot: number;
  baseAmount: number;
  totalFee: number;
  totalAmount: number;
  version: number;
  savedAt: string;
  updatedAt: string;
}
export interface Save {
  productId?: number | null;
  productName?: string;
  price?: number;
  feeRate?: number;
  accountId?: number;
  accountNumber?: string;
  plannedQuantity: number;
}
export interface ProductUpdate {
  productName: string;
  price: number;
  feeRate: number;
  active: boolean;
  version: number;
}
export interface Problem {
  status: number;
  code: string;
  detail: string;
  traceId: string;
}
