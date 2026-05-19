import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { CreateOrderRequest, Order } from '../../models/clearfund.models';

@Component({
  selector: 'cf-create-order',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <h1>Create Order</h1>

    <div class="card">
      <form [formGroup]="form" (ngSubmit)="submit()">
        <div class="field">
          <label>Account Ref</label>
          <input formControlName="accountRef" placeholder="ACC-1002" />
          <div class="error" *ngIf="show('accountRef')">Account ref is required.</div>
        </div>

        <div class="field">
          <label>Fund Code</label>
          <input formControlName="fundCode" placeholder="LU0292096186" />
          <div class="error" *ngIf="show('fundCode')">Fund code is required.</div>
        </div>

        <div class="field">
          <label>Order Type</label>
          <select formControlName="orderType">
            <option value="SUBSCRIPTION">SUBSCRIPTION (cash → units)</option>
            <option value="REDEMPTION">REDEMPTION (units → cash)</option>
          </select>
        </div>

        <div class="field" *ngIf="isSubscription()">
          <label>Cash Amount</label>
          <input type="number" formControlName="cashAmount" placeholder="2000.00" />
          <div class="error" *ngIf="show('cashAmount')">Enter a cash amount greater than 0.</div>
        </div>

        <div class="field" *ngIf="!isSubscription()">
          <label>Units</label>
          <input type="number" formControlName="units" placeholder="50" />
          <div class="error" *ngIf="show('units')">Enter a units value greater than 0.</div>
        </div>

        <button class="primary" type="submit" [disabled]="submitting">Place order</button>
        <a class="link" routerLink="/orders" style="margin-left:1rem">Back to order book</a>
      </form>
    </div>

    <div class="card" *ngIf="created">
      <strong>Order created:</strong> {{ created.orderRef }}
      <span class="badge badge active">{{ created.status }}</span>
    </div>
    <p class="error" *ngIf="error">{{ error }}</p>
  `,
})
export class CreateOrderComponent {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  submitting = false;
  created?: Order;
  error = '';

  readonly form = this.fb.group({
    accountRef: ['', Validators.required],
    fundCode: ['', Validators.required],
    orderType: ['SUBSCRIPTION', Validators.required],
    cashAmount: [null as number | null],
    units: [null as number | null],
  });

  constructor() {
    this.applyTypeValidators('SUBSCRIPTION');
    this.form.controls.orderType.valueChanges.subscribe((t) =>
      this.applyTypeValidators(t ?? 'SUBSCRIPTION'),
    );
  }

  isSubscription(): boolean {
    return this.form.controls.orderType.value === 'SUBSCRIPTION';
  }

  /** Only the relevant amount field is required, mirroring the backend rule. */
  private applyTypeValidators(type: string): void {
    const cash = this.form.controls.cashAmount;
    const units = this.form.controls.units;
    if (type === 'SUBSCRIPTION') {
      cash.setValidators([Validators.required, Validators.min(0.01)]);
      units.clearValidators();
      units.setValue(null);
    } else {
      units.setValidators([Validators.required, Validators.min(0.000001)]);
      cash.clearValidators();
      cash.setValue(null);
    }
    cash.updateValueAndValidity();
    units.updateValueAndValidity();
  }

  show(name: string): boolean {
    const c = this.form.get(name);
    return !!c && c.invalid && (c.dirty || c.touched);
  }

  submit(): void {
    this.error = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body: CreateOrderRequest = {
      accountRef: v.accountRef!,
      fundCode: v.fundCode!,
      orderType: v.orderType as CreateOrderRequest['orderType'],
      cashAmount: this.isSubscription() ? Number(v.cashAmount) : null,
      units: this.isSubscription() ? null : Number(v.units),
    };
    this.submitting = true;
    this.api.createOrder(body).subscribe({
      next: (o) => {
        this.created = o;
        this.submitting = false;
        this.form.reset({ orderType: 'SUBSCRIPTION' });
      },
      error: (e) => {
        this.error = e?.error?.messages?.join(', ') || 'Order creation failed.';
        this.submitting = false;
      },
    });
  }
}
