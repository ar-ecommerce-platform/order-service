package com.ecommerce.orderservice.entity;

import com.ecommerce.orderservice.domain.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A customer order and its resolved line items. */
@Entity
@Table(name = "orders")
public class OrderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(nullable = false)
  private long totalCents;

  private Long paymentId;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "order_id")
  private List<OrderLine> lines = new ArrayList<>();

  protected OrderEntity() {
    // for JPA
  }

  public OrderEntity(String userId, List<OrderLine> lines, OrderStatus status) {
    this.userId = userId;
    this.lines = new ArrayList<>(lines);
    this.status = status;
    this.totalCents = lines.stream().mapToLong(OrderLine::lineTotalCents).sum();
    this.createdAt = Instant.now();
  }

  public void markPaid(Long paymentId) {
    this.paymentId = paymentId;
    this.status = OrderStatus.CONFIRMED;
  }

  public void markRejectedStock() {
    this.status = OrderStatus.REJECTED_STOCK;
  }

  public void markPaymentFailed() {
    this.status = OrderStatus.PAYMENT_FAILED;
  }

  public Long getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public long getTotalCents() {
    return totalCents;
  }

  public Long getPaymentId() {
    return paymentId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public List<OrderLine> getLines() {
    return List.copyOf(lines);
  }
}
