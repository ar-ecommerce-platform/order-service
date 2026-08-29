package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.service.StockUnavailableException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/** Talks to inventory-service to reserve stock for order lines. */
@Component
public class InventoryClient {

  private final RestClient client;

  public InventoryClient(
      Builder loadBalancedRestClientBuilder,
      @Value("${downstream.inventory-service}") String baseUrl) {
    this.client = loadBalancedRestClientBuilder.baseUrl(baseUrl).build();
  }

  /**
   * Reserves {@code quantity} units of a product.
   *
   * @throws StockUnavailableException if the reservation is rejected (404 or 409)
   */
  public void reserve(Long productId, int quantity) {
    client
        .post()
        .uri("/inventory/{id}/reserve", productId)
        .body(Map.of("quantity", quantity))
        .retrieve()
        .onStatus(InventoryClient::isReservationRejected, (req, res) -> raiseUnavailable(productId))
        .toBodilessEntity();
  }

  private static boolean isReservationRejected(HttpStatusCode status) {
    return status.isSameCodeAs(HttpStatus.CONFLICT) || status.isSameCodeAs(HttpStatus.NOT_FOUND);
  }

  private static void raiseUnavailable(Long productId) {
    throw new StockUnavailableException(productId);
  }
}
