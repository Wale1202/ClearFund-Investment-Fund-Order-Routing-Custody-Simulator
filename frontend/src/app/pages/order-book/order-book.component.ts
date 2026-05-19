import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Order, OrderStatus, OrderType, Paged } from '../../models/clearfund.models';
import { statusClass } from '../../shared/status-class';

@Component({
  selector: 'cf-order-book',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h1>Order Book</h1>

    <div class="toolbar">
      <select [value]="status ?? ''" (change)="onStatus($any($event.target).value)">
        <option value="">All statuses</option>
        <option *ngFor="let s of statuses" [value]="s">{{ s }}</option>
      </select>
      <select [value]="type ?? ''" (change)="onType($any($event.target).value)">
        <option value="">All types</option>
        <option value="SUBSCRIPTION">SUBSCRIPTION</option>
        <option value="REDEMPTION">REDEMPTION</option>
      </select>
      <a class="link" routerLink="/orders/new">+ Create order</a>
    </div>

    <div class="card" *ngIf="page as p">
      <table>
        <thead>
          <tr>
            <th>Order Ref</th><th>Account</th><th>Fund</th><th>Type</th>
            <th>Status</th><th>Cash</th><th>Units</th><th>Settlement</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let o of p.content">
            <td>
              <a class="link" *ngIf="o.id != null; else noId" [routerLink]="['/orders', o.id]">
                {{ o.orderRef }}
              </a>
              <ng-template #noId>{{ o.orderRef }}</ng-template>
            </td>
            <td>{{ o.accountRef }}</td>
            <td>{{ o.fundCode }}</td>
            <td>{{ o.orderType }}</td>
            <td><span class="badge" [class]="'badge ' + cls(o.status)">{{ o.status }}</span></td>
            <td>{{ o.cashAmount ?? '-' }}</td>
            <td>{{ o.units ?? '-' }}</td>
            <td>{{ o.settlementDate ?? '-' }}</td>
          </tr>
          <tr *ngIf="p.content.length === 0">
            <td colspan="8" class="muted">No orders.</td>
          </tr>
        </tbody>
      </table>

      <div class="toolbar" style="margin-top:1rem">
        <button (click)="prev()" [disabled]="p.first">Prev</button>
        <span class="muted">Page {{ p.page + 1 }} / {{ p.totalPages || 1 }}</span>
        <button (click)="next()" [disabled]="p.last">Next</button>
        <span class="muted">{{ p.totalElements }} total</span>
      </div>
    </div>

    <p class="error" *ngIf="error">{{ error }}</p>
  `,
})
export class OrderBookComponent implements OnInit {
  private readonly api = inject(ApiService);

  page?: Paged<Order>;
  error = '';
  pageIndex = 0;
  status?: OrderStatus;
  type?: OrderType;
  readonly statuses: OrderStatus[] = [
    'RECEIVED', 'VALIDATED', 'ROUTED', 'ACCEPTED',
    'SETTLEMENT_PENDING', 'SETTLED', 'REJECTED', 'CANCELLED',
  ];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api
      .listOrders({ status: this.status, type: this.type, page: this.pageIndex })
      .subscribe({
        next: (p) => (this.page = p),
        error: () => (this.error = 'Could not load orders. Is the backend running?'),
      });
  }

  cls = statusClass;

  onStatus(v: string): void {
    this.status = (v || undefined) as OrderStatus | undefined;
    this.pageIndex = 0;
    this.load();
  }

  onType(v: string): void {
    this.type = (v || undefined) as OrderType | undefined;
    this.pageIndex = 0;
    this.load();
  }

  next(): void {
    if (this.page && !this.page.last) {
      this.pageIndex++;
      this.load();
    }
  }

  prev(): void {
    if (this.pageIndex > 0) {
      this.pageIndex--;
      this.load();
    }
  }
}
