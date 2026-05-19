import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'orders' },
  {
    path: 'orders',
    loadComponent: () =>
      import('./pages/order-book/order-book.component').then((m) => m.OrderBookComponent),
  },
  {
    path: 'orders/new',
    loadComponent: () =>
      import('./pages/create-order/create-order.component').then((m) => m.CreateOrderComponent),
  },
  {
    path: 'orders/:id',
    loadComponent: () =>
      import('./pages/order-details/order-details.component').then((m) => m.OrderDetailsComponent),
  },
  {
    path: 'failed',
    loadComponent: () =>
      import('./pages/failed-orders/failed-orders.component').then((m) => m.FailedOrdersComponent),
  },
  {
    path: 'holdings',
    loadComponent: () =>
      import('./pages/account-holdings/account-holdings.component').then(
        (m) => m.AccountHoldingsComponent,
      ),
  },
  {
    path: 'cash',
    loadComponent: () =>
      import('./pages/cash-balances/cash-balances.component').then((m) => m.CashBalancesComponent),
  },
  {
    path: 'settlement',
    loadComponent: () =>
      import('./pages/settlement-queue/settlement-queue.component').then(
        (m) => m.SettlementQueueComponent,
      ),
  },
  {
    path: 'health',
    loadComponent: () =>
      import('./pages/system-health/system-health.component').then((m) => m.SystemHealthComponent),
  },
  { path: '**', redirectTo: 'orders' },
];
