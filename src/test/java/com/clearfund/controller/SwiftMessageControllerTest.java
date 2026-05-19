package com.clearfund.controller;

import com.clearfund.dto.OrderResponse;
import com.clearfund.dto.ParsedSwiftMessage;
import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;
import com.clearfund.exception.SwiftParseException;
import com.clearfund.service.OrderService;
import com.clearfund.service.SwiftMessageParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SwiftMessageController.class)
class SwiftMessageControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SwiftMessageParser swiftMessageParser;
    @MockBean private OrderService orderService;

    private static final String MESSAGE = """
            :20C::SEME//ORD123456
            :35B:ISIN IE00B4L5Y983
            :22H::BUSE//SUBS
            :19A::ORDR//EUR10000
            :97A::SAFE//ACC99821
            """;

    @Test
    void parse_validMessage_returns200() throws Exception {
        when(swiftMessageParser.parse(any())).thenReturn(new ParsedSwiftMessage(
                "ORD123456", "IE00B4L5Y983", "SUBS", OrderType.SUBSCRIPTION,
                "EUR", new BigDecimal("10000"), "ACC99821"));

        mockMvc.perform(post("/api/messages/parse")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(MESSAGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isin").value("IE00B4L5Y983"))
                .andExpect(jsonPath("$.orderType").value("SUBSCRIPTION"));
    }

    @Test
    void parse_invalidMessage_returns422() throws Exception {
        when(swiftMessageParser.parse(any()))
                .thenThrow(new SwiftParseException("Missing ISIN (:35B:ISIN)"));

        mockMvc.perform(post("/api/messages/parse")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("garbage"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("SWIFT_PARSE_ERROR"))
                .andExpect(jsonPath("$.messages[0]").value("Missing ISIN (:35B:ISIN)"));
    }

    @Test
    void createOrder_returns201() throws Exception {
        when(swiftMessageParser.parse(any())).thenReturn(new ParsedSwiftMessage(
                "ORD123456", "IE00B4L5Y983", "SUBS", OrderType.SUBSCRIPTION,
                "EUR", new BigDecimal("10000"), "ACC99821"));
        when(orderService.placeOrder(any())).thenReturn(new OrderResponse(
                "ORD-20260519-ABCD1234", "ACC99821", "IE00B4L5Y983", OrderType.SUBSCRIPTION,
                OrderStatus.RECEIVED, new BigDecimal("10000"), null, null,
                LocalDate.of(2026, 5, 19), LocalDate.of(2026, 5, 21), null,
                Instant.parse("2026-05-19T10:15:30Z")));

        mockMvc.perform(post("/api/messages/create-order")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(MESSAGE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }
}
