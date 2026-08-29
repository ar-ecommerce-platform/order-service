package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.entity.OrderEntity;
import com.ecommerce.orderservice.entity.OrderLine;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.web.dto.OrderResponse;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The short database transactions that make up an order. Kept separate from {@link OrderService} so
 * that each write commits independently: the orchestration spans external HTTP calls and must not
 * hold a single transaction open across them, and a failed order (REJECTED_STOCK / PAYMENT_FAILED)
 * must survive as an audit record even though the placement call ends in an exception.
 */
@Component
public class OrderTransactions {

  private final OrderRepository repository;

  public OrderTransactions(OrderRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public OrderEntity createPending(String userId, List<OrderLine> lines) {
    return repository.save(new OrderEntity(userId, lines, OrderStatus.PENDING));
  }

  @Transactional
  public void markRejectedStock(Long orderId) {
    load(orderId).markRejectedStock();
  }

  @Transactional
  public void markPaymentFailed(Long orderId) {
    load(orderId).markPaymentFailed();
  }

  @Transactional
  public OrderResponse confirm(Long orderId, Long paymentId) {
    OrderEntity order = load(orderId);
    order.markPaid(paymentId);
    return OrderResponse.from(order);
  }

  @Transactional(readOnly = true)
  public OrderResponse getById(Long orderId) {
    return OrderResponse.from(load(orderId));
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> findByUser(String userId) {
    return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(OrderResponse::from)
        .toList();
  }

  private OrderEntity load(Long orderId) {
    return repository
        .findWithLinesById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
  }
}
