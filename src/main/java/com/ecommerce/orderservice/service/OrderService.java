package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.client.NotificationClient;
import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.PaymentClient.PaymentResult;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.entity.OrderEntity;
import com.ecommerce.orderservice.entity.OrderLine;
import com.ecommerce.orderservice.web.dto.OrderResponse;
import com.ecommerce.orderservice.web.dto.PlaceOrderRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Places orders by orchestrating the product, inventory, payment and notification services.
 *
 * <p>The orchestration is not wrapped in a single database transaction - it spans external HTTP
 * calls. Each state change is a short independent transaction ({@link OrderTransactions}). This is
 * a deliberately simple flow: it does <em>not</em> compensate a partial stock reservation if a
 * later line fails, retry, or wrap calls in a circuit breaker.
 */
@Service
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderTransactions store;
  private final ProductClient productClient;
  private final InventoryClient inventoryClient;
  private final PaymentClient paymentClient;
  private final NotificationClient notificationClient;

  public OrderService(
      OrderTransactions store,
      ProductClient productClient,
      InventoryClient inventoryClient,
      PaymentClient paymentClient,
      NotificationClient notificationClient) {
    this.store = store;
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
  public OrderResponse place(PlaceOrderRequest request) {
    List<OrderLine> lines = priceLines(request.items());
    OrderEntity order = store.createPending(request.userId(), lines);

    reserveStock(order.getId(), lines);
    Long paymentId = authorizePayment(order.getId(), order.getTotalCents());

    OrderResponse confirmed = store.confirm(order.getId(), paymentId);
    notificationClient.orderConfirmed(request.userId(), order.getId());
    return confirmed;
  }

  public OrderResponse getById(Long id) {
    return store.getById(id);
  }

  public List<OrderResponse> findByUser(String userId) {
    return store.findByUser(userId);
  }

  private List<OrderLine> priceLines(List<PlaceOrderRequest.Item> items) {
    return items.stream().map(this::toPricedLine).toList();
  }

  private OrderLine toPricedLine(PlaceOrderRequest.Item item) {
    ProductClient.ProductView product = productClient.getProduct(item.productId());
    return new OrderLine(item.productId(), item.quantity(), product.priceCents());
  }

  private void reserveStock(Long orderId, List<OrderLine> lines) {
    try {
      lines.forEach(line -> inventoryClient.reserve(line.getProductId(), line.getQuantity()));
    } catch (StockUnavailableException ex) {
      store.markRejectedStock(orderId);
      log.info("order {} rejected: {}", orderId, ex.getMessage());
      throw ex;
    }
  }

  private Long authorizePayment(Long orderId, long totalCents) {
    PaymentResult result = paymentClient.authorize(orderId, totalCents);
    if (!result.isApproved()) {
      store.markPaymentFailed(orderId);
      log.info("order {} payment declined", orderId);
      throw new PaymentDeclinedException(orderId);
    }
    return result.paymentId();
  }
}
