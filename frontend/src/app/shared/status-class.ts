import { OrderStatus } from '../models/clearfund.models';

/** Maps an order status to a badge CSS modifier (see styles.css). */
export function statusClass(status: OrderStatus | string): string {
  switch (status) {
    case 'SETTLED':
      return 'settled';
    case 'SETTLEMENT_PENDING':
      return 'pending';
    case 'REJECTED':
    case 'CANCELLED':
      return 'failed';
    default:
      return 'active';
  }
}
