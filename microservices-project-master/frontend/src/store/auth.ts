import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { AuthResponse } from "@/types";

interface AuthUser {
  userId: number;
  username: string;
  email: string;
  roles: string[];
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  setAuth: (auth: AuthResponse) => void;
  setAccessToken: (token: string) => void;
  clear: () => void;
  hasRole: (role: string) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      setAuth: (auth) =>
        set({
          accessToken: auth.accessToken,
          refreshToken: auth.refreshToken,
          user: {
            userId: auth.userId,
            username: auth.username,
            email: auth.email,
            roles: auth.roles,
          },
          isAuthenticated: true,
        }),
      setAccessToken: (token) => set({ accessToken: token }),
      clear: () =>
        set({
          accessToken: null,
          refreshToken: null,
          user: null,
          isAuthenticated: false,
        }),
      hasRole: (role) => get().user?.roles?.includes(role) ?? false,
    }),
    { name: "paygateway-auth" }
  )
);
