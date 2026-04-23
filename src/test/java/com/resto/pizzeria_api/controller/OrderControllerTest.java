package com.resto.pizzeria_api.controller;

import com.resto.pizzeria_api.model.Client;
import com.resto.pizzeria_api.model.Dish;
import com.resto.pizzeria_api.model.Order;
import com.resto.pizzeria_api.model.OrderItem;
import com.resto.pizzeria_api.repository.ClientRepository;
import com.resto.pizzeria_api.repository.DishRepository;
import com.resto.pizzeria_api.repository.OrderItemRepository;
import com.resto.pizzeria_api.repository.OrderRepository;
import io.qameta.allure.*;
import io.qameta.allure.junit5.AllureJunit5;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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
@ExtendWith(AllureJunit5.class)

@Epic("Gestion des commandes")
@Feature("API REST — Orders")
@Owner("resto-pizza-api")

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

  // -------------------------------------------------------------------------
  // Setup
  // -------------------------------------------------------------------------

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

    savedClient = new Client();
    savedClient.setFirstName("Jean");
    savedClient.setLastName("Dupont");
    savedClient = clientRepository.save(savedClient);

    restTestClient = RestTestClient.bindToApplicationContext(context).build();
  }

  // -------------------------------------------------------------------------
  // Fixtures
  // -------------------------------------------------------------------------

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

  private RestTestClientResponse exchange(RestTestClient.RequestHeadersSpec<?> spec) {
    return RestTestClientResponse.from(spec.exchange());
  }

  // =========================================================================
  // GET /api/orders
  // =========================================================================

  @Nested
  @DisplayName("GET /api/orders")
  class GetAllOrders {

    @Test
    @Story("Lister les commandes")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que GET /api/orders retourne 200 avec [] quand la table H2 est vide")
    @DisplayName("200 — tableau vide")
    void shouldReturn200WithEmptyArray() {
      Allure.step("HTTP GET /api/orders (table vide)");
      var response = exchange(restTestClient.get().uri("/api/orders"));

      Allure.step("Assert : status=200, body=[]");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .isEqualTo("[]");
    }

    @Test
    @Story("Lister les commandes")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Vérifie que GET /api/orders retourne 200 avec les commandes persistées en H2")
    @DisplayName("200 — liste de commandes")
    void shouldReturn200WithOrders() {
      persistOrder();

      Allure.step("HTTP GET /api/orders");
      var response = exchange(restTestClient.get().uri("/api/orders"));

      Allure.step("Assert : status=200, [0].dailyId=1");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$[0].dailyId").isEqualTo(1);
    }
  }

  // =========================================================================
  // GET /api/orders/{id}
  // =========================================================================

  @Nested
  @DisplayName("GET /api/orders/{id}")
  class GetOrderById {

    @Test
    @Story("Récupérer une commande par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Vérifie que GET /api/orders/{id} retourne 200 + la commande H2 correspondante")
    @DisplayName("200 — commande trouvée")
    void shouldReturn200WhenExists() {
      Order saved = persistOrder();

      Allure.step("HTTP GET /api/orders/" + saved.getId());
      var response = exchange(restTestClient.get().uri("/api/orders/{id}", saved.getId()));

      Allure.step("Assert : status=200, dailyId=1, items non vide");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.dailyId").isEqualTo(1);

      assertThat(exchange(restTestClient.get().uri("/api/orders/{id}", saved.getId())))
          .bodyJson()
          .extractingPath("$.items[0].quantity").isEqualTo(2);
    }

    @Test
    @Story("Récupérer une commande par ID")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que GET /api/orders/{id} retourne 404 + CODE_NOT_FOUND pour un ID inexistant")
    @DisplayName("404 — commande inexistante")
    void shouldReturn404WhenNotFound() {
      Allure.step("HTTP GET /api/orders/9999 (ID absent de H2)");
      var response = exchange(restTestClient.get().uri("/api/orders/{id}", 9999));

      Allure.step("Assert : status=404, codeExtended=CODE_NOT_FOUND");
      assertThat(response)
          .hasStatus(HttpStatus.NOT_FOUND)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
    }
  }

  // =========================================================================
  // POST /api/orders
  // =========================================================================

  @Nested
  @DisplayName("POST /api/orders")
  class CreateOrder {

    @Test
    @Story("Créer une commande")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Vérifie que POST /api/orders retourne 201 + l'ID généré + l'item persisté avec la bonne quantité")
    @DisplayName("201 — commande créée avec item")
    void shouldReturn201WithItem() {
      Allure.step("HTTP POST /api/orders — payload {dailyId:1, client, items:[{dish, quantity:2}]}");
      var response = RestTestClientResponse.from(
          restTestClient.post()
              .uri("/api/orders")
              .contentType(MediaType.APPLICATION_JSON)
              .body(orderBodyWithItem(savedClient.getId(), savedDish.getId(), 2))
              .exchange()
      );

      Allure.step("Assert : status=201, $.id présent, items[0].quantity=2");
      assertThat(response)
          .hasStatus(HttpStatus.CREATED)
          .bodyJson()
          .hasPath("$.id");

      assertThat(response)
          .bodyJson()
          .extractingPath("$.items[0].quantity").isEqualTo(2);
    }

    @Test
    @Story("Créer une commande")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que le contrôleur auto-assigne creationDate lors du POST /api/orders")
    @DisplayName("201 — creationDate auto-assignée")
    void shouldAutoAssignCreationDate() {
      Allure.step("HTTP POST /api/orders — payload minimal avec item");
      var response = RestTestClientResponse.from(
          restTestClient.post()
              .uri("/api/orders")
              .contentType(MediaType.APPLICATION_JSON)
              .body(orderBodyWithItem(savedClient.getId(), savedDish.getId(), 1))
              .exchange()
      );

      Allure.step("Assert : status=201, $.creationDate présent");
      assertThat(response)
          .hasStatus(HttpStatus.CREATED)
          .bodyJson()
          .hasPath("$.creationDate");
    }

    @Test
    @Story("Créer une commande")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que POST /api/orders accepte une commande sans item et retourne 201 avec items=[]")
    @DisplayName("201 — commande sans item")
    void shouldReturn201WithoutItems() {
      Allure.step("HTTP POST /api/orders — payload {dailyId:1, client, items:[]}");
      var response = RestTestClientResponse.from(
          restTestClient.post()
              .uri("/api/orders")
              .contentType(MediaType.APPLICATION_JSON)
              .body(orderBodyWithoutItem(savedClient.getId()))
              .exchange()
      );

      Allure.step("Assert : status=201, $.items=[]");
      assertThat(response)
          .hasStatus(HttpStatus.CREATED)
          .bodyJson()
          .extractingPath("$.items").isEqualTo(List.of());
    }
  }

  // =========================================================================
  // PUT /api/orders/{id}
  // =========================================================================

  @Nested
  @DisplayName("PUT /api/orders/{id}")
  class UpdateOrder {

    @Test
    @Story("Modifier une commande")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Vérifie que PUT /api/orders/{id} met à jour dailyId et les items en H2 et retourne 200")
    @DisplayName("200 — commande mise à jour")
    void shouldReturn200WhenUpdated() {
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

      Allure.step("HTTP PUT /api/orders/" + saved.getId() + " — payload {dailyId:99, quantity:5}");
      var response = RestTestClientResponse.from(
          restTestClient.put()
              .uri("/api/orders/{id}", saved.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .body(updatedBody)
              .exchange()
      );

      Allure.step("Assert : status=200, dailyId=99, items[0].quantity=5");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.dailyId").isEqualTo(99);

      assertThat(response)
          .bodyJson()
          .extractingPath("$.items[0].quantity").isEqualTo(5);
    }

    @Test
    @Story("Modifier une commande")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que PUT /api/orders/{id} retourne 404 + CODE_NOT_FOUND pour un ID inexistant")
    @DisplayName("404 — commande inexistante")
    void shouldReturn404WhenNotFound() {
      Allure.step("HTTP PUT /api/orders/9999 (ID absent de H2)");
      var response = RestTestClientResponse.from(
          restTestClient.put()
              .uri("/api/orders/{id}", 9999)
              .contentType(MediaType.APPLICATION_JSON)
              .body(orderBodyWithItem(savedClient.getId(), savedDish.getId(), 1))
              .exchange()
      );

      Allure.step("Assert : status=404, codeExtended=CODE_NOT_FOUND");
      assertThat(response)
          .hasStatus(HttpStatus.NOT_FOUND)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
    }
  }

  // =========================================================================
  // DELETE /api/orders/{id}
  // =========================================================================

  @Nested
  @DisplayName("DELETE /api/orders/{id}")
  class DeleteOrder {

    @Test
    @Story("Supprimer une commande (hard delete)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Vérifie que DELETE /api/orders/{id} retourne 204 et supprime définitivement la commande en H2")
    @DisplayName("204 — hard delete effectué")
    void shouldReturn204WhenDeleted() {
      Order saved = persistOrder();

      Allure.step("HTTP DELETE /api/orders/" + saved.getId());
      var response = exchange(restTestClient.delete().uri("/api/orders/{id}", saved.getId()));

      Allure.step("Assert : status=204, commande absente en H2 (hard delete)");
      assertThat(response).hasStatus(HttpStatus.NO_CONTENT);

      assertThat(orderRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @Story("Supprimer une commande (hard delete)")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que DELETE /api/orders/{id} retourne 404 + CODE_NOT_FOUND pour un ID inexistant")
    @DisplayName("404 — commande inexistante")
    void shouldReturn404WhenNotFound() {
      Allure.step("HTTP DELETE /api/orders/9999 (ID absent de H2)");
      var response = exchange(restTestClient.delete().uri("/api/orders/{id}", 9999));

      Allure.step("Assert : status=404, codeExtended=CODE_NOT_FOUND");
      assertThat(response)
          .hasStatus(HttpStatus.NOT_FOUND)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
    }

    @Test
    @Story("Supprimer une commande (hard delete)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Après DELETE, la commande ne doit plus apparaître dans GET /api/orders")
    @DisplayName("Commande hard-deletée disparaît du GET /api/orders")
    void shouldMakeOrderInvisibleInGetAll() {
      Order saved = persistOrder();

      Allure.step("HTTP DELETE /api/orders/" + saved.getId());
      exchange(restTestClient.delete().uri("/api/orders/{id}", saved.getId()));

      Allure.step("HTTP GET /api/orders — la commande doit être absente");
      var response = exchange(restTestClient.get().uri("/api/orders"));

      Allure.step("Assert : status=200, body=[]");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .isEqualTo("[]");
    }
  }
}