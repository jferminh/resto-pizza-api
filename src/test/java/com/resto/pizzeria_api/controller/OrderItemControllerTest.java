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

@Epic("Gestion des articles de commande")
@Feature("API REST — OrderItems")
@Owner("resto-pizza-api")

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
  // Fixtures
  // -------------------------------------------------------------------------

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

  private RestTestClientResponse exchange(RestTestClient.RequestHeadersSpec<?> spec) {
    return RestTestClientResponse.from(spec.exchange());
  }

  // =========================================================================
  // GET /api/order-items
  // =========================================================================

  @Nested
  @DisplayName("GET /api/order-items")
  class GetAllOrderItems {

    @Test
    @Story("Lister les articles")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que GET /api/order-items retourne 200 avec [] quand la table H2 est vide")
    @DisplayName("200 — tableau vide")
    void shouldReturn200WithEmptyArray() {
      Allure.step("HTTP GET /api/order-items (table vide)");
      var response = exchange(restTestClient.get().uri("/api/order-items"));

      Allure.step("Assert : status=200, body=[]");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .isEqualTo("[]");
    }

    @Test
    @Story("Lister les articles")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Vérifie que GET /api/order-items retourne 200 avec les articles persistés en H2")
    @DisplayName("200 — liste d'articles")
    void shouldReturn200WithItems() {
      persistItem(3);

      Allure.step("HTTP GET /api/order-items");
      var response = exchange(restTestClient.get().uri("/api/order-items"));

      Allure.step("Assert : status=200, [0].quantity=3");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$[0].quantity").isEqualTo(3);
    }
  }

  // =========================================================================
  // GET /api/order-items/{id}
  // =========================================================================

  @Nested
  @DisplayName("GET /api/order-items/{id}")
  class GetOrderItemById {

    @Test
    @Story("Récupérer un article par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Vérifie que GET /api/order-items/{id} retourne 200 + l'article H2 correspondant")
    @DisplayName("200 — article trouvé")
    void shouldReturn200WhenExists() {
      OrderItem saved = persistItem(2);

      Allure.step("HTTP GET /api/order-items/" + saved.getId());
      var response = exchange(restTestClient.get().uri("/api/order-items/{id}", saved.getId()));

      Allure.step("Assert : status=200, quantity=2");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.quantity").isEqualTo(2);
    }

    @Test
    @Story("Récupérer un article par ID")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que GET /api/order-items/{id} retourne 404 + CODE_NOT_FOUND pour un ID inexistant")
    @DisplayName("404 — article inexistant")
    void shouldReturn404WhenNotFound() {
      Allure.step("HTTP GET /api/order-items/9999 (ID absent de H2)");
      var response = exchange(restTestClient.get().uri("/api/order-items/{id}", 9999));

      Allure.step("Assert : status=404, codeExtended=CODE_NOT_FOUND");
      assertThat(response)
          .hasStatus(HttpStatus.NOT_FOUND)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
    }
  }

  // =========================================================================
  // POST /api/order-items
  // =========================================================================

  @Nested
  @DisplayName("POST /api/order-items")
  class CreateOrderItem {

    @Test
    @Story("Créer un article")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Vérifie que POST /api/order-items retourne 201 + l'ID généré + la quantité correcte")
    @DisplayName("201 — article créé")
    void shouldReturn201WhenValid() {
      // OPTIMISATION 2 : une seule requête POST, assertions sur id ET quantity dans la même réponse
      Allure.step("HTTP POST /api/order-items — payload {order, dish, quantity:4}");
      var response = RestTestClientResponse.from(
          restTestClient.post()
              .uri("/api/order-items")
              .contentType(MediaType.APPLICATION_JSON)
              .body(itemBody(savedOrder.getId(), savedDish.getId(), 4))
              .exchange()
      );

      Allure.step("Assert : status=201, $.id présent, quantity=4");
      assertThat(response)
          .hasStatus(HttpStatus.CREATED)
          .bodyJson()
          .hasPath("$.id");

      assertThat(response)
          .bodyJson()
          .extractingPath("$.quantity").isEqualTo(4);
    }

    @Test
    @Story("Créer un article")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que POST /api/order-items lie correctement le dish dans la réponse")
    @DisplayName("201 — dish lié correctement")
    void shouldLinkDishCorrectly() {
      Allure.step("HTTP POST /api/order-items — payload {order, dish:" + savedDish.getId() + ", quantity:1}");
      var response = RestTestClientResponse.from(
          restTestClient.post()
              .uri("/api/order-items")
              .contentType(MediaType.APPLICATION_JSON)
              .body(itemBody(savedOrder.getId(), savedDish.getId(), 1))
              .exchange()
      );

      Allure.step("Assert : status=201, $.dish.id=" + savedDish.getId());
      assertThat(response)
          .hasStatus(HttpStatus.CREATED)
          .bodyJson()
          .extractingPath("$.dish.id").isEqualTo(savedDish.getId());
    }
  }

  // =========================================================================
  // PUT /api/order-items/{id}
  // =========================================================================

  @Nested
  @DisplayName("PUT /api/order-items/{id}")
  class UpdateOrderItem {

    @Test
    @Story("Modifier un article")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Vérifie que PUT /api/order-items/{id} met à jour la quantité en H2 et retourne 200")
    @DisplayName("200 — quantité mise à jour")
    void shouldReturn200WithNewQuantity() {
      OrderItem saved = persistItem(2);

      String updatedBody = """
                {
                  "dish": { "id": %d },
                  "quantity": 10
                }
                """.formatted(savedDish.getId());

      Allure.step("HTTP PUT /api/order-items/" + saved.getId() + " — payload {quantity:10}");
      var response = RestTestClientResponse.from(
          restTestClient.put()
              .uri("/api/order-items/{id}", saved.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .body(updatedBody)
              .exchange()
      );

      Allure.step("Assert : status=200, quantity=10");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.quantity").isEqualTo(10);
    }

    @Test
    @Story("Modifier un article")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que PUT /api/order-items/{id} retourne 404 + CODE_NOT_FOUND pour un ID inexistant")
    @DisplayName("404 — article inexistant")
    void shouldReturn404WhenNotFound() {
      String body = """
                {
                  "dish": { "id": %d },
                  "quantity": 5
                }
                """.formatted(savedDish.getId());

      Allure.step("HTTP PUT /api/order-items/9999 (ID absent de H2)");
      var response = RestTestClientResponse.from(
          restTestClient.put()
              .uri("/api/order-items/{id}", 9999)
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
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
  // DELETE /api/order-items/{id}
  // =========================================================================

  @Nested
  @DisplayName("DELETE /api/order-items/{id}")
  class DeleteOrderItem {

    @Test
    @Story("Supprimer un article (hard delete)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Vérifie que DELETE /api/order-items/{id} retourne 204 et supprime définitivement l'article en H2")
    @DisplayName("204 — hard delete effectué")
    void shouldReturn204WhenDeleted() {
      OrderItem saved = persistItem(2);

      Allure.step("HTTP DELETE /api/order-items/" + saved.getId());
      var response = exchange(restTestClient.delete().uri("/api/order-items/{id}", saved.getId()));

      Allure.step("Assert : status=204, article absent en H2 (hard delete)");
      assertThat(response).hasStatus(HttpStatus.NO_CONTENT);

      assertThat(orderItemRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @Story("Supprimer un article (hard delete)")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que DELETE /api/order-items/{id} retourne 404 + CODE_NOT_FOUND pour un ID inexistant")
    @DisplayName("404 — article inexistant")
    void shouldReturn404WhenNotFound() {
      Allure.step("HTTP DELETE /api/order-items/9999 (ID absent de H2)");
      var response = exchange(restTestClient.delete().uri("/api/order-items/{id}", 9999));

      Allure.step("Assert : status=404, codeExtended=CODE_NOT_FOUND");
      assertThat(response)
          .hasStatus(HttpStatus.NOT_FOUND)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
    }

    @Test
    @Story("Supprimer un article (hard delete)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Après DELETE, l'article ne doit plus apparaître dans GET /api/order-items")
    @DisplayName("Article hard-deleté disparaît du GET /api/order-items")
    void shouldMakeItemInvisibleInGetAll() {
      OrderItem saved = persistItem(2);

      Allure.step("HTTP DELETE /api/order-items/" + saved.getId());
      exchange(restTestClient.delete().uri("/api/order-items/{id}", saved.getId()));

      Allure.step("HTTP GET /api/order-items — l'article doit être absent");
      var response = exchange(restTestClient.get().uri("/api/order-items"));

      Allure.step("Assert : status=200, body=[]");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .isEqualTo("[]");
    }
  }
}