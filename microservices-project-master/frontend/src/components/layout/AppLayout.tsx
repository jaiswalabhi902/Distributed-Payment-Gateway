import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { CreditCard, LayoutDashboard, LogOut, ShieldCheck, User } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth";
import { authService } from "@/services/auth";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const NAV = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard, end: true },
  { to: "/payments", label: "Payments", icon: CreditCard, end: false },
];

export function AppLayout() {
  const user = useAuthStore((s) => s.user);
  const clear = useAuthStore((s) => s.clear);
  const navigate = useNavigate();

  const handleLogout = async () => {
    await authService.logout();
    clear();
    toast.success("Signed out");
    navigate("/login");
  };

  const initials = user?.username?.slice(0, 2).toUpperCase() ?? "U";

  return (
    <div className="flex min-h-screen">
      {/* Sidebar */}
      <aside className="hidden w-64 flex-col border-r border-border/50 bg-card/40 backdrop-blur-md md:flex">
        <div className="flex h-16 items-center gap-2 border-b border-border/50 px-6">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <span className="text-lg font-semibold tracking-tight">PayGateway</span>
        </div>
        <nav className="flex-1 space-y-1 p-4">
          {NAV.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-primary/15 text-primary"
                    : "text-muted-foreground hover:bg-accent hover:text-foreground"
                )
              }
            >
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-border/50 p-4 text-xs text-muted-foreground">
          Signed in as
          <div className="truncate font-medium text-foreground">{user?.email}</div>
        </div>
      </aside>

      {/* Main */}
      <div className="flex flex-1 flex-col">
        <header className="flex h-16 items-center justify-between border-b border-border/50 bg-card/30 px-6 backdrop-blur-md">
          <div className="md:hidden flex items-center gap-2 font-semibold">
            <ShieldCheck className="h-5 w-5 text-primary" /> PayGateway
          </div>
          <div className="flex-1" />
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="gap-2">
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/15 text-xs font-semibold text-primary">
                  {initials}
                </span>
                <span className="hidden sm:inline">{user?.username}</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuLabel>
                <div className="flex flex-col">
                  <span>{user?.username}</span>
                  <span className="text-xs font-normal text-muted-foreground">
                    {user?.roles?.join(", ")}
                  </span>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onSelect={() => navigate("/payments")}>
                <User className="h-4 w-4" /> My payments
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={handleLogout} className="text-red-400">
                <LogOut className="h-4 w-4" /> Sign out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </header>

        <main className="flex-1 p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
