import { api } from "@/lib/api";
import type { ApiResponse, AuthResponse, UserProfile } from "@/types";

export const authService = {
  async login(username: string, password: string) {
    const res = await api.post<ApiResponse<AuthResponse>>("/api/auth/login", {
      username,
      password,
    });
    return res.data.data;
  },

  async register(username: string, email: string, password: string) {
    const res = await api.post<ApiResponse<UserProfile>>("/api/auth/register", {
      username,
      email,
      password,
    });
    return res.data.data;
  },

  async me() {
    const res = await api.get<ApiResponse<UserProfile>>("/api/auth/me");
    return res.data.data;
  },

  async logout() {
    try {
      await api.post("/api/auth/logout");
    } catch {
      /* best effort */
    }
  },
};
