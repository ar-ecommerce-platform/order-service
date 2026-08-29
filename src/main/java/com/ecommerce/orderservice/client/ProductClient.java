package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.service.UnknownProductException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/** Talks to product-service to price and validate order lines. */
@Component
public class ProductClient {

  /** Minimal projection of a product; only what the order flow needs. */
  public record ProductView(Long id, String name, long priceCents) {}

  private final RestClient client;

  public ProductClient(
      Builder loadBalancedRestClientBuilder,
      @Value("${downstream.product-service}") String baseUrl) {
    this.client = loadBalancedRestClientBuilder.baseUrl(baseUrl).build();
  }

  /**
   * Fetches a product by id.
   *
   * @throws UnknownProductException if the product does not exist
   */
  public ProductView getProduct(Long productId) {
    ProductView product =
        client
            .get()
            .uri("/products/{id}", productId)
            .retrieve()
            .onStatus(
                status -> status.isSameCodeAs(HttpStatus.NOT_FOUND),
                (req, res) -> raiseUnknown(productId))
            .body(ProductView.class);
    if (product == null) {
      throw new UnknownProductException(productId);
    }
    return product;
  }

  private static void raiseUnknown(Long productId) {
    throw new UnknownProductException(productId);
  }
}
