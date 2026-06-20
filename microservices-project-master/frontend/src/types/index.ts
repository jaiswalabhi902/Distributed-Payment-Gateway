export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  username: string;
  email: string;
  roles: string[];
}

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  enabled: boolean;
  roles: string[];
  createdAt: string;
}

export type PaymentStatus =
  | "PENDING"
  | "PROCESSING"
  | "SUCCESS"
  | "FAILED"
  | "REFUNDED"
  | "PARTIAL_REFUND";

export type PaymentMethod =
  | "CREDIT_CARD"
  | "DEBIT_CARD"
  | "UPI"
  | "NET_BANKING"
  | "WALLET"
  | "BANK_TRANSFER"
  | "CRYPTO";

export interface Payment {
  id: number;
  orderId: string;
  userId: number;
  amount: number;
  currency: string;
  status: PaymentStatus;
  paymentMethod: PaymentMethod;
  transactionId: string;
  description?: string;
  refundedAmount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentRequest {
  orderId: string;
  amount: number;
  currency: string;
  paymentMethod: PaymentMethod;
  description?: string;
}

export const PAYMENT_STATUSES: PaymentStatus[] = [
  "PENDING",
  "PROCESSING",
  "SUCCESS",
  "FAILED",
  "REFUNDED",
  "PARTIAL_REFUND",
];

export const PAYMENT_METHODS: PaymentMethod[] = [
  "CREDIT_CARD",
  "DEBIT_CARD",
  "UPI",
  "NET_BANKING",
  "WALLET",
  "BANK_TRANSFER",
  "CRYPTO",
];
