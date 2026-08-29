package com.ecommerce.orderservice.domain;

/** Lifecycle state of an order. Only {@link #PENDING} is non-terminal. */
public enum OrderStatus {
  PENDING,
  CONFIRMED,
  REJECTED_STOCK,
  PAYMENT_FAILED
}
