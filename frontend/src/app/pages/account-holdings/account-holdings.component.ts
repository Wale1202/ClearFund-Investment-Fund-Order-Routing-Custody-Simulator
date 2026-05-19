import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Holding } from '../../models/clearfund.models';

@Component({
  selector: 'cf-account-holdings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h1>Account Holdings</h1>

    <div class="toolbar">
      <input type="number" [(ngModel)]="accountId" placeholder="Account id (e.g. 1)" />
      <button class="primary" (click)="load()">Load</button>
    </div>

    <div class="card">
      <table>
        <thead><tr><th>Account</th><th>Fund</th><th>Fund name</th><th>Units</th></tr></thead>
        <tbody>
          <tr *ngFor="let h of holdings">
            <td>{{ h.accountRef }}</td>
            <td>{{ h.fundCode }}</td>
            <td>{{ h.fundName }}</td>
            <td>{{ h.units }}</td>
          </tr>
          <tr *ngIf="loaded && holdings.length === 0">
            <td colspan="4" class="muted">No holdings for this account.</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p class="error" *ngIf="error">{{ error }}</p>
  `,
})
export class AccountHoldingsComponent {
  private readonly api = inject(ApiService);
  accountId: number | null = 1;
  holdings: Holding[] = [];
  loaded = false;
  error = '';

  load(): void {
    this.error = '';
    this.loaded = false;
    if (this.accountId == null) return;
    this.api.holdings(this.accountId).subscribe({
      next: (h) => {
        this.holdings = h;
        this.loaded = true;
      },
      error: (e) =>
        (this.error = e?.status === 404 ? 'Account not found.' : 'Could not load holdings.'),
    });
  }
}
