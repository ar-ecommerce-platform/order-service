package com.ecommerce.orderservice.web;

import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.web.dto.OrderResponse;
import com.ecommerce.orderservice.web.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order placement and lookup, always scoped to the caller. {@value #USER_ID} is set by the gateway
 * from the verified token (any client-sent value is stripped there).
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

  static final String USER_ID = "X-User-Id";

  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<OrderResponse> place(
      @RequestHeader(USER_ID) String userId, @Valid @RequestBody PlaceOrderRequest request) {
    OrderResponse body = service.place(userId, request);
    return ResponseEntity.created(URI.create("/orders/" + body.id())).body(body);
  }

  @GetMapping("/{id}")
  public OrderResponse getById(@PathVariable Long id, @RequestHeader(USER_ID) String userId) {
    return service.getById(id, userId);
  }

  @GetMapping
  public List<OrderResponse> mine(@RequestHeader(USER_ID) String userId) {
    return service.findByUser(userId);
  }
}
