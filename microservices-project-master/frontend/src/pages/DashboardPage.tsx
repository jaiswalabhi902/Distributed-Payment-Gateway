import { useMemo } from "react";
import { Link } from "react-router-dom";
import {
  Bar,
  BarChart,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { CreditCard, DollarSign, RotateCcw, TrendingUp } from "lucide-react";
import { usePayments } from "@/hooks/usePayments";
import { useAuthStore } from "@/store/auth";
import { formatCurrency, formatDate } from "@/lib/utils";
import type { Payment } from "@/types";
import { CreatePaymentDialog } from "@/components/payments/CreatePaymentDialog";
import { StatusBadge } from "@/components/payments/StatusBadge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

const STATUS_COLORS: Record<string, string> = {
  PENDING: "#f59e0b",
  PROCESSING: "#0ea5e9",
  SUCCESS: "#10b981",
  FAILED: "#ef4444",
  REFUNDED: "#94a3b8",
  PARTIAL_REFUND: "#6366f1",
};

function StatCard({
  title,
  value,
  icon: Icon,
  hint,
}: {
  title: string;
  value: string;
  icon: React.ElementType;
  hint?: string;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-semibold">{value}</div>
        {hint && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
      </CardContent>
    </Card>
  );
}

export function DashboardPage() {
  const { data, isLoading } = usePayments();
  const username = useAuthStore((s) => s.user?.username);

  const stats = useMemo(() => {
    const list: Payment[] = data ?? [];
    const total = list.length;
    const amount = list.reduce((s, p) => s + p.amount, 0);
    const refunded = list.reduce((s, p) => s + (p.refundedAmount ?? 0), 0);
    const success = list.filter((p) => p.status === "SUCCESS").length;
    const byStatus = Object.entries(
      list.reduce<Record<string, number>>((acc, p) => {
        acc[p.status] = (acc[p.status] ?? 0) + 1;
        return acc;
      }, {})
    ).map(([name, value]) => ({ name, value }));
    const byMethod = Object.entries(
      list.reduce<Record<string, number>>((acc, p) => {
        acc[p.paymentMethod] = (acc[p.paymentMethod] ?? 0) + p.amount;
        return acc;
      }, {})
    ).map(([name, value]) => ({ name: name.replace(/_/g, " "), value: Number(value.toFixed(2)) }));
    const recent = [...list].sort((a, b) => b.id - a.id).slice(0, 5);
    return {
      total,
      amount,
      refunded,
      successRate: total ? Math.round((success / total) * 100) : 0,
      byStatus,
      byMethod,
      recent,
    };
  }, [data]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Welcome back{username ? `, ${username}` : ""}
          </h1>
          <p className="text-sm text-muted-foreground">Here’s your payment activity overview.</p>
        </div>
        <CreatePaymentDialog />
      </div>

      {/* Stat cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {isLoading ? (
          Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28" />)
        ) : (
          <>
            <StatCard title="Total payments" value={String(stats.total)} icon={CreditCard} />
            <StatCard
              title="Total volume"
              value={formatCurrency(stats.amount)}
              icon={DollarSign}
            />
            <StatCard
              title="Success rate"
              value={`${stats.successRate}%`}
              icon={TrendingUp}
              hint="Payments marked SUCCESS"
            />
            <StatCard
              title="Total refunded"
              value={formatCurrency(stats.refunded)}
              icon={RotateCcw}
            />
          </>
        )}
      </div>

      {/* Charts */}
      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Status distribution</CardTitle>
          </CardHeader>
          <CardContent>
            {stats.byStatus.length === 0 ? (
              <p className="py-16 text-center text-sm text-muted-foreground">No data yet.</p>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={stats.byStatus}
                    dataKey="value"
                    nameKey="name"
                    innerRadius={60}
                    outerRadius={95}
                    paddingAngle={3}
                  >
                    {stats.byStatus.map((entry) => (
                      <Cell key={entry.name} fill={STATUS_COLORS[entry.name] ?? "#6366f1"} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      background: "hsl(222 44% 9%)",
                      border: "1px solid hsl(217 33% 18%)",
                      borderRadius: 8,
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            )}
            <div className="mt-2 flex flex-wrap justify-center gap-3">
              {stats.byStatus.map((s) => (
                <span key={s.name} className="flex items-center gap-1.5 text-xs">
                  <span
                    className="h-2.5 w-2.5 rounded-full"
                    style={{ background: STATUS_COLORS[s.name] ?? "#6366f1" }}
                  />
                  {s.name.replace(/_/g, " ")} ({s.value})
                </span>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Volume by method</CardTitle>
          </CardHeader>
          <CardContent>
            {stats.byMethod.length === 0 ? (
              <p className="py-16 text-center text-sm text-muted-foreground">No data yet.</p>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={stats.byMethod}>
                  <XAxis
                    dataKey="name"
                    tick={{ fontSize: 11, fill: "hsl(215 20% 65%)" }}
                    interval={0}
                    angle={-20}
                    textAnchor="end"
                    height={60}
                  />
                  <YAxis tick={{ fontSize: 11, fill: "hsl(215 20% 65%)" }} />
                  <Tooltip
                    cursor={{ fill: "hsl(217 33% 17% / 0.4)" }}
                    contentStyle={{
                      background: "hsl(222 44% 9%)",
                      border: "1px solid hsl(217 33% 18%)",
                      borderRadius: 8,
                    }}
                  />
                  <Bar dataKey="value" fill="#6366f1" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Recent */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-base">Recent payments</CardTitle>
          <Button asChild variant="ghost" size="sm">
            <Link to="/payments">View all</Link>
          </Button>
        </CardHeader>
        <CardContent className="space-y-1">
          {isLoading && <Skeleton className="h-32 w-full" />}
          {!isLoading && stats.recent.length === 0 && (
            <p className="py-8 text-center text-sm text-muted-foreground">No payments yet.</p>
          )}
          {stats.recent.map((p) => (
            <Link
              key={p.id}
              to={`/payments/${p.id}`}
              className="flex items-center justify-between rounded-lg px-3 py-2.5 transition-colors hover:bg-accent"
            >
              <div className="flex flex-col">
                <span className="text-sm font-medium">{p.orderId}</span>
                <span className="text-xs text-muted-foreground">{formatDate(p.createdAt)}</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-sm font-medium">
                  {formatCurrency(p.amount, p.currency)}
                </span>
                <StatusBadge status={p.status} />
              </div>
            </Link>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
