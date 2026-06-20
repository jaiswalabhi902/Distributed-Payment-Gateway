import { useQuery } from "@tanstack/react-query";
import { paymentService } from "@/services/payments";
import { useAuthStore } from "@/store/auth";

export function usePayments() {
  const userId = useAuthStore((s) => s.user?.userId);
  return useQuery({
    queryKey: ["payments", userId],
    queryFn: () => paymentService.listByUser(userId as number),
    enabled: typeof userId === "number",
  });
}
