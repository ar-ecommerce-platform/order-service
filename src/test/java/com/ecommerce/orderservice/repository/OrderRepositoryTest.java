package com.ecommerce.orderservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.entity.OrderEntity;
import com.ecommerce.orderservice.entity.OrderLine;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class OrderRepositoryTest {

  @Autowired private OrderRepository repository;

  private OrderEntity newOrder(String userId) {
    return new OrderEntity(
        userId,
        List.of(new OrderLine(1L, 2, 2000), new OrderLine(2L, 1, 1500)),
        OrderStatus.CONFIRMED);
  }

  @Test
  void findByUser_returnsOnlyThatUsersOrders() {
    repository.save(newOrder("ada"));
    repository.save(newOrder("ada"));
    repository.save(newOrder("grace"));

    assertThat(repository.findByUserIdOrderByCreatedAtDesc("ada")).hasSize(2);
    assertThat(repository.findByUserIdOrderByCreatedAtDesc("nobody")).isEmpty();
  }

  @Test
  void findWithLinesById_loadsLines() {
    Long id = repository.save(newOrder("ada")).getId();

    assertThat(repository.findWithLinesById(id))
        .isPresent()
        .get()
        .satisfies(
            order -> {
              assertThat(order.getLines()).hasSize(2);
              assertThat(order.getTotalCents()).isEqualTo(5500);
            });
  }
}
