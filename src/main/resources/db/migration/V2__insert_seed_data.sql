-- =============================================================================
-- ClearFund V2 - seed data for local development / demos
--
-- IDs are assigned explicitly so the rows can reference each other, then the
-- sequences are advanced past the seeded values.
--   PostgreSQL: SELECT setval('seq', N, true);  -- next nextval() = N + 1
--   Oracle:     there is no setval(). Either create the sequences with
--               START WITH 100, or run:
--                 ALTER SEQUENCE fund_seq RESTART START WITH 4;  -- 12c+
-- =============================================================================

-- --- Funds (ISIN-like fund codes) -----------------------------------------
INSERT INTO funds (fund_id, fund_code, fund_name, currency, status, nav_per_unit, created_at, updated_at) VALUES
    (1, 'IE00B4L5Y983', 'ClearFund Global Equity', 'EUR', 'OPEN', 12.500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'LU0292096186', 'ClearFund Euro Bond',     'EUR', 'OPEN', 10.250000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'IE00B03HCZ61', 'ClearFund Money Market',  'EUR', 'OPEN',  1.000000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --- Accounts (one SUSPENDED, to exercise the validation path) -------------
INSERT INTO accounts (account_id, account_ref, name, email, status, created_at, updated_at) VALUES
    (1, 'ACC-1001', 'Jane Doe',            'jane.doe@example.com',      'ACTIVE',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'ACC-1002', 'John Smith',          'john.smith@example.com',    'ACTIVE',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'ACC-1003', 'Acme Pension Trust',  'ops@acmepension.example.com','SUSPENDED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --- EUR cash balances -----------------------------------------------------
INSERT INTO cash_balances (cash_balance_id, account_id, currency, amount, version, created_at, updated_at) VALUES
    (1, 1, 'EUR', 50000.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 2, 'EUR', 10000.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 3, 'EUR',  2500.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --- Initial holdings for account 1 ---------------------------------------
INSERT INTO holdings (holding_id, account_id, fund_id, units, version, created_at, updated_at) VALUES
    (1, 1, 1, 400.000000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 2, 150.000000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --- Sample orders in different lifecycle states ---------------------------
INSERT INTO fund_orders (order_id, order_ref, account_id, fund_id, order_type, status,
                         cash_amount, units, nav_used, trade_date, settlement_date,
                         reject_reason, created_at, updated_at) VALUES
    -- 1: completed subscription (full lifecycle, has an audit trail below)
    (1, 'ORD-20260515-0001', 1, 1, 'SUBSCRIPTION', 'SETTLED',
        5000.00, 400.000000, 12.500000, DATE '2026-05-15', DATE '2026-05-17',
        NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 2: brand new subscription, not yet processed
    (2, 'ORD-20260518-0002', 2, 2, 'SUBSCRIPTION', 'RECEIVED',
        2000.00, NULL, NULL, DATE '2026-05-18', DATE '2026-05-20',
        NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 3: redemption awaiting settlement
    (3, 'ORD-20260518-0003', 1, 1, 'REDEMPTION', 'SETTLEMENT_PENDING',
        625.00, 50.000000, 12.500000, DATE '2026-05-18', DATE '2026-05-20',
        NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 4: rejected subscription (insufficient cash)
    (4, 'ORD-20260518-0004', 3, 3, 'SUBSCRIPTION', 'REJECTED',
        999999.00, NULL, NULL, DATE '2026-05-18', DATE '2026-05-20',
        'Insufficient cash: required 999999.00 EUR, available 2500.00',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 5: cancelled subscription
    (5, 'ORD-20260517-0005', 1, 2, 'SUBSCRIPTION', 'CANCELLED',
        1000.00, NULL, NULL, DATE '2026-05-17', DATE '2026-05-19',
        'Cancelled: client changed mind', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --- Audit trail for order 1 (one row per transition) ----------------------
INSERT INTO audit_events (audit_id, order_id, order_ref, from_status, to_status, detail, created_at) VALUES
    (1, 1, 'ORD-20260515-0001', NULL,                 'RECEIVED',           'Order received',              CURRENT_TIMESTAMP),
    (2, 1, 'ORD-20260515-0001', 'RECEIVED',           'VALIDATED',          'Business validation passed',  CURRENT_TIMESTAMP),
    (3, 1, 'ORD-20260515-0001', 'VALIDATED',          'ROUTED',             'Routed to fund',              CURRENT_TIMESTAMP),
    (4, 1, 'ORD-20260515-0001', 'ROUTED',             'ACCEPTED',           'Accepted at NAV 12.500000',   CURRENT_TIMESTAMP),
    (5, 1, 'ORD-20260515-0001', 'ACCEPTED',           'SETTLEMENT_PENDING', 'Awaiting settlement',         CURRENT_TIMESTAMP),
    (6, 1, 'ORD-20260515-0001', 'SETTLEMENT_PENDING', 'SETTLED',            'Settled 400 units @ 12.5',    CURRENT_TIMESTAMP);

-- --- Settlement instruction for the pending redemption (order 3) -----------
INSERT INTO settlement_instructions (settlement_instruction_id, order_id, instruction_ref, status,
                                     settlement_date, amount, currency, created_at, updated_at) VALUES
    (1, 3, 'SI-20260518-0001', 'PENDING', DATE '2026-05-20', 625.00, 'EUR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --- Advance sequences past the seeded IDs (PostgreSQL) --------------------
-- Oracle: see the header note - use ALTER SEQUENCE ... RESTART START WITH.
SELECT setval('fund_seq', 3, true);
SELECT setval('account_seq', 3, true);
SELECT setval('cash_balance_seq', 3, true);
SELECT setval('holding_seq', 2, true);
SELECT setval('fund_order_seq', 5, true);
SELECT setval('audit_event_seq', 6, true);
SELECT setval('settlement_instruction_seq', 1, true);
