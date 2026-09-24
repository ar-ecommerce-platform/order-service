package com.ecommerce.orderservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.client.NotificationClient;
import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.PaymentClient.PaymentResult;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.client.ProductClient.ProductView;
import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.service.StockUnavailableException;
import com.ecommerce.orderservice.web.dto.PlaceOrderRequest;
import com.ecommerce.orderservice.web.dto.PlaceOrderRequest.Item;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the full OrderService orchestration and JPA layer against a real PostgreSQL instance
 * (Testcontainers) - only the HTTP boundary to the other services is mocked. Requires Docker.
 */
@SpringBootTest(
    properties = {
      "eureka.client.enabled=false",
      "spring.cloud.discovery.enabled=false",
      "spring.cloud.service-registry.auto-registration.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@Testcontainers
class OrderPersistenceIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private OrderService orderService;
  @Autowired private OrderRepository orderRepository;

  @MockitoBean private ProductClient productClient;
  @MockitoBean private InventoryClient inventoryClient;
  @MockitoBean private PaymentClient paymentClient;
  @MockitoBean private NotificationClient notificationClient;

  private static PlaceOrderRequest twoLines() {
    return new PlaceOrderRequest(List.of(new Item(1L, 2), new Item(2L, 1)));
  }

  @Test
  void place_persistsConfirmedOrderWithLinesToPostgres() {
    when(productClient.getProduct(1L)).thenReturn(new ProductView(1L, "Chair", 1299_00));
    when(productClient.getProduct(2L)).thenReturn(new ProductView(2L, "Desk", 899_00));
    when(paymentClient.authorize(any(), anyLong())).thenReturn(new PaymentResult(42L, "APPROVED"));

    var response = orderService.place("ada", twoLines());

    assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    var persisted = orderRepository.findWithLinesById(response.id()).orElseThrow();
    assertThat(persisted.getLines()).hasSize(2);
    assertThat(persisted.getTotalCents()).isEqualTo(1299_00L * 2 + 899_00L);
    assertThat(persisted.getPaymentId()).isEqualTo(42L);
  }

  @Test
  void place_persistsRejectedStockOrderWhenReservationFails() {
    when(productClient.getProduct(anyLong())).thenReturn(new ProductView(1L, "Chair", 1000));
    doThrow(new StockUnavailableException(1L)).when(inventoryClient).reserve(anyLong(), anyInt());

    assertThatThrownBy(() -> orderService.place("ada", twoLines()))
        .isInstanceOf(StockUnavailableException.class);

    assertThat(orderRepository.findByUserIdOrderByCreatedAtDesc("ada"))
        .anySatisfy(o -> assertThat(o.getStatus()).isEqualTo(OrderStatus.REJECTED_STOCK));
  }
}
