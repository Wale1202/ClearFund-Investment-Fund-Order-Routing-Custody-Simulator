-- =============================================================================
-- ClearFund V3 - performance indexes
--
-- Primary keys and UNIQUE constraints already create indexes implicitly, so
-- the indexes below only cover the *non-unique* access paths the application
-- actually uses. Each is justified against a real query.
--
-- Oracle notes:
--   * CREATE INDEX syntax is identical; PostgreSQL's "IF NOT EXISTS" is not
--     supported on Oracle, so it is omitted (Flyway runs each script once).
--   * Neither PostgreSQL nor Oracle auto-indexes foreign-key columns, which
--     is why the account_id / fund_id indexes below are created explicitly.
-- =============================================================================

-- fund_orders.status
-- Used by: GET /api/orders?status=...  (FundOrderRepository.search),
--          SystemService health summary (GROUP BY status),
--          the settlement engine's status filter.
CREATE INDEX idx_fund_orders_status ON fund_orders (status);

-- fund_orders (status, settlement_date) - composite
-- Directly serves FundOrderRepository
--   .findByStatusAndSettlementDateLessThanEqual(SETTLEMENT_PENDING, today),
-- the query the scheduled settlement engine runs on every tick. The leading
-- column also covers status-only lookups, but the dedicated status index
-- above is kept for the GROUP BY / count path.
CREATE INDEX idx_fund_orders_status_settlement ON fund_orders (status, settlement_date);

-- fund_orders.account_id
-- Used by: filtering/joining orders for a given account.
CREATE INDEX idx_fund_orders_account_id ON fund_orders (account_id);

-- fund_orders.fund_id
-- Used by: per-fund order reporting and joins to funds.
CREATE INDEX idx_fund_orders_fund_id ON fund_orders (fund_id);

-- fund_orders.created_at
-- Used by: recency ordering and date-range reporting on orders.
CREATE INDEX idx_fund_orders_created_at ON fund_orders (created_at);

-- audit_events (order_ref, created_at) - composite
-- Directly serves AuditEventRepository
--   .findByOrderRefOrderByCreatedAtAsc(orderRef): the composite lets the DB
-- both filter by order_ref and return rows already in created_at order,
-- avoiding a sort for GET /api/orders/{id}/audit-events.
CREATE INDEX idx_audit_events_order_ref_created ON audit_events (order_ref, created_at);

-- audit_events.order_id
-- Used by: reconciling the audit trail by numeric order id.
CREATE INDEX idx_audit_events_order_id ON audit_events (order_id);

-- audit_events.created_at
-- Used by: operational/time-window queries across the whole audit log.
CREATE INDEX idx_audit_events_created_at ON audit_events (created_at);

-- Note: holdings(account_id) and cash_balances(account_id) are intentionally
-- NOT indexed here - the UNIQUE constraints uq_holding_account_fund and
-- uq_cash_account_ccy already create composite indexes whose leading column
-- is account_id, which the database can use for the
-- findByAccountId(...) lookups. Adding standalone indexes would be redundant.
