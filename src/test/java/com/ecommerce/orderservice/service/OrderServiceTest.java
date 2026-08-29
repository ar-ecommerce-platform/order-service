package com.ecommerce.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.client.NotificationClient;
import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.PaymentClient.PaymentResult;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.entity.OrderEntity;
import com.ecommerce.orderservice.entity.OrderLine;
import com.ecommerce.orderservice.web.dto.OrderResponse;
import com.ecommerce.orderservice.web.dto.PlaceOrderRequest;
import com.ecommerce.orderservice.web.dto.PlaceOrderRequest.Item;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderServiceTest {

  private OrderTransactions store;
  private ProductClient productClient;
  private InventoryClient inventoryClient;
  private PaymentClient paymentClient;
  private OrderService service;

  @BeforeEach
  void setUp() {
    store = Mockito.mock(OrderTransactions.class);
    productClient = Mockito.mock(ProductClient.class);
    inventoryClient = Mockito.mock(InventoryClient.class);
    paymentClient = Mockito.mock(PaymentClient.class);
    NotificationClient notificationClient = Mockito.mock(NotificationClient.class);
    service =
        new OrderService(store, productClient, inventoryClient, paymentClient, notificationClient);

    when(productClient.getProduct(1L)).thenReturn(new ProductClient.ProductView(1L, "Desk", 2000L));
    when(store.createPending(eq("ada"), any()))
        .thenReturn(
            new OrderEntity("ada", List.of(new OrderLine(1L, 2, 2000)), OrderStatus.PENDING));
  }

  private static PlaceOrderRequest request(int quantity) {
    return new PlaceOrderRequest("ada", List.of(new Item(1L, quantity)));
  }

  @Test
  void place_confirmsOrderOnHappyPath() {
    when(paymentClient.authorize(any(), anyLong())).thenReturn(new PaymentResult(99L, "APPROVED"));
    when(store.confirm(any(), eq(99L)))
        .thenReturn(
            new OrderResponse(
                1L, "ada", OrderStatus.CONFIRMED, 4000, 99L, Instant.now(), List.of()));

    OrderResponse order = service.place(request(2));

    assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(order.paymentId()).isEqualTo(99L);
  }

  @Test
  void place_marksRejectedStockWhenReservationFails() {
    doThrow(new StockUnavailableException(1L)).when(inventoryClient).reserve(eq(1L), anyInt());

    assertThatThrownBy(() -> service.place(request(2)))
        .isInstanceOf(StockUnavailableException.class);
    verify(store).markRejectedStock(any());
  }

  @Test
  void place_marksPaymentFailedWhenDeclined() {
    when(paymentClient.authorize(any(), anyLong())).thenReturn(new PaymentResult(100L, "DECLINED"));

    assertThatThrownBy(() -> service.place(request(2)))
        .isInstanceOf(PaymentDeclinedException.class);
    verify(store).markPaymentFailed(any());
  }
}
