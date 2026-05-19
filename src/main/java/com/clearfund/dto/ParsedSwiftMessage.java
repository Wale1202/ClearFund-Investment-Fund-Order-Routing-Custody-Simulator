package com.clearfund.dto;

import com.clearfund.enums.OrderType;

import java.math.BigDecimal;

/**
 * The result of parsing a simplified SWIFT-style message.
 *
 * <p>This is an educational subset, not real ISO 15022/20022 — only the
 * handful of fields ClearFund needs are recognised.</p>
 */
public record ParsedSwiftMessage(
        String reference,           // :20C::SEME//   sender's reference
        String isin,                // :35B:ISIN       fund identifier
        String rawBusinessFunction, // :22H::BUSE//    e.g. SUBS / REDM
        OrderType orderType,        // mapped from the business function
        String currency,            // from :19A::ORDR//<CCY><amount>
        BigDecimal amount,          // from :19A::ORDR//
        String safeAccount          // :97A::SAFE//    account code
) {
}
