import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Order } from '../../models/clearfund.models';

@Component({
  selector: 'cf-failed-orders',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h1>Failed Orders</h1>
    <p class="muted">Orders that were rejected by validation (with the stored reason).</p>

    <div class="card">
      <table>
        <thead>
          <tr><th>Order Ref</th><th>Account</th><th>Fund</th><th>Type</th><th>Reason</th></tr>
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
            <td>{{ o.rejectReason ?? '-' }}</td>
          </tr>
          <tr *ngIf="orders.length === 0">
            <td colspan="5" class="muted">No failed orders.</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p class="error" *ngIf="error">{{ error }}</p>
  `,
})
export class FailedOrdersComponent implements OnInit {
  private readonly api = inject(ApiService);
  orders: Order[] = [];
  error = '';

  ngOnInit(): void {
    this.api.listOrders({ status: 'REJECTED', size: 100 }).subscribe({
      next: (p) => (this.orders = p.content),
      error: () => (this.error = 'Could not load failed orders.'),
    });
  }
}
