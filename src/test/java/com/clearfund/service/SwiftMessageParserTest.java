package com.clearfund.service;

import com.clearfund.dto.ParsedSwiftMessage;
import com.clearfund.enums.OrderType;
import com.clearfund.exception.SwiftParseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SwiftMessageParserTest {

    private final SwiftMessageParser parser = new SwiftMessageParser();

    private static final String VALID_SUBS = """
            :20C::SEME//ORD123456
            :35B:ISIN IE00B4L5Y983
            :22H::BUSE//SUBS
            :19A::ORDR//EUR10000
            :97A::SAFE//ACC99821
            """;

    @Test
    void parsesValidSubscriptionMessage() {
        ParsedSwiftMessage msg = parser.parse(VALID_SUBS);

        assertThat(msg.reference()).isEqualTo("ORD123456");
        assertThat(msg.isin()).isEqualTo("IE00B4L5Y983");
        assertThat(msg.rawBusinessFunction()).isEqualTo("SUBS");
        assertThat(msg.orderType()).isEqualTo(OrderType.SUBSCRIPTION);
        assertThat(msg.currency()).isEqualTo("EUR");
        assertThat(msg.amount()).isEqualByComparingTo("10000");
        assertThat(msg.safeAccount()).isEqualTo("ACC99821");
    }

    @Test
    void mapsRedmToRedemptionAndToleratesBlankLinesAndWhitespace() {
        String message = """

                  :20C::SEME//ORD999
                :35B:ISIN US0378331005
                :22H::BUSE//REDM
                :19A::ORDR//USD2500.75
                :97A::SAFE//ACC12345

                """;

        ParsedSwiftMessage msg = parser.parse(message);

        assertThat(msg.orderType()).isEqualTo(OrderType.REDEMPTION);
        assertThat(msg.currency()).isEqualTo("USD");
        assertThat(msg.amount()).isEqualByComparingTo("2500.75");
    }

    @Test
    void rejectsMissingIsin() {
        String message = VALID_SUBS.replace(":35B:ISIN IE00B4L5Y983\n", "");

        assertThatThrownBy(() -> parser.parse(message))
                .isInstanceOf(SwiftParseException.class)
                .hasMessageContaining("ISIN");
    }

    @Test
    void rejectsMissingAmount() {
        String message = VALID_SUBS.replace(":19A::ORDR//EUR10000\n", "");

        assertThatThrownBy(() -> parser.parse(message))
                .isInstanceOf(SwiftParseException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void rejectsMissingSafeAccount() {
        String message = VALID_SUBS.replace(":97A::SAFE//ACC99821\n", "");

        assertThatThrownBy(() -> parser.parse(message))
                .isInstanceOf(SwiftParseException.class)
                .hasMessageContaining("safekeeping account");
    }

    @Test
    void rejectsMissingBusinessFunction() {
        String message = VALID_SUBS.replace(":22H::BUSE//SUBS\n", "");

        assertThatThrownBy(() -> parser.parse(message))
                .isInstanceOf(SwiftParseException.class)
                .hasMessageContaining("business function");
    }

    @Test
    void rejectsUnsupportedBusinessFunction() {
        String message = VALID_SUBS.replace("SUBS", "SWAP");

        assertThatThrownBy(() -> parser.parse(message))
                .isInstanceOf(SwiftParseException.class)
                .hasMessageContaining("Unsupported business function");
    }

    @Test
    void rejectsMalformedAmount() {
        String message = VALID_SUBS.replace(":19A::ORDR//EUR10000", ":19A::ORDR//10000");

        assertThatThrownBy(() -> parser.parse(message))
                .isInstanceOf(SwiftParseException.class)
                .hasMessageContaining("Malformed amount");
    }

    @Test
    void rejectsEmptyMessage() {
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(SwiftParseException.class)
                .hasMessageContaining("empty");
    }
}
