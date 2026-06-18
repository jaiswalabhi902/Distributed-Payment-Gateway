# PayGateway Console (Frontend)

A modern dashboard for the microservices payment platform, built with **React + Vite +
TypeScript + Tailwind + shadcn/ui**. It talks to the system exclusively through the **API
gateway**.

## Features

- JWT login with automatic access-token refresh (via the gateway).
- Dashboard: stat cards, status-distribution and volume charts (Recharts), recent activity.
- Payments: searchable/filterable table, create-payment dialog.
- Payment detail: full transaction view with status updates and refunds.
- Dark, responsive UI; toast notifications; loading skeletons.

## Tech

| Concern | Library |
|---|---|
| Build/dev | Vite |
| UI | React 18 + TypeScript |
| Styling | Tailwind CSS + shadcn/ui (Radix primitives) |
| Server state | TanStack Query |
| Auth state | Zustand (persisted) |
| HTTP | Axios (with JWT + refresh interceptors) |
| Charts | Recharts |
| Icons / toasts | lucide-react / sonner |

## Run locally

```bash
cd frontend
cp .env.example .env      # set VITE_API_URL to your gateway
npm install
npm run dev               # http://localhost:5173
```

`VITE_API_URL` defaults to `http://localhost:8090` (the canonical gateway). On a machine where
the gateway is published on a different port, set it accordingly (e.g. `http://localhost:18090`).

The gateway already enables permissive CORS, so the SPA can call it directly.

Default demo credentials: **admin / admin123**.

## Build

```bash
npm run build     # type-checks then bundles to dist/
npm run preview   # serve the production build
```
