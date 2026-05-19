-- =============================================================================
-- ClearFund V1 - core schema
--
-- Written in PostgreSQL syntax. Oracle differences are called out inline as
-- "-- Oracle:" notes. Key portability choices:
--   * Explicit sequences (not BIGSERIAL / IDENTITY) so the same DDL maps
--     cleanly onto Oracle, which has no SERIAL type.
--   * VARCHAR              -> Oracle: VARCHAR2
--   * NUMERIC(p,s)         -> Oracle: NUMBER(p,s)
--   * TIMESTAMP WITH TIME ZONE matches Hibernate's default mapping for
--     java.time.Instant.   -> Oracle: TIMESTAMP WITH TIME ZONE (same)
-- =============================================================================

-- --- Sequences -------------------------------------------------------------
-- Names match the @SequenceGenerator(sequenceName = ...) on each entity.
-- Oracle: CREATE SEQUENCE fund_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE fund_seq                   START 1 INCREMENT 1;
CREATE SEQUENCE account_seq                START 1 INCREMENT 1;
CREATE SEQUENCE cash_balance_seq           START 1 INCREMENT 1;
CREATE SEQUENCE holding_seq                START 1 INCREMENT 1;
CREATE SEQUENCE fund_order_seq             START 1 INCREMENT 1;
CREATE SEQUENCE audit_event_seq            START 1 INCREMENT 1;
CREATE SEQUENCE settlement_instruction_seq START 1 INCREMENT 1;

-- --- funds -----------------------------------------------------------------
-- fund_code holds an ISIN-like identifier (ISINs are 12 chars, which is why
-- the column is VARCHAR(12)).
CREATE TABLE funds (
    fund_id      BIGINT         NOT NULL,
    fund_code    VARCHAR(12)    NOT NULL,
    fund_name    VARCHAR(160)   NOT NULL,
    currency     VARCHAR(3)     NOT NULL,
    status       VARCHAR(20)    NOT NULL,
    nav_per_unit NUMERIC(18, 6) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_funds PRIMARY KEY (fund_id),
    CONSTRAINT uq_funds_fund_code UNIQUE (fund_code)
);

-- --- accounts --------------------------------------------------------------
CREATE TABLE accounts (
    account_id  BIGINT       NOT NULL,
    account_ref VARCHAR(24)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    email       VARCHAR(160) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (account_id),
    CONSTRAINT uq_accounts_account_ref UNIQUE (account_ref),
    CONSTRAINT uq_accounts_email UNIQUE (email)
);

-- --- cash_balances ---------------------------------------------------------
-- One row per (account, currency). "version" backs JPA optimistic locking.
CREATE TABLE cash_balances (
    cash_balance_id BIGINT         NOT NULL,
    account_id      BIGINT         NOT NULL,
    currency        VARCHAR(3)     NOT NULL,
    amount          NUMERIC(18, 2) NOT NULL,
    version         BIGINT         NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_cash_balances PRIMARY KEY (cash_balance_id),
    CONSTRAINT uq_cash_account_ccy UNIQUE (account_id, currency),
    CONSTRAINT fk_cash_account FOREIGN KEY (account_id) REFERENCES accounts (account_id)
);

-- --- holdings --------------------------------------------------------------
-- Custody positions: one row per (account, fund).
CREATE TABLE holdings (
    holding_id BIGINT         NOT NULL,
    account_id BIGINT         NOT NULL,
    fund_id    BIGINT         NOT NULL,
    units      NUMERIC(18, 6) NOT NULL,
    version    BIGINT         NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_holdings PRIMARY KEY (holding_id),
    CONSTRAINT uq_holding_account_fund UNIQUE (account_id, fund_id),
    CONSTRAINT fk_holding_account FOREIGN KEY (account_id) REFERENCES accounts (account_id),
    CONSTRAINT fk_holding_fund    FOREIGN KEY (fund_id)    REFERENCES funds (fund_id)
);

-- --- fund_orders -----------------------------------------------------------
-- Named "fund_orders" because ORDER is a reserved word in Oracle and SQL.
-- cash_amount/units/nav_used are nullable: which side is supplied vs derived
-- depends on the order type and lifecycle stage.
CREATE TABLE fund_orders (
    order_id        BIGINT         NOT NULL,
    order_ref       VARCHAR(24)    NOT NULL,
    account_id      BIGINT         NOT NULL,
    fund_id         BIGINT         NOT NULL,
    order_type      VARCHAR(12)    NOT NULL,
    status          VARCHAR(20)    NOT NULL,  -- longest value: SETTLEMENT_PENDING (18)
    cash_amount     NUMERIC(18, 2),
    units           NUMERIC(18, 6),
    nav_used        NUMERIC(18, 6),
    trade_date      DATE,
    settlement_date DATE,
    reject_reason   VARCHAR(400),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_fund_orders PRIMARY KEY (order_id),
    CONSTRAINT uq_fund_orders_order_ref UNIQUE (order_ref),
    CONSTRAINT fk_order_account FOREIGN KEY (account_id) REFERENCES accounts (account_id),
    CONSTRAINT fk_order_fund    FOREIGN KEY (fund_id)    REFERENCES funds (fund_id)
);

-- --- audit_events ----------------------------------------------------------
-- Append-only trail. Deliberately NOT foreign-keyed to fund_orders: the trail
-- must survive and stay immutable even if order rows are archived. order_id /
-- order_ref are stored by value.
CREATE TABLE audit_events (
    audit_id    BIGINT       NOT NULL,
    order_id    BIGINT       NOT NULL,
    order_ref   VARCHAR(24)  NOT NULL,
    from_status VARCHAR(20),
    to_status   VARCHAR(20)  NOT NULL,  -- longest value: SETTLEMENT_PENDING (18)
    detail      VARCHAR(400),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_audit_events PRIMARY KEY (audit_id)
);

-- --- settlement_instructions ----------------------------------------------
-- Forward-looking table for the scheduled settlement engine. One settlement
-- instruction is raised per order that reaches SETTLEMENT_PENDING.
CREATE TABLE settlement_instructions (
    settlement_instruction_id BIGINT         NOT NULL,
    order_id                  BIGINT         NOT NULL,
    instruction_ref           VARCHAR(24)    NOT NULL,
    status                    VARCHAR(20)    NOT NULL,
    settlement_date           DATE           NOT NULL,
    amount                    NUMERIC(18, 2),
    currency                  VARCHAR(3),
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_settlement_instructions PRIMARY KEY (settlement_instruction_id),
    CONSTRAINT uq_settlement_instruction_ref UNIQUE (instruction_ref),
    CONSTRAINT fk_settlement_order FOREIGN KEY (order_id) REFERENCES fund_orders (order_id)
);
