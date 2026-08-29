package com.ecommerce.orderservice.client;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/** Talks to payment-service to authorize an order's total. */
@Component
public class PaymentClient {

  /** Result of an authorization attempt. */
  public record PaymentResult(Long paymentId, String status) {

    public boolean isApproved() {
      return "APPROVED".equals(status);
    }
  }

  private final RestClient client;

  public PaymentClient(
      Builder loadBalancedRestClientBuilder,
      @Value("${downstream.payment-service}") String baseUrl) {
    this.client = loadBalancedRestClientBuilder.baseUrl(baseUrl).build();
  }

  public PaymentResult authorize(Long orderId, long amountCents) {
    return client
        .post()
        .uri("/payments")
        .body(Map.of("orderId", orderId, "amountCents", amountCents))
        .retrieve()
        .body(PaymentResult.class);
  }
}
