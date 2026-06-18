import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2, Plus } from "lucide-react";
import { toast } from "sonner";
import { paymentService } from "@/services/payments";
import { apiError } from "@/lib/api";
import { PAYMENT_METHODS, type PaymentMethod } from "@/types";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const CURRENCIES = ["USD", "EUR", "GBP", "INR", "AUD", "CAD", "SGD"];

function newOrderId() {
  return `ORD-${Date.now().toString().slice(-8)}-${Math.floor(Math.random() * 1000)}`;
}

export function CreatePaymentDialog() {
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const [orderId, setOrderId] = useState(newOrderId());
  const [amount, setAmount] = useState("100.00");
  const [currency, setCurrency] = useState("USD");
  const [method, setMethod] = useState<PaymentMethod>("CREDIT_CARD");
  const [description, setDescription] = useState("");

  const mutation = useMutation({
    mutationFn: () =>
      paymentService.create({
        orderId,
        amount: Number(amount),
        currency,
        paymentMethod: method,
        description: description || undefined,
      }),
    onSuccess: (p) => {
      toast.success(`Payment ${p.orderId} created`);
      qc.invalidateQueries({ queryKey: ["payments"] });
      setOpen(false);
      setOrderId(newOrderId());
      setDescription("");
    },
    onError: (e) => toast.error(apiError(e)),
  });

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>
          <Plus className="h-4 w-4" /> New payment
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Create payment</DialogTitle>
          <DialogDescription>
            Submit a new payment through the gateway.
          </DialogDescription>
        </DialogHeader>

        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            mutation.mutate();
          }}
        >
          <div className="space-y-2">
            <Label htmlFor="orderId">Order ID</Label>
            <Input id="orderId" value={orderId} onChange={(e) => setOrderId(e.target.value)} required />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="amount">Amount</Label>
              <Input
                id="amount"
                type="number"
                step="0.01"
                min="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label>Currency</Label>
              <Select value={currency} onValueChange={setCurrency}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {CURRENCIES.map((c) => (
                    <SelectItem key={c} value={c}>
                      {c}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label>Payment method</Label>
            <Select value={method} onValueChange={(v) => setMethod(v as PaymentMethod)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {PAYMENT_METHODS.map((m) => (
                  <SelectItem key={m} value={m}>
                    {m.replace(/_/g, " ")}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description (optional)</Label>
            <Input
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. Order for invoice #1024"
            />
          </div>

          <DialogFooter>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Create payment
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
