import { Badge } from "@/components/ui/badge";
import type { PaymentStatus } from "@/types";

const META: Record<PaymentStatus, { label: string; variant: "default" | "secondary" | "success" | "warning" | "info" | "destructive" }> = {
  PENDING: { label: "Pending", variant: "warning" },
  PROCESSING: { label: "Processing", variant: "info" },
  SUCCESS: { label: "Success", variant: "success" },
  FAILED: { label: "Failed", variant: "destructive" },
  REFUNDED: { label: "Refunded", variant: "secondary" },
  PARTIAL_REFUND: { label: "Partial refund", variant: "default" },
};

export function StatusBadge({ status }: { status: PaymentStatus }) {
  const meta = META[status] ?? { label: status, variant: "secondary" as const };
  return <Badge variant={meta.variant}>{meta.label}</Badge>;
}
