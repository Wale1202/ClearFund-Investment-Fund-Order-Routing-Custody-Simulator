// Mirrors the backend DTOs (see com.clearfund.dto).

export type OrderType = 'SUBSCRIPTION' | 'REDEMPTION';

export type OrderStatus =
  | 'RECEIVED'
  | 'VALIDATED'
  | 'ROUTED'
  | 'ACCEPTED'
  | 'SETTLEMENT_PENDING'
  | 'SETTLED'
  | 'REJECTED'
  | 'CANCELLED';

export interface CreateOrderRequest {
  accountRef: string;
  fundCode: string;
  orderType: OrderType;
  cashAmount?: number | null;
  units?: number | null;
}

export interface Order {
  // Optional: the backend OrderResponse currently exposes orderRef, not the
  // numeric id. Kept optional so lifecycle actions/links work once the
  // backend adds it (see frontend README note).
  id?: number;
  orderRef: string;
  accountRef: string;
  fundCode: string;
  orderType: OrderType;
  status: OrderStatus;
  cashAmount: number | null;
  units: number | null;
  navUsed: number | null;
  tradeDate: string | null;
  settlementDate: string | null;
  rejectReason: string | null;
  createdAt: string;
}

export interface Paged<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AuditEvent {
  fromStatus: string | null;
  toStatus: string;
  detail: string | null;
  createdAt: string;
}

export interface Holding {
  accountRef: string;
  fundCode: string;
  fundName: string;
  units: number;
}

export interface CashBalance {
  accountRef: string;
  currency: string;
  amount: number;
}

export interface HealthSummary {
  status: string;
  generatedAt: string;
  totalAccounts: number;
  totalFunds: number;
  totalOrders: number;
  ordersByStatus: Record<string, number>;
}
