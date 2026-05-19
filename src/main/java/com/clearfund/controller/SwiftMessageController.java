package com.clearfund.controller;

import com.clearfund.dto.CreateOrderRequest;
import com.clearfund.dto.OrderResponse;
import com.clearfund.dto.ParsedSwiftMessage;
import com.clearfund.enums.OrderType;
import com.clearfund.service.OrderService;
import com.clearfund.service.SwiftMessageParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock SWIFT-style message intake. <strong>Educational only</strong> — this
 * is not real SWIFT integration.
 *
 * <pre>
 * POST /api/messages/parse           (Content-Type: text/plain)
 * :20C::SEME//ORD123456
 * :35B:ISIN IE00B4L5Y983
 * :22H::BUSE//SUBS
 * :19A::ORDR//EUR10000
 * :97A::SAFE//ACC99821
 *
 * 200 OK
 * { "reference": "ORD123456", "isin": "IE00B4L5Y983",
 *   "rawBusinessFunction": "SUBS", "orderType": "SUBSCRIPTION",
 *   "currency": "EUR", "amount": 10000, "safeAccount": "ACC99821" }
 *
 * Missing/invalid field -> 422
 * { "status": 422, "error": "SWIFT_PARSE_ERROR",
 *   "messages": ["Missing ISIN (:35B:ISIN)"] }
 * </pre>
 */
@RestController
@RequestMapping("/api/messages")
public class SwiftMessageController {

    private final SwiftMessageParser swiftMessageParser;
    private final OrderService orderService;

    public SwiftMessageController(SwiftMessageParser swiftMessageParser,
                                  OrderService orderService) {
        this.swiftMessageParser = swiftMessageParser;
        this.orderService = orderService;
    }

    /** Parse only — useful for previewing what an order would look like. */
    @PostMapping(value = "/parse", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ParsedSwiftMessage parse(@RequestBody String rawMessage) {
        return swiftMessageParser.parse(rawMessage);
    }

    /**
     * Parse the message and create the corresponding order.
     *
     * <p>Simplification (state this in interviews): we use the ISIN directly
     * as the fund code and the SAFE account directly as the account ref. In a
     * real system you would resolve ISIN → internal fund and validate the
     * safekeeping account against custody records. For a REDEMPTION the ORDR
     * value is treated as units; for a SUBSCRIPTION as cash.</p>
     */
    @PostMapping(value = "/create-order", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<OrderResponse> createOrder(@RequestBody String rawMessage) {
        ParsedSwiftMessage message = swiftMessageParser.parse(rawMessage);

        boolean subscription = message.orderType() == OrderType.SUBSCRIPTION;
        CreateOrderRequest request = new CreateOrderRequest(
                message.safeAccount(),
                message.isin(),
                message.orderType(),
                subscription ? message.amount() : null,
                subscription ? null : message.amount());

        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
