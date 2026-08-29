package com.ecommerce.orderservice.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.service.OrderNotFoundException;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.service.PaymentDeclinedException;
import com.ecommerce.orderservice.service.StockUnavailableException;
import com.ecommerce.orderservice.web.dto.OrderResponse;
import com.ecommerce.orderservice.web.dto.PlaceOrderRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

  private static final String ONE_ITEM =
      "{\"userId\":\"ada\",\"items\":[{\"productId\":1,\"quantity\":2}]}";

  @Autowired private MockMvc mvc;

  @MockitoBean private OrderService service;

  private static OrderResponse confirmed() {
    return new OrderResponse(
        1L,
        "ada",
        OrderStatus.CONFIRMED,
        4000,
        99L,
        Instant.now(),
        List.of(new OrderResponse.Line(1L, 2, 2000, 4000)));
  }

  @Test
  void place_returns201Confirmed() throws Exception {
    when(service.place(any(PlaceOrderRequest.class))).thenReturn(confirmed());

    mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(ONE_ITEM))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.totalCents").value(4000));
  }

  @Test
  void place_stockUnavailable_returns409() throws Exception {
    when(service.place(any())).thenThrow(new StockUnavailableException(1L));

    mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(ONE_ITEM))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("REJECTED_STOCK"));
  }

  @Test
  void place_paymentDeclined_returns402() throws Exception {
    when(service.place(any())).thenThrow(new PaymentDeclinedException(1L));

    mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(ONE_ITEM))
        .andExpect(status().isPaymentRequired())
        .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"));
  }

  @Test
  void place_rejectsEmptyItems() throws Exception {
    mvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"ada\",\"items\":[]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getById_missing_returns404() throws Exception {
    when(service.getById(9L)).thenThrow(new OrderNotFoundException(9L));

    mvc.perform(get("/orders/9"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
  }
}
