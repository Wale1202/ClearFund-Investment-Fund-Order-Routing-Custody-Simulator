package com.clearfund.service;

import com.clearfund.dto.ParsedSwiftMessage;
import com.clearfund.enums.OrderType;
import com.clearfund.exception.SwiftParseException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a simplified, SWIFT-<em>style</em> securities message into a
 * {@link ParsedSwiftMessage}. This is a teaching parser: it recognises only
 * the five fields ClearFund cares about and is deliberately forgiving about
 * whitespace and unknown lines.
 *
 * <p>Expected lines (order independent):</p>
 * <pre>
 * :20C::SEME//ORD123456        sender reference
 * :35B:ISIN IE00B4L5Y983       fund ISIN
 * :22H::BUSE//SUBS             business function (SUBS | REDM)
 * :19A::ORDR//EUR10000         settlement amount (CCY + value)
 * :97A::SAFE//ACC99821         safekeeping account
 * </pre>
 */
@Service
public class SwiftMessageParser {

    private static final String TAG_REFERENCE = ":20C::SEME//";
    private static final String TAG_ISIN      = ":35B:ISIN ";
    private static final String TAG_FUNCTION  = ":22H::BUSE//";
    private static final String TAG_AMOUNT    = ":19A::ORDR//";
    private static final String TAG_ACCOUNT   = ":97A::SAFE//";

    /** e.g. "EUR10000" or "EUR10000.50" — 3-letter currency then a number. */
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("^([A-Z]{3})([0-9]+(?:\\.[0-9]+)?)$");

    public ParsedSwiftMessage parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new SwiftParseException("Message body is empty");
        }

        String reference = null;
        String isin = null;
        String businessFunction = null;
        String currency = null;
        BigDecimal amount = null;
        String safeAccount = null;

        for (String rawLine : rawMessage.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue; // tolerate blank lines
            }
            if (line.startsWith(TAG_REFERENCE)) {
                reference = after(line, TAG_REFERENCE);
            } else if (line.startsWith(TAG_ISIN)) {
                isin = after(line, TAG_ISIN);
            } else if (line.startsWith(TAG_FUNCTION)) {
                businessFunction = after(line, TAG_FUNCTION);
            } else if (line.startsWith(TAG_ACCOUNT)) {
                safeAccount = after(line, TAG_ACCOUNT);
            } else if (line.startsWith(TAG_AMOUNT)) {
                String value = after(line, TAG_AMOUNT);
                Matcher matcher = AMOUNT_PATTERN.matcher(value);
                if (!matcher.matches()) {
                    throw new SwiftParseException(
                            "Malformed amount in " + TAG_AMOUNT + " (expected <CCY><value>): " + value);
                }
                currency = matcher.group(1);
                amount = new BigDecimal(matcher.group(2));
            }
            // unknown lines are intentionally ignored (forgiving parser)
        }

        OrderType orderType = mapOrderType(businessFunction);
        validateRequired(isin, amount, safeAccount);

        return new ParsedSwiftMessage(
                reference, isin, businessFunction, orderType, currency, amount, safeAccount);
    }

    /** SUBS -> SUBSCRIPTION, REDM -> REDEMPTION. */
    private OrderType mapOrderType(String businessFunction) {
        if (businessFunction == null) {
            throw new SwiftParseException("Missing business function (" + TAG_FUNCTION + ")");
        }
        return switch (businessFunction) {
            case "SUBS" -> OrderType.SUBSCRIPTION;
            case "REDM" -> OrderType.REDEMPTION;
            default -> throw new SwiftParseException(
                    "Unsupported business function '%s' (expected SUBS or REDM)"
                            .formatted(businessFunction));
        };
    }

    private void validateRequired(String isin, BigDecimal amount, String safeAccount) {
        if (isBlank(isin)) {
            throw new SwiftParseException("Missing ISIN (" + TAG_ISIN.trim() + ")");
        }
        if (amount == null) {
            throw new SwiftParseException("Missing order amount (" + TAG_AMOUNT + ")");
        }
        if (amount.signum() <= 0) {
            throw new SwiftParseException("Order amount must be greater than zero");
        }
        if (isBlank(safeAccount)) {
            throw new SwiftParseException("Missing safekeeping account (" + TAG_ACCOUNT + ")");
        }
    }

    private String after(String line, String prefix) {
        return line.substring(prefix.length()).trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
