package com.ecommerce.orderservice.web.dto;

import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.entity.OrderEntity;
import java.time.Instant;
import java.util.List;

/** View of an order and its lines. */
public record OrderResponse(
    Long id,
    String userId,
    OrderStatus status,
    long totalCents,
    Long paymentId,
    Instant createdAt,
    List<Line> lines) {

  /** View of a single order line. */
  public record Line(Long productId, int quantity, long unitPriceCents, long lineTotalCents) {}

  public static OrderResponse from(OrderEntity order) {
    List<Line> lines =
        order.getLines().stream()
            .map(
                l ->
                    new Line(
                        l.getProductId(),
                        l.getQuantity(),
                        l.getUnitPriceCents(),
                        l.lineTotalCents()))
            .toList();
    return new OrderResponse(
        order.getId(),
        order.getUserId(),
        order.getStatus(),
        order.getTotalCents(),
        order.getPaymentId(),
        order.getCreatedAt(),
        lines);
  }
}
