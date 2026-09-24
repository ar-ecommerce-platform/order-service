package com.ecommerce.orderservice.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** Request body to place an order. The buyer is the caller ({@code X-User-Id}), not a field. */
public record PlaceOrderRequest(@NotEmpty @Valid List<Item> items) {

  /** One requested product and quantity. */
  public record Item(@NotNull Long productId, @Positive int quantity) {}
}
