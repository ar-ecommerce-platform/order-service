package com.ecommerce.orderservice.service;

/** Raised when payment-service declines authorization for an order total. */
public class PaymentDeclinedException extends RuntimeException {

  public PaymentDeclinedException(Long orderId) {
    super("Payment declined for order " + orderId);
  }
}
