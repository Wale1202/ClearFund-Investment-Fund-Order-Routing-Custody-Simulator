# ClearFund Frontend

A deliberately small Angular 17 dashboard for the ClearFund backend. Standalone
components, no UI framework, plain CSS. The backend is the focus of the
portfolio project — this is just a clean operator view on top of it.

## Run

```bash
cd frontend
npm install
npm start            # ng serve on http://localhost:4200
```

Start the backend first (`scripts/run-local.sh` in the project root). The dev
server proxies `/api` → `http://localhost:8080` via `proxy.conf.json`, so no
CORS configuration is needed on the backend during development.

Production build: `npm run build` (uses `environment.prod.ts`, where
`apiUrl` is `/api` — serve the SPA behind a reverse proxy that also exposes the
backend at `/api`).

## Structure

```
src/app/
  app.component.ts        shell + top navigation
  app.routes.ts           lazy-loaded routes
  app.config.ts           router + HttpClient providers
  models/                 TypeScript interfaces mirroring the backend DTOs
  services/api.service.ts single typed gateway to the REST API
  shared/status-class.ts  status → badge colour helper
  pages/
    order-book/           list + filter + paginate orders
    create-order/         reactive form (subscription / redemption)
    order-details/        one order + lifecycle actions + audit trail
    failed-orders/        REJECTED orders with reasons
    settlement-queue/     SETTLEMENT_PENDING orders + Settle action
    account-holdings/     custody positions for an account id
    cash-balances/        cash positions for an account id
    system-health/        counts + orders-by-status breakdown
```

## Pages → API

| Page | Endpoint(s) |
|---|---|
| Order Book | `GET /api/orders` (status/type filters, paging) |
| Create Order | `POST /api/orders` |
| Order Details | `GET /api/orders/{id}`, `/validate /route /accept /settle /cancel`, `/audit-events` |
| Failed Orders | `GET /api/orders?status=REJECTED` |
| Settlement Queue | `GET /api/orders?status=SETTLEMENT_PENDING`, `POST /api/orders/{id}/settle` |
| Account Holdings | `GET /api/accounts/{id}/holdings` |
| Cash Balances | `GET /api/accounts/{id}/cash-balances` |
| System Health | `GET /api/system/health-summary` |

## Known limitation (worth raising in an interview)

The lifecycle endpoints are keyed by the **numeric order id** (`/api/orders/{id}`),
but the backend's `OrderResponse` currently exposes only `orderRef`, not `id`.
So the Order Book / Settlement Queue rows can display every order, but the
**per-order links and action buttons only light up once an `id` is present**
(the `Order` model declares `id` as optional and the templates guard on it).

The clean fix is a one-line backend change: add `id` to `OrderResponse` (and
its mapper). The frontend is already written to consume it. I left the backend
as-is because this task was scoped to the frontend, but this is exactly the
kind of API/UI contract gap worth calling out rather than hiding.

## Notes

- Reactive forms with cross-field validation: only the relevant amount field
  (cash for subscription, units for redemption) is required, mirroring the
  backend rule.
- Error handling is intentionally light (inline messages), not a global
  interceptor — appropriate for a portfolio dashboard, not production.
