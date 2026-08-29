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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Order placement and lookup endpoints. */
@RestController
@RequestMapping("/orders")
public class OrderController {

  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
    OrderResponse body = service.place(request);
    return ResponseEntity.created(URI.create("/orders/" + body.id())).body(body);
  }

  @GetMapping("/{id}")
  public OrderResponse getById(@PathVariable Long id) {
    return service.getById(id);
  }

  @GetMapping
  public List<OrderResponse> byUser(@RequestParam String userId) {
    return service.findByUser(userId);
  }
}
