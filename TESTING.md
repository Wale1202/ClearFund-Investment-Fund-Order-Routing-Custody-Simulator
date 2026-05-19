# Testing ClearFund

48 tests across four layers. The strategy favours fast, isolated unit tests
for business logic, thin web-slice tests for the HTTP contract, and a single
real-database integration test that exercises the whole flow end to end.

## What is tested

| Area | Test | Style |
|---|---|---|
| Order creation | `OrderServiceImplTest.placeOrder_*` | JUnit 5 + Mockito |
| Order validation | `OrderServiceImplTest.validateOrder_*` | Mockito |
| Invalid order rejection | `validateOrder_*Rejected*`, `OrderControllerTest` 422 case | Mockito / MockMvc |
| Status transition rules | `OrderStatusTest` (incl. parameterized terminal/in-flight) | Pure unit |
| Audit event creation | `AuditServiceTest` (ArgumentCaptor on the saved entity) | Mockito |
| Settlement logic | `SettlementServiceImplTest` (subscription, redemption, both failure modes, wrong-state guard) | Mockito |
| Mock SWIFT parsing | `SwiftMessageParserTest` (valid + every failure mode) | Pure unit |
| REST controllers | `OrderControllerTest`, `SwiftMessageControllerTest` | `@WebMvcTest` + MockMvc |
| Full lifecycle on a real DB | `OrderLifecycleIntegrationTest` | `@SpringBootTest` + Testcontainers (PostgreSQL) |

The integration test also implicitly verifies the **Flyway migrations** and
that Hibernate `validate` accepts the entity mappings — it runs V1–V3 against
a real PostgreSQL before the assertions.

## How to run the tests

```bash
# Everything (unit + web + integration)
mvn test

# Just the fast tests (skip the Docker-backed integration test)
mvn test -Dtest='!OrderLifecycleIntegrationTest'

# A single class
mvn test -Dtest=SettlementServiceImplTest
```

The integration test is annotated `@Testcontainers(disabledWithoutDocker = true)`,
so on a machine or CI agent **without Docker it is skipped, not failed** — the
core build stays green everywhere, and the deep test runs wherever Docker is
available.

## Why testing matters in financial systems

In an order-routing and custody system the tests are not just a quality gate,
they protect money and trust:

- **Correctness of money movement.** A wrong rounding mode or a flipped
  sign in subscription vs. redemption directly mis-states client holdings and
  cash. These paths are asserted explicitly.
- **State-machine integrity.** Settling an order twice, or skipping
  validation, could double-spend cash. The transition rules are unit-tested
  in isolation so every service can trust them.
- **Atomicity.** Settlement touches cash, holdings, the instruction and the
  order. The failure tests prove that an insufficient-funds settlement
  changes *nothing* — partial settlement is the worst outcome in custody.
- **Auditability.** Regulators and operations need a complete trail. The
  audit test pins the exact persisted fields so the trail can't silently
  regress.
- **Schema drift is caught early.** The Testcontainers test found a real
  bug during development — `SETTLEMENT_PENDING` (18 chars) overflowing a
  `VARCHAR(16)` status column — that unit tests with mocks could never see.

## What I would add next in a production environment

This is a portfolio project; in production I would extend the strategy with:

1. **Concurrency / optimistic-locking tests** — two settlements racing on the
   same `CashBalance`/`Holding`, asserting the `@Version` conflict is handled.
2. **Repository slice tests** (`@DataJpaTest`) for the custom `@Query`
   methods (paged `search`, `countGroupedByStatus`) against PostgreSQL.
3. **A Flyway migration test** that asserts each new migration applies on top
   of the previous schema *and* that `validate` still passes (catch breaking
   DDL before deploy).
4. **Contract tests** for the REST API (e.g. Spring Cloud Contract) so
   downstream consumers break the build, not production.
5. **Idempotency / retry tests** for settlement once it becomes an async
   scheduled engine (re-processing a `SETTLEMENT_PENDING` order must be safe).
6. **Property-based tests** on the pricing math (units = cash / NAV and the
   inverse) to fuzz rounding across many NAV/amount combinations.
7. **Mutation testing** (PIT) to measure assertion quality, not just line
   coverage, plus a coverage gate (JaCoCo) in CI.
8. **Performance/load tests** on the order list and settlement batch with
   realistic data volumes, validating the V3 indexes actually help.
9. **Security tests** once authn/authz exists (an account must not act on
   another account's orders or holdings).
