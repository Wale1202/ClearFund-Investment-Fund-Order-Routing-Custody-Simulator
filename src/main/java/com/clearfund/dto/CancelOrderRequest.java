package com.clearfund.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for cancelling an order; a reason is mandatory for the audit trail. */
public record CancelOrderRequest(
        @NotBlank(message = "reason is required") String reason
) {
}
