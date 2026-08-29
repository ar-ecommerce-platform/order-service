package com.ecommerce.orderservice.web;

import com.ecommerce.orderservice.service.OrderNotFoundException;
import com.ecommerce.orderservice.service.PaymentDeclinedException;
import com.ecommerce.orderservice.service.StockUnavailableException;
import com.ecommerce.orderservice.service.UnknownProductException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps orchestration failures onto meaningful HTTP status codes. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(OrderNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(OrderNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", ex.getMessage());
  }

  @ExceptionHandler(UnknownProductException.class)
  public ResponseEntity<ApiError> handleUnknownProduct(UnknownProductException ex) {
    return build(HttpStatus.BAD_REQUEST, "UNKNOWN_PRODUCT", ex.getMessage());
  }

  @ExceptionHandler(StockUnavailableException.class)
  public ResponseEntity<ApiError> handleStock(StockUnavailableException ex) {
    return build(HttpStatus.CONFLICT, "REJECTED_STOCK", ex.getMessage());
  }

  @ExceptionHandler(PaymentDeclinedException.class)
  public ResponseEntity<ApiError> handlePayment(PaymentDeclinedException ex) {
    return build(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", ex.getMessage());
  }

  private static ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message));
  }
}
