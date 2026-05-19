import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { CashBalance } from '../../models/clearfund.models';

@Component({
  selector: 'cf-cash-balances',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h1>Cash Balances</h1>

    <div class="toolbar">
      <input type="number" [(ngModel)]="accountId" placeholder="Account id (e.g. 1)" />
      <button class="primary" (click)="load()">Load</button>
    </div>

    <div class="card">
      <table>
        <thead><tr><th>Account</th><th>Currency</th><th>Amount</th></tr></thead>
        <tbody>
          <tr *ngFor="let c of balances">
            <td>{{ c.accountRef }}</td>
            <td>{{ c.currency }}</td>
            <td>{{ c.amount }}</td>
          </tr>
          <tr *ngIf="loaded && balances.length === 0">
            <td colspan="3" class="muted">No cash balances for this account.</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p class="error" *ngIf="error">{{ error }}</p>
  `,
})
export class CashBalancesComponent {
  private readonly api = inject(ApiService);
  accountId: number | null = 1;
  balances: CashBalance[] = [];
  loaded = false;
  error = '';

  load(): void {
    this.error = '';
    this.loaded = false;
    if (this.accountId == null) return;
    this.api.cashBalances(this.accountId).subscribe({
      next: (b) => {
        this.balances = b;
        this.loaded = true;
      },
      error: (e) =>
        (this.error = e?.status === 404 ? 'Account not found.' : 'Could not load balances.'),
    });
  }
}
