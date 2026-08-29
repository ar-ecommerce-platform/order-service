package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.OrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link OrderEntity}. Lines are eagerly fetched for read paths. */
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

  @EntityGraph(attributePaths = "lines")
  List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);

  @EntityGraph(attributePaths = "lines")
  Optional<OrderEntity> findWithLinesById(Long id);
}
