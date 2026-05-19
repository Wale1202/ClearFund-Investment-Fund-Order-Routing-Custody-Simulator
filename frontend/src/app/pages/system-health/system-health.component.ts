import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ApiService } from '../../services/api.service';
import { HealthSummary } from '../../models/clearfund.models';

@Component({
  selector: 'cf-system-health',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1>System Health Summary</h1>

    <div class="card" *ngIf="health as h">
      <p>
        Status:
        <span class="badge" [class]="'badge ' + (h.status === 'UP' ? 'settled' : 'failed')">
          {{ h.status }}
        </span>
        <span class="muted">· generated {{ h.generatedAt }}</span>
      </p>
      <table>
        <tbody>
          <tr><th>Total accounts</th><td>{{ h.totalAccounts }}</td></tr>
          <tr><th>Total funds</th><td>{{ h.totalFunds }}</td></tr>
          <tr><th>Total orders</th><td>{{ h.totalOrders }}</td></tr>
        </tbody>
      </table>

      <h3>Orders by status</h3>
      <table>
        <thead><tr><th>Status</th><th>Count</th></tr></thead>
        <tbody>
          <tr *ngFor="let kv of statusEntries(h)">
            <td>{{ kv[0] }}</td>
            <td>{{ kv[1] }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p class="error" *ngIf="error">{{ error }}</p>
  `,
})
export class SystemHealthComponent implements OnInit {
  private readonly api = inject(ApiService);
  health?: HealthSummary;
  error = '';

  ngOnInit(): void {
    this.api.healthSummary().subscribe({
      next: (h) => (this.health = h),
      error: () => (this.error = 'Could not load the health summary.'),
    });
  }

  statusEntries(h: HealthSummary): [string, number][] {
    return Object.entries(h.ordersByStatus);
  }
}
