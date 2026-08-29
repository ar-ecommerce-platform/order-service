package com.ecommerce.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One product line within an {@link OrderEntity}. */
@Entity
@Table(name = "order_lines")
public class OrderLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long productId;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false)
  private long unitPriceCents;

  protected OrderLine() {
    // for JPA
  }

  public OrderLine(Long productId, int quantity, long unitPriceCents) {
    this.productId = productId;
    this.quantity = quantity;
    this.unitPriceCents = unitPriceCents;
  }

  public long lineTotalCents() {
    return unitPriceCents * quantity;
  }

  public Long getId() {
    return id;
  }

  public Long getProductId() {
    return productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public long getUnitPriceCents() {
    return unitPriceCents;
  }
}
