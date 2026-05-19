import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuditEvent, Order } from '../../models/clearfund.models';
import { statusClass } from '../../shared/status-class';

@Component({
  selector: 'cf-order-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h1>Order Details</h1>
    <a class="link" routerLink="/orders">&larr; Back to order book</a>

    <div class="card" *ngIf="order as o" style="margin-top:1rem">
      <h2 style="margin-top:0">
        {{ o.orderRef }}
        <span class="badge" [class]="'badge ' + cls(o.status)">{{ o.status }}</span>
      </h2>
      <p class="muted">
        {{ o.orderType }} · Account {{ o.accountRef }} · Fund {{ o.fundCode }}
      </p>
      <table>
        <tbody>
          <tr><th>Cash amount</th><td>{{ o.cashAmount ?? '-' }}</td></tr>
          <tr><th>Units</th><td>{{ o.units ?? '-' }}</td></tr>
          <tr><th>NAV used</th><td>{{ o.navUsed ?? '-' }}</td></tr>
          <tr><th>Trade date</th><td>{{ o.tradeDate ?? '-' }}</td></tr>
          <tr><th>Settlement date</th><td>{{ o.settlementDate ?? '-' }}</td></tr>
          <tr *ngIf="o.rejectReason"><th>Reason</th><td>{{ o.rejectReason }}</td></tr>
        </tbody>
      </table>

      <div class="actions" style="margin-top:1rem">
        <button (click)="act('validate')">Validate</button>
        <button (click)="act('route')">Route</button>
        <button (click)="act('accept')">Accept</button>
        <button class="primary" (click)="act('settle')">Settle</button>
        <button (click)="cancel()">Cancel</button>
      </div>
    </div>

    <div class="card" *ngIf="events.length">
      <h3 style="margin-top:0">Audit trail</h3>
      <table>
        <thead><tr><th>From</th><th>To</th><th>Detail</th><th>At</th></tr></thead>
        <tbody>
          <tr *ngFor="let e of events">
            <td>{{ e.fromStatus ?? '-' }}</td>
            <td>{{ e.toStatus }}</td>
            <td>{{ e.detail }}</td>
            <td class="muted">{{ e.createdAt }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <p class="error" *ngIf="error">{{ error }}</p>
  `,
})
export class OrderDetailsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  id!: number;
  order?: Order;
  events: AuditEvent[] = [];
  error = '';
  cls = statusClass;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.reload();
  }

  reload(): void {
    this.api.getOrder(this.id).subscribe({
      next: (o) => (this.order = o),
      error: () => (this.error = 'Order not found.'),
    });
    this.api.auditEvents(this.id).subscribe({
      next: (e) => (this.events = e),
      error: () => {},
    });
  }

  act(step: 'validate' | 'route' | 'accept' | 'settle'): void {
    this.error = '';
    this.api[step](this.id).subscribe({
      next: () => this.reload(),
      error: (e) => (this.error = e?.error?.messages?.join(', ') || 'Action failed.'),
    });
  }

  cancel(): void {
    const reason = window.prompt('Cancellation reason?');
    if (!reason) return;
    this.api.cancel(this.id, reason).subscribe({
      next: () => this.reload(),
      error: (e) => (this.error = e?.error?.messages?.join(', ') || 'Cancel failed.'),
    });
  }
}
