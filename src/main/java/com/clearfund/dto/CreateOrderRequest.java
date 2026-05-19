package com.clearfund.dto;

import com.clearfund.enums.OrderType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

/**
 * Request body for placing an order.
 *
 * <p>Exactly one amount is required depending on the order type:</p>
 * <ul>
 *   <li>SUBSCRIPTION → {@code cashAmount} must be supplied (units derived).</li>
 *   <li>REDEMPTION   → {@code units} must be supplied (cash derived).</li>
 * </ul>
 * The cross-field rule is enforced by {@link #amountConsistentWithType()}.
 */
public record CreateOrderRequest(

        @NotBlank(message = "accountRef is required")
        String accountRef,

        @NotBlank(message = "fundCode is required")
        String fundCode,

        @NotNull(message = "orderType is required")
        OrderType orderType,

        @DecimalMin(value = "0.01", message = "cashAmount must be greater than 0")
        BigDecimal cashAmount,

        @DecimalMin(value = "0.000001", message = "units must be greater than 0")
        BigDecimal units
) {

    @JsonIgnore
    @AssertTrue(message = "SUBSCRIPTION requires cashAmount only; REDEMPTION requires units only")
    public boolean isAmountConsistentWithType() {
        if (orderType == null) {
            return true; // @NotNull already reports this
        }
        return switch (orderType) {
            case SUBSCRIPTION -> cashAmount != null && units == null;
            case REDEMPTION -> units != null && cashAmount == null;
        };
    }
}
