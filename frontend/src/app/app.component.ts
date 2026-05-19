import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'cf-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <span class="brand">ClearFund</span>
      <nav>
        <a routerLink="/orders" routerLinkActive="active">Order Book</a>
        <a routerLink="/orders/new" routerLinkActive="active">Create Order</a>
        <a routerLink="/settlement" routerLinkActive="active">Settlement Queue</a>
        <a routerLink="/failed" routerLinkActive="active">Failed Orders</a>
        <a routerLink="/holdings" routerLinkActive="active">Holdings</a>
        <a routerLink="/cash" routerLinkActive="active">Cash Balances</a>
        <a routerLink="/health" routerLinkActive="active">System Health</a>
      </nav>
    </header>
    <main class="content">
      <router-outlet />
    </main>
  `,
})
export class AppComponent {}
