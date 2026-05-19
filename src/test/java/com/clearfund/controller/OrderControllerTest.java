package com.clearfund.controller;

import com.clearfund.dto.OrderResponse;
import com.clearfund.dto.PagedResponse;
import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;
import com.clearfund.exception.ResourceNotFoundException;
import com.clearfund.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private OrderService orderService;

    private OrderResponse sampleOrder() {
        return new OrderResponse("ORD-20260519-ABCD1234", "ACC-1", "CFEQ01",
                OrderType.SUBSCRIPTION, OrderStatus.RECEIVED, new BigDecimal("5000.00"),
                null, null, LocalDate.of(2026, 5, 19), LocalDate.of(2026, 5, 21),
                null, Instant.parse("2026-05-19T10:15:30Z"));
    }

    @Test
    void placeOrder_validRequest_returns201() throws Exception {
        when(orderService.placeOrder(any())).thenReturn(sampleOrder());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "accountRef": "ACC-1", "fundCode": "CFEQ01",
                                  "orderType": "SUBSCRIPTION", "cashAmount": 5000.00 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderRef").value("ORD-20260519-ABCD1234"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void placeOrder_invalidRequest_returns422WithMessages() throws Exception {
        // Missing accountRef/fundCode and a SUBSCRIPTION with no cashAmount.
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "orderType": "SUBSCRIPTION" }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.messages").isNotEmpty());
    }

    @Test
    void getOrder_found_returns200() throws Exception {
        when(orderService.getOrder(42L)).thenReturn(sampleOrder());

        mockMvc.perform(get("/api/orders/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountRef").value("ACC-1"));
    }

    @Test
    void getOrder_unknown_returns404() throws Exception {
        when(orderService.getOrder(99L))
                .thenThrow(ResourceNotFoundException.of("Order", "99"));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void validate_returns200() throws Exception {
        when(orderService.validateOrder(anyLong())).thenReturn(sampleOrder());

        mockMvc.perform(post("/api/orders/42/validate"))
                .andExpect(status().isOk());
    }

    @Test
    void listOrders_returnsPagedEnvelope() throws Exception {
        when(orderService.listOrders(any(), any(), any()))
                .thenReturn(new PagedResponse<>(List.of(sampleOrder()), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/orders?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].orderRef").value("ORD-20260519-ABCD1234"));
    }
}
