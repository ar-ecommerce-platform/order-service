package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.client.NotificationClient;
import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.PaymentClient.PaymentResult;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.entity.OrderEntity;
import com.ecommerce.orderservice.entity.OrderLine;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.web.dto.OrderResponse;
import com.ecommerce.orderservice.web.dto.PlaceOrderRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places orders by orchestrating the product, inventory, payment and notification services.
 *
 * <p>This is a deliberately simple synchronous orchestration. It does <em>not</em> compensate a
 * partial stock reservation if a later line fails, retry, or wrap calls in a circuit breaker - each
 * of those is a planned follow-up (see infra/RUNBOOK.md).
 */
@Service
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final ProductClient productClient;
  private final InventoryClient inventoryClient;
  private final PaymentClient paymentClient;
  private final NotificationClient notificationClient;

  public OrderService(
      OrderRepository orderRepository,
      ProductClient productClient,
      InventoryClient inventoryClient,
      PaymentClient paymentClient,
      NotificationClient notificationClient) {
    this.orderRepository = orderRepository;
    this.productClient = productClient;
    this.inventoryClient = inventoryClient;
    this.paymentClient = paymentClient;
    this.notificationClient = notificationClient;
  }

  /**
   * Runs the placement flow and returns the resolved order.
   *
   * @throws UnknownProductException if a line references an unknown product
   * @throws StockUnavailableException if stock cannot be reserved (order saved as REJECTED_STOCK)
   * @throws PaymentDeclinedException if payment is declined (order saved as PAYMENT_FAILED)
   */
  @Transactional
  public OrderResponse place(PlaceOrderRequest request) {
    List<OrderLine> lines = priceLines(request.items());
    OrderEntity order =
        orderRepository.save(new OrderEntity(request.userId(), lines, OrderStatus.PENDING));

    reserveStock(order, lines);
    PaymentResult payment = authorizePayment(order);

    order.markPaid(payment.paymentId());
    orderRepository.save(order);
    notificationClient.orderConfirmed(order.getUserId(), order.getId());
    return OrderResponse.from(order);
  }

  @Transactional(readOnly = true)
  public OrderResponse getById(Long id) {
    return orderRepository
        .findWithLinesById(id)
        .map(OrderResponse::from)
        .orElseThrow(() -> new OrderNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> findByUser(String userId) {
    return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(OrderResponse::from)
        .toList();
  }

  private List<OrderLine> priceLines(List<PlaceOrderRequest.Item> items) {
    return items.stream().map(this::toPricedLine).toList();
  }

  private OrderLine toPricedLine(PlaceOrderRequest.Item item) {
    ProductClient.ProductView product = productClient.getProduct(item.productId());
    return new OrderLine(item.productId(), item.quantity(), product.priceCents());
  }

  private void reserveStock(OrderEntity order, List<OrderLine> lines) {
    try {
      lines.forEach(line -> inventoryClient.reserve(line.getProductId(), line.getQuantity()));
    } catch (StockUnavailableException ex) {
      order.markRejectedStock();
      orderRepository.save(order);
      log.info("order {} rejected: {}", order.getId(), ex.getMessage());
      throw ex;
    }
  }

  private PaymentResult authorizePayment(OrderEntity order) {
    PaymentResult result = paymentClient.authorize(order.getId(), order.getTotalCents());
    if (!result.isApproved()) {
      order.markPaymentFailed();
      orderRepository.save(order);
      log.info("order {} payment declined", order.getId());
      throw new PaymentDeclinedException(order.getId());
    }
    return result;
  }
}
