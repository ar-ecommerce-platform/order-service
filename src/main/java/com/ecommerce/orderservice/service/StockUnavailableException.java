package com.ecommerce.orderservice.service;

/** Raised when inventory-service cannot reserve the requested quantity. */
public class StockUnavailableException extends RuntimeException {

  public StockUnavailableException(Long productId) {
    super("Stock unavailable for product " + productId);
  }
}
