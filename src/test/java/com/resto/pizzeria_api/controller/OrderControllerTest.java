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
@DisplayName("OrderController — tests d'intégration")
class OrderControllerTest {

  @Autowired private RestTestClient restTestClient;
  @Autowired private WebApplicationContext context;
  @Autowired private OrderRepository orderRepository;
  @Autowired private OrderItemRepository orderItemRepository;
  @Autowired private DishRepository dishRepository;
  @Autowired private ClientRepository clientRepository;

  private Dish savedDish;
  private Client savedClient;

  @BeforeEach
  void setUp() {
    // Ordre de suppression respectant les FK
    orderItemRepository.deleteAll();
    orderRepository.deleteAll();
    dishRepository.deleteAll();
    clientRepository.deleteAll();

    // Fixtures partagées
    savedDish = new Dish();
    savedDish.setName("Margherita");
    savedDish.setPrice(new BigDecimal("9.90"));
    savedDish.setDescription("Pizza classique");
    savedDish.setAvailable(true);
    savedDish = dishRepository.save(savedDish);

    savedClient = new Client();
    savedClient.setFirstName("Jean");
    savedClient.setLastName("Dupont");
    savedClient = clientRepository.save(savedClient);

    restTestClient = RestTestClient.bindToApplicationContext(context).build();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private RestTestClientResponse exchange(RestTestClient.RequestHeadersSpec<?> spec) {
    return RestTestClientResponse.from(spec.exchange());
  }

  /** Construit un body JSON de commande minimal avec un item */
  private String orderBodyWithItem(Integer clientId, Integer dishId, int quantity) {
    return """
        {
          "dailyId": 1,
          "client": { "id": %d },
          "items": [
            { "dish": { "id": %d }, "quantity": %d }
          ]
        }
        """.formatted(clientId, dishId, quantity);
  }

  /** Construit un body JSON de commande sans item */
  private String orderBodyWithoutItem(Integer clientId) {
    return """
        {
          "dailyId": 1,
          "client": { "id": %d },
          "items": []
        }
        """.formatted(clientId);
  }

  /** Persiste une commande complète en base via repository */
  private Order persistOrder() {
    Order order = new Order();
    order.setDailyId(1);
    order.setCreationDate(LocalDateTime.now());
    order.setClient(savedClient);

    OrderItem item = new OrderItem();
    item.setDish(savedDish);
    item.setQuantity(2);
    item.setOrder(order);
    order.setItems(List.of(item));

    return orderRepository.save(order);
  }

  // =========================================================================
  // GET /api/orders
  // =========================================================================

  @Test
  @DisplayName("GET /api/orders — 200 tableau vide")
  void getAllOrders_shouldReturn200WithEmptyArray() {
    assertThat(exchange(restTestClient.get().uri("/api/orders")))
        .hasStatusOk()
        .bodyJson()
        .isEqualTo("[]");
  }

  @Test
  @DisplayName("GET /api/orders — 200 avec commandes en base")
  void getAllOrders_shouldReturn200WithOrders() {
    persistOrder();

    assertThat(exchange(restTestClient.get().uri("/api/orders")))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].dailyId").isEqualTo(1);
  }

  // =========================================================================
  // GET /api/orders/{id}
  // =========================================================================

  @Test
  @DisplayName("GET /api/orders/{id} — 200 commande trouvée")
  void getOrderById_shouldReturn200WhenExists() {
    Order saved = persistOrder();

    assertThat(exchange(restTestClient.get().uri("/api/orders/{id}", saved.getId())))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.dailyId").isEqualTo(1);
  }

  @Test
  @DisplayName("GET /api/orders/{id} — 404 commande inexistante")
  void getOrderById_shouldReturn404WhenNotFound() {
    assertThat(exchange(restTestClient.get().uri("/api/orders/{id}", 9999)))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  // =========================================================================
  // POST /api/orders
  // =========================================================================

  @Test
  @DisplayName("POST /api/orders — 201 commande créée avec item")
  void createOrder_shouldReturn201WithItem() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .body(orderBodyWithItem(savedClient.getId(), savedDish.getId(), 2))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .hasPath("$.id");

    assertThat(RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .body(orderBodyWithItem(savedClient.getId(), savedDish.getId(), 3))
            .exchange()))
        .bodyJson()
        .extractingPath("$.items[0].quantity").isEqualTo(3);
  }

  @Test
  @DisplayName("POST /api/orders — creationDate auto-assignée")
  void createOrder_shouldAutoAssignCreationDate() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .body(orderBodyWithItem(savedClient.getId(), savedDish.getId(), 1))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .hasPath("$.creationDate");
  }

  @Test
  @DisplayName("POST /api/orders — 201 commande sans item")
  void createOrder_shouldReturn201WithoutItems() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .body(orderBodyWithoutItem(savedClient.getId()))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .extractingPath("$.items").isEqualTo(List.of());
  }

  // =========================================================================
  // PUT /api/orders/{id}
  // =========================================================================

  @Test
  @DisplayName("PUT /api/orders/{id} — 200 commande mise à jour")
  void updateOrder_shouldReturn200WhenUpdated() {
    Order saved = persistOrder();

    String updatedBody = """
        {
          "dailyId": 99,
          "client": { "id": %d },
          "items": [
            { "dish": { "id": %d }, "quantity": 5 }
          ]
        }
        """.formatted(savedClient.getId(), savedDish.getId());

    var response = RestTestClientResponse.from(
        restTestClient.put()
            .uri("/api/orders/{id}", saved.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .body(updatedBody)
            .exchange());

    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.dailyId").isEqualTo(99);

    assertThat(RestTestClientResponse.from(
        restTestClient.put()
            .uri("/api/orders/{id}", saved.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .body(updatedBody)
            .exchange()))
        .bodyJson()
        .extractingPath("$.items[0].quantity").isEqualTo(5);
  }

  @Test
  @DisplayName("PUT /api/orders/{id} — 404 commande inexistante")
  void updateOrder_shouldReturn404WhenNotFound() {
    var response = RestTestClientResponse.from(
        restTestClient.put()
            .uri("/api/orders/{id}", 9999)
            .contentType(MediaType.APPLICATION_JSON)
            .body(orderBodyWithItem(savedClient.getId(), savedDish.getId(), 1))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  // =========================================================================
  // DELETE /api/orders/{id}
  // =========================================================================

  @Test
  @DisplayName("DELETE /api/orders/{id} — 204 commande supprimée")
  void deleteOrder_shouldReturn204WhenDeleted() {
    Order saved = persistOrder();

    assertThat(exchange(restTestClient.delete().uri("/api/orders/{id}", saved.getId())))
        .hasStatus(HttpStatus.NO_CONTENT);

    // Vérifie que la commande n'est plus en base
    assertThat(orderRepository.findById(saved.getId())).isEmpty();
  }

  @Test
  @DisplayName("DELETE /api/orders/{id} — 404 commande inexistante")
  void deleteOrder_shouldReturn404WhenNotFound() {
    assertThat(exchange(restTestClient.delete().uri("/api/orders/{id}", 9999)))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }
}