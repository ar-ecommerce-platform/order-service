package com.ecommerce.orderservice.service;

/** Raised when an order line references a product that product-service does not know. */
public class UnknownProductException extends RuntimeException {

  public UnknownProductException(Long productId) {
    super("Unknown product " + productId);
  }
}
