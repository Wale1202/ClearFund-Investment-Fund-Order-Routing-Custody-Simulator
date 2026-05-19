import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AuditEvent,
  CashBalance,
  CreateOrderRequest,
  HealthSummary,
  Holding,
  Order,
  OrderStatus,
  OrderType,
  Paged,
} from '../models/clearfund.models';

/** Single point of contact with the ClearFund REST API. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  // --- Orders ---

  listOrders(opts: {
    status?: OrderStatus;
    type?: OrderType;
    page?: number;
    size?: number;
  } = {}): Observable<Paged<Order>> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 20))
      .set('sort', 'id,desc');
    if (opts.status) params = params.set('status', opts.status);
    if (opts.type) params = params.set('type', opts.type);
    return this.http.get<Paged<Order>>(`${this.base}/orders`, { params });
  }

  getOrder(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.base}/orders/${id}`);
  }

  createOrder(body: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(`${this.base}/orders`, body);
  }

  validate(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.base}/orders/${id}/validate`, {});
  }

  route(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.base}/orders/${id}/route`, {});
  }

  accept(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.base}/orders/${id}/accept`, {});
  }

  settle(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.base}/orders/${id}/settle`, {});
  }

  cancel(id: number, reason: string): Observable<Order> {
    return this.http.post<Order>(`${this.base}/orders/${id}/cancel`, { reason });
  }

  auditEvents(id: number): Observable<AuditEvent[]> {
    return this.http.get<AuditEvent[]>(`${this.base}/orders/${id}/audit-events`);
  }

  // --- Accounts ---

  holdings(accountId: number): Observable<Holding[]> {
    return this.http.get<Holding[]>(`${this.base}/accounts/${accountId}/holdings`);
  }

  cashBalances(accountId: number): Observable<CashBalance[]> {
    return this.http.get<CashBalance[]>(`${this.base}/accounts/${accountId}/cash-balances`);
  }

  // --- System ---

  healthSummary(): Observable<HealthSummary> {
    return this.http.get<HealthSummary>(`${this.base}/system/health-summary`);
  }
}
