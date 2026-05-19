import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Order } from '../../models/clearfund.models';

@Component({
  selector: 'cf-settlement-queue',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h1>Settlement Queue</h1>
    <p class="muted">Orders awaiting settlement (SETTLEMENT_PENDING).</p>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>Order Ref</th><th>Account</th><th>Fund</th><th>Type</th>
            <th>Cash</th><th>Units</th><th>Settlement date</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let o of orders">
            <td>
              <a class="link" *ngIf="o.id != null; else noId" [routerLink]="['/orders', o.id]">
                {{ o.orderRef }}
              </a>
              <ng-template #noId>{{ o.orderRef }}</ng-template>
            </td>
            <td>{{ o.accountRef }}</td>
            <td>{{ o.fundCode }}</td>
            <td>{{ o.orderType }}</td>
            <td>{{ o.cashAmount ?? '-' }}</td>
            <td>{{ o.units ?? '-' }}</td>
            <td>{{ o.settlementDate ?? '-' }}</td>
            <td>
              <button
                class="primary"
                [disabled]="o.id == null"
                (click)="o.id != null && settle(o.id)"
              >
                Settle
              </button>
            </td>
          </tr>
          <tr *ngIf="orders.length === 0">
            <td colspan="8" class="muted">Queue is empty.</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p class="error" *ngIf="error">{{ error }}</p>
  `,
})
export class SettlementQueueComponent implements OnInit {
  private readonly api = inject(ApiService);
  orders: Order[] = [];
  error = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.listOrders({ status: 'SETTLEMENT_PENDING', size: 100 }).subscribe({
      next: (p) => (this.orders = p.content),
      error: () => (this.error = 'Could not load the settlement queue.'),
    });
  }

  settle(id: number): void {
    this.error = '';
    this.api.settle(id).subscribe({
      next: () => this.load(),
      error: (e) => (this.error = e?.error?.messages?.join(', ') || 'Settlement failed.'),
    });
  }
}
