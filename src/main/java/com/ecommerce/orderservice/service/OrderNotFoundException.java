package com.ecommerce.orderservice.service;

/** Raised when an order id does not exist. */
public class OrderNotFoundException extends RuntimeException {

  public OrderNotFoundException(Long id) {
    super("No order with id " + id);
  }
}
