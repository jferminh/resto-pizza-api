package com.resto.pizzeria_api.controller;

import com.resto.pizzeria_api.model.Client;
import com.resto.pizzeria_api.model.Dish;
import com.resto.pizzeria_api.model.Order;
import com.resto.pizzeria_api.model.OrderItem;
import com.resto.pizzeria_api.repository.ClientRepository;
import com.resto.pizzeria_api.repository.DishRepository;
import com.resto.pizzeria_api.repository.OrderItemRepository;
import com.resto.pizzeria_api.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@DisplayName("OrderItemController — tests d'intégration")
class OrderItemControllerTest {

  @Autowired private RestTestClient restTestClient;
  @Autowired private WebApplicationContext context;
  @Autowired private OrderItemRepository orderItemRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private DishRepository dishRepository;
  @Autowired private ClientRepository clientRepository;

  private Dish savedDish;
  private Order savedOrder;

  @BeforeEach
  void setUp() {
    orderItemRepository.deleteAll();
    orderRepository.deleteAll();
    dishRepository.deleteAll();
    clientRepository.deleteAll();

    savedDish = new Dish();
    savedDish.setName("Margherita");
    savedDish.setPrice(new BigDecimal("9.90"));
    savedDish.setDescription("Pizza classique");
    savedDish.setAvailable(true);
    savedDish = dishRepository.save(savedDish);

    Client client = new Client();
    client.setFirstName("Jean");
    client.setLastName("Dupont");
    client = clientRepository.save(client);

    savedOrder = new Order();
    savedOrder.setDailyId(1);
    savedOrder.setCreationDate(LocalDateTime.now());
    savedOrder.setClient(client);
    savedOrder.setItems(List.of());
    savedOrder = orderRepository.save(savedOrder);

    restTestClient = RestTestClient.bindToApplicationContext(context).build();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private RestTestClientResponse exchange(RestTestClient.RequestHeadersSpec<?> spec) {
    return RestTestClientResponse.from(spec.exchange());
  }

  private OrderItem persistItem(int quantity) {
    OrderItem item = new OrderItem();
    item.setOrder(savedOrder);
    item.setDish(savedDish);
    item.setQuantity(quantity);
    return orderItemRepository.save(item);
  }

  private String itemBody(Integer orderId, Integer dishId, int quantity) {
    return """
        {
          "order": { "id": %d },
          "dish":  { "id": %d },
          "quantity": %d
        }
        """.formatted(orderId, dishId, quantity);
  }

  // =========================================================================
  // GET /api/order-items
  // =========================================================================

  @Test
  @DisplayName("GET /api/order-items — 200 tableau vide")
  void getAllOrderItems_shouldReturn200WithEmptyArray() {
    assertThat(exchange(restTestClient.get().uri("/api/order-items")))
        .hasStatusOk()
        .bodyJson()
        .isEqualTo("[]");
  }

  @Test
  @DisplayName("GET /api/order-items — 200 avec items en base")
  void getAllOrderItems_shouldReturn200WithItems() {
    persistItem(3);

    assertThat(exchange(restTestClient.get().uri("/api/order-items")))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].quantity").isEqualTo(3);
  }

  // =========================================================================
  // GET /api/order-items/{id}
  // =========================================================================

  @Test
  @DisplayName("GET /api/order-items/{id} — 200 item trouvé")
  void getOrderItemById_shouldReturn200WhenExists() {
    OrderItem saved = persistItem(2);

    assertThat(exchange(restTestClient.get().uri("/api/order-items/{id}", saved.getId())))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.quantity").isEqualTo(2);
  }

  @Test
  @DisplayName("GET /api/order-items/{id} — 404 item inexistant")
  void getOrderItemById_shouldReturn404WhenNotFound() {
    assertThat(exchange(restTestClient.get().uri("/api/order-items/{id}", 9999)))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  // =========================================================================
  // POST /api/order-items
  // =========================================================================

  @Test
  @DisplayName("POST /api/order-items — 201 item créé")
  void createOrderItem_shouldReturn201WhenValid() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/order-items")
            .contentType(MediaType.APPLICATION_JSON)
            .body(itemBody(savedOrder.getId(), savedDish.getId(), 4))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .hasPath("$.id");

    assertThat(RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/order-items")
            .contentType(MediaType.APPLICATION_JSON)
            .body(itemBody(savedOrder.getId(), savedDish.getId(), 4))
            .exchange()))
        .bodyJson()
        .extractingPath("$.quantity").isEqualTo(4);
  }

  @Test
  @DisplayName("POST /api/order-items — dish lié correctement")
  void createOrderItem_shouldLinkDishCorrectly() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/order-items")
            .contentType(MediaType.APPLICATION_JSON)
            .body(itemBody(savedOrder.getId(), savedDish.getId(), 1))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .extractingPath("$.dish.id").isEqualTo(savedDish.getId());
  }

  // =========================================================================
  // PUT /api/order-items/{id}
  // =========================================================================

  @Test
  @DisplayName("PUT /api/order-items/{id} — 200 quantité mise à jour")
  void updateOrderItem_shouldReturn200WithNewQuantity() {
    OrderItem saved = persistItem(2);

    String updatedBody = """
        {
          "dish": { "id": %d },
          "quantity": 10
        }
        """.formatted(savedDish.getId());

    assertThat(RestTestClientResponse.from(
        restTestClient.put()
            .uri("/api/order-items/{id}", saved.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .body(updatedBody)
            .exchange()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.quantity").isEqualTo(10);
  }

  @Test
  @DisplayName("PUT /api/order-items/{id} — 404 item inexistant")
  void updateOrderItem_shouldReturn404WhenNotFound() {
    String body = """
        {
          "dish": { "id": %d },
          "quantity": 5
        }
        """.formatted(savedDish.getId());

    assertThat(RestTestClientResponse.from(
        restTestClient.put()
            .uri("/api/order-items/{id}", 9999)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .exchange()))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  // =========================================================================
  // DELETE /api/order-items/{id}
  // =========================================================================

  @Test
  @DisplayName("DELETE /api/order-items/{id} — 204 item supprimé")
  void deleteOrderItem_shouldReturn204WhenDeleted() {
    OrderItem saved = persistItem(2);

    assertThat(exchange(restTestClient.delete().uri("/api/order-items/{id}", saved.getId())))
        .hasStatus(HttpStatus.NO_CONTENT);

    assertThat(orderItemRepository.findById(saved.getId())).isEmpty();
  }

  @Test
  @DisplayName("DELETE /api/order-items/{id} — 404 item inexistant")
  void deleteOrderItem_shouldReturn404WhenNotFound() {
    assertThat(exchange(restTestClient.delete().uri("/api/order-items/{id}", 9999)))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }
}