# ClearFund API — Examples

Base URL: `http://localhost:8080`

All error responses share one envelope:

```json
{ "timestamp": "2026-05-19T10:15:30Z", "status": 422, "error": "VALIDATION_FAILED",
  "messages": ["cashAmount must be greater than 0"], "path": "/api/orders" }
```

| Outcome | HTTP status |
|---|---|
| Order created | `201 Created` |
| Lifecycle step / read | `200 OK` |
| Bean-validation failure | `422 UNPROCESSABLE_ENTITY` (`VALIDATION_FAILED`) |
| Unknown account/fund/order | `404 NOT_FOUND` (`NOT_FOUND`) |
| Illegal state transition / business rule | `409 CONFLICT` (`BUSINESS_RULE_VIOLATION`) |

---

## Orders

### Place an order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{ "accountRef": "ACC-1", "fundCode": "CFEQ01",
        "orderType": "SUBSCRIPTION", "cashAmount": 5000.00 }'
```
```json
201 Created
{ "orderRef": "ORD-20260519-1A2B3C4D", "accountRef": "ACC-1", "fundCode": "CFEQ01",
  "orderType": "SUBSCRIPTION", "status": "RECEIVED", "cashAmount": 5000.00,
  "units": null, "navUsed": null, "tradeDate": "2026-05-19",
  "settlementDate": "2026-05-21", "rejectReason": null,
  "createdAt": "2026-05-19T10:15:30Z" }
```

A redemption supplies `units` instead of `cashAmount`:
```json
{ "accountRef": "ACC-1", "fundCode": "CFEQ01",
  "orderType": "REDEMPTION", "units": 100.000000 }
```

### List orders (paginated + filtered)
```bash
curl "http://localhost:8080/api/orders?status=SETTLED&type=SUBSCRIPTION&page=0&size=20&sort=id,desc"
```
```json
200 OK
{ "content": [ { "orderRef": "ORD-...", "status": "SETTLED", "...": "..." } ],
  "page": 0, "size": 20, "totalElements": 42, "totalPages": 3,
  "first": true, "last": false }
```
Both `status` and `type` are optional; omit them for an unfiltered list.

### Get one order
```bash
curl http://localhost:8080/api/orders/42        # -> 200 OrderResponse, or 404
```

### Drive the lifecycle
```bash
curl -X POST http://localhost:8080/api/orders/42/validate   # RECEIVED  -> VALIDATED (or REJECTED)
curl -X POST http://localhost:8080/api/orders/42/route       # VALIDATED -> ROUTED
curl -X POST http://localhost:8080/api/orders/42/accept      # ROUTED    -> ACCEPTED -> SETTLEMENT_PENDING
curl -X POST http://localhost:8080/api/orders/42/settle      # SETTLEMENT_PENDING -> SETTLED
```
Calling a step from the wrong state returns `409`:
```json
{ "status": 409, "error": "BUSINESS_RULE_VIOLATION",
  "messages": ["Order ORD-... cannot transition from RECEIVED to ROUTED"] }
```

### Cancel an order
```bash
curl -X POST http://localhost:8080/api/orders/42/cancel \
  -H 'Content-Type: application/json' -d '{ "reason": "client changed mind" }'
```
```json
200 OK   -> OrderResponse with "status": "CANCELLED", "rejectReason": "client changed mind"
```

### Audit trail
```bash
curl http://localhost:8080/api/orders/42/audit-events
```
```json
200 OK
[ { "fromStatus": null, "toStatus": "RECEIVED", "detail": "Order received",
    "createdAt": "2026-05-19T10:15:30Z" },
  { "fromStatus": "RECEIVED", "toStatus": "VALIDATED",
    "detail": "Business validation passed", "createdAt": "2026-05-19T10:16:00Z" } ]
```

---

## Accounts

```bash
curl http://localhost:8080/api/accounts/1/holdings
curl http://localhost:8080/api/accounts/1/cash-balances
```
```json
[ { "accountRef": "ACC-1", "fundCode": "CFEQ01",
    "fundName": "ClearFund Equity", "units": 400.000000 } ]

[ { "accountRef": "ACC-1", "currency": "GBP", "amount": 5000.00 } ]
```

---

## System

```bash
curl http://localhost:8080/api/system/health-summary
```
```json
200 OK
{ "status": "UP", "generatedAt": "2026-05-19T10:15:30Z",
  "totalAccounts": 3, "totalFunds": 2, "totalOrders": 42,
  "ordersByStatus": { "RECEIVED": 1, "VALIDATED": 0, "ROUTED": 0, "ACCEPTED": 0,
    "SETTLEMENT_PENDING": 4, "SETTLED": 35, "REJECTED": 1, "CANCELLED": 1 } }
```
