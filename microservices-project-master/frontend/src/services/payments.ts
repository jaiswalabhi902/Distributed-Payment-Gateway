import { api } from "@/lib/api";
import type {
  ApiResponse,
  CreatePaymentRequest,
  Payment,
  PaymentStatus,
} from "@/types";

export const paymentService = {
  async listByUser(userId: number) {
    const res = await api.get<ApiResponse<Payment[]>>(`/api/payments/user/${userId}`);
    return res.data.data;
  },

  async get(id: number) {
    const res = await api.get<ApiResponse<Payment>>(`/api/payments/${id}`);
    return res.data.data;
  },

  async create(payload: CreatePaymentRequest) {
    const res = await api.post<ApiResponse<Payment>>("/api/payments/create", payload);
    return res.data.data;
  },

  async updateStatus(id: number, status: PaymentStatus, reason?: string) {
    const res = await api.put<ApiResponse<Payment>>(`/api/payments/${id}/status`, {
      status,
      reason,
    });
    return res.data.data;
  },

  async refund(id: number, refundAmount: number, refundReason?: string) {
    const res = await api.post<ApiResponse<Payment>>(`/api/payments/${id}/refund`, {
      refundAmount,
      refundReason,
    });
    return res.data.data;
  },

  async count(userId: number) {
    const res = await api.get<ApiResponse<number>>(`/api/payments/user/${userId}/count`);
    return res.data.data;
  },
};
