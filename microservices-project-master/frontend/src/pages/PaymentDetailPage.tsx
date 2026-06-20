import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Loader2, RotateCcw } from "lucide-react";
import { toast } from "sonner";
import { paymentService } from "@/services/payments";
import { apiError } from "@/lib/api";
import { formatCurrency, formatDate } from "@/lib/utils";
import { PAYMENT_STATUSES, type Payment, type PaymentStatus } from "@/types";
import { StatusBadge } from "@/components/payments/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

function Detail({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-4 border-b border-border/40 py-3 last:border-0">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className="text-sm font-medium text-right">{value}</span>
    </div>
  );
}

export function PaymentDetailPage() {
  const { id } = useParams();
  const paymentId = Number(id);
  const qc = useQueryClient();

  const { data: payment, isLoading } = useQuery({
    queryKey: ["payment", paymentId],
    queryFn: () => paymentService.get(paymentId),
    enabled: Number.isFinite(paymentId),
  });

  const [status, setStatus] = useState<PaymentStatus | "">("");
  const [refundAmount, setRefundAmount] = useState("");

  const refresh = (updated: Payment) => {
    qc.setQueryData(["payment", paymentId], updated);
    qc.invalidateQueries({ queryKey: ["payments"] });
  };

  const statusMutation = useMutation({
    mutationFn: () => paymentService.updateStatus(paymentId, status as PaymentStatus),
    onSuccess: (p) => {
      toast.success(`Status updated to ${p.status}`);
      refresh(p);
      setStatus("");
    },
    onError: (e) => toast.error(apiError(e)),
  });

  const refundMutation = useMutation({
    mutationFn: () => paymentService.refund(paymentId, Number(refundAmount), "Console refund"),
    onSuccess: (p) => {
      toast.success(`Refunded ${formatCurrency(Number(refundAmount), p.currency)}`);
      refresh(p);
      setRefundAmount("");
    },
    onError: (e) => toast.error(apiError(e)),
  });

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full max-w-2xl" />
      </div>
    );
  }

  if (!payment) {
    return (
      <div className="space-y-4">
        <p>Payment not found.</p>
        <Button asChild variant="outline">
          <Link to="/payments">Back to payments</Link>
        </Button>
      </div>
    );
  }

  const refundable = payment.amount - (payment.refundedAmount ?? 0);

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button asChild variant="ghost" size="icon">
          <Link to="/payments">
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{payment.orderId}</h1>
          <p className="text-sm text-muted-foreground">{payment.transactionId}</p>
        </div>
        <div className="ml-auto">
          <StatusBadge status={payment.status} />
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Transaction details</CardTitle>
          </CardHeader>
          <CardContent>
            <Detail label="Amount" value={formatCurrency(payment.amount, payment.currency)} />
            <Detail
              label="Refunded"
              value={formatCurrency(payment.refundedAmount ?? 0, payment.currency)}
            />
            <Detail label="Payment method" value={payment.paymentMethod.replace(/_/g, " ")} />
            <Detail label="Currency" value={payment.currency} />
            <Detail label="User ID" value={payment.userId} />
            <Detail label="Description" value={payment.description || "—"} />
            <Detail label="Created" value={formatDate(payment.createdAt)} />
            <Detail label="Updated" value={formatDate(payment.updatedAt)} />
          </CardContent>
        </Card>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Update status</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <Select value={status} onValueChange={(v) => setStatus(v as PaymentStatus)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select status" />
                </SelectTrigger>
                <SelectContent>
                  {PAYMENT_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {s.replace(/_/g, " ")}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button
                className="w-full"
                disabled={!status || statusMutation.isPending}
                onClick={() => statusMutation.mutate()}
              >
                {statusMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                Apply
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Refund</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="space-y-2">
                <Label htmlFor="refund">
                  Amount{" "}
                  <span className="text-muted-foreground">
                    (up to {formatCurrency(refundable, payment.currency)})
                  </span>
                </Label>
                <Input
                  id="refund"
                  type="number"
                  step="0.01"
                  min="0.01"
                  max={refundable}
                  value={refundAmount}
                  onChange={(e) => setRefundAmount(e.target.value)}
                  placeholder="0.00"
                />
              </div>
              <Button
                variant="destructive"
                className="w-full"
                disabled={!refundAmount || refundable <= 0 || refundMutation.isPending}
                onClick={() => refundMutation.mutate()}
              >
                {refundMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <RotateCcw className="h-4 w-4" />
                )}
                Process refund
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
