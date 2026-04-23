package com.resto.pizzeria_api.controller;

import com.resto.pizzeria_api.model.Dish;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@ExtendWith(AllureJunit5.class)

@Epic("Gestion des plats")
@Feature("API REST — Dishes")
@Owner("resto-pizza-api")

@DisplayName("DishController — tests d'intégration")
class DishControllerTest {

  @Autowired private RestTestClient restTestClient;
  @Autowired private WebApplicationContext context;
  @Autowired private DishRepository dishRepository;
  @Autowired private OrderItemRepository orderItemRepository;
  @Autowired private OrderRepository orderRepository;

  // -------------------------------------------------------------------------
  // Setup
  // -------------------------------------------------------------------------

  @BeforeEach
  void setUp() {
    orderItemRepository.deleteAll();
    orderRepository.deleteAll();
    dishRepository.deleteAll();
    restTestClient = RestTestClient
        .bindToApplicationContext(context)
        .build();
  }

  // -------------------------------------------------------------------------
  // Fixtures
  // -------------------------------------------------------------------------

  private Dish buildDish(String name, BigDecimal price) {
    Dish d = new Dish();
    d.setName(name);
    d.setPrice(price);
    d.setCategory("Plat principal");
    d.setDescription("Une délicieuse pizza");
    d.setAvailable(true);
    return d;
  }

  private Dish persistDish(String name, BigDecimal price) {
    return dishRepository.save(buildDish(name, price));
  }

  /** Plat soft-deleté (available=false) — jamais visible dans les résultats */
  private Dish persistDeletedDish(String name, BigDecimal price) {
    Dish d = buildDish(name, price);
    d.setAvailable(false);
    return dishRepository.save(d);
  }

  private RestTestClientResponse exchange(RestTestClient.RequestHeadersSpec<?> spec) {
    return RestTestClientResponse.from(spec.exchange());
  }

  // =========================================================================
  // GET /api/dishes
  // =========================================================================

  @Nested
  @DisplayName("GET /api/dishes")
  class GetAllDishes {

    @Test
    @Story("Lister les plats disponibles")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que GET /api/dishes retourne 200 avec [] quand la table H2 est vide")
    @DisplayName("200 — tableau vide")
    void shouldReturn200WithEmptyArray() {
      Allure.step("HTTP GET /api/dishes (table vide)");
      var response = exchange(restTestClient.get().uri("/api/dishes"));

      Allure.step("Assert : status=200, body=[]");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .isEqualTo("[]");
    }

    @Test
    @Story("Lister les plats disponibles")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que GET /api/dishes retourne 200 avec les plats disponibles persistés en H2")
    @DisplayName("200 — liste de plats disponibles")
    void shouldReturn200WithAvailableDishes() {
      persistDish("Margherita", new BigDecimal("9.90"));
      persistDish("Regina",     new BigDecimal("11.50"));

      Allure.step("HTTP GET /api/dishes");
      var response = exchange(restTestClient.get().uri("/api/dishes"));

      Allure.step("Assert : status=200, [0].name=Margherita, [1].name=Regina");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$[0].name").isEqualTo("Margherita");

      assertThat(exchange(restTestClient.get().uri("/api/dishes")))
          .bodyJson()
          .extractingPath("$[1].name").isEqualTo("Regina");
    }

    @Test
    @Story("Lister les plats disponibles")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Les plats soft-deletés (available=false) ne doivent jamais apparaître dans GET /api/dishes")
    @DisplayName("200 — les plats soft-deletés sont exclus")
    void shouldExcludeSoftDeletedDishes() {
      persistDish("Margherita",   new BigDecimal("9.90"));
      persistDeletedDish("Invisible", new BigDecimal("5.00"));

      Allure.step("HTTP GET /api/dishes (1 dispo + 1 archivé en H2)");
      var response = exchange(restTestClient.get().uri("/api/dishes"));

      Allure.step("Assert : status=200, length=1 (Invisible exclu)");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.length()").isEqualTo(1);
    }
  }

  // =========================================================================
  // GET /api/dishes/{id}
  // =========================================================================

  @Nested
  @DisplayName("GET /api/dishes/{id}")
  class GetDishById {

    @Test
    @Story("Récupérer un plat par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Vérifie que GET /api/dishes/{id} retourne 200 + le plat H2 correspondant")
    @DisplayName("200 — plat trouvé")
    void shouldReturn200WhenExists() {
      Dish saved = persistDish("Margherita", new BigDecimal("9.90"));

      Allure.step("HTTP GET /api/dishes/" + saved.getId());
      var response = exchange(restTestClient.get().uri("/api/dishes/{id}", saved.getId()));

      Allure.step("Assert : status=200, name=Margherita, price=9.90");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.name").isEqualTo("Margherita");

      assertThat(exchange(restTestClient.get().uri("/api/dishes/{id}", saved.getId())))
          .bodyJson()
          .extractingPath("$.price").isEqualTo(9.90);
    }

    @Test
    @Story("Récupérer un plat par ID")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que GET /api/dishes/{id} retourne 404 + CODE_NOT_FOUND pour un ID inexistant")
    @DisplayName("404 — plat inexistant")
    void shouldReturn404WhenNotFound() {
      Allure.step("HTTP GET /api/dishes/9999 (ID absent de H2)");
      var response = exchange(restTestClient.get().uri("/api/dishes/{id}", 9999));

      Allure.step("Assert : status=404, codeExtended=CODE_NOT_FOUND");
      assertThat(response)
          .hasStatus(HttpStatus.NOT_FOUND)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
    }

    @Test
    @Story("Récupérer un plat par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Un plat soft-deleté (available=false) doit être traité comme inexistant — 404 attendu")
    @DisplayName("404 — plat soft-deleté")
    void shouldReturn404WhenSoftDeleted() {
      Dish deleted = persistDeletedDish("Invisible", new BigDecimal("5.00"));

      Allure.step("HTTP GET /api/dishes/" + deleted.getId() + " (plat archivé)");
      var response = exchange(restTestClient.get().uri("/api/dishes/{id}", deleted.getId()));

      Allure.step("Assert : status=404, codeExtended=CODE_NOT_FOUND");
      assertThat(response)
          .hasStatus(HttpStatus.NOT_FOUND)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
    }
  }

  // =========================================================================
  // POST /api/dishes
  // =========================================================================

  @Nested
  @DisplayName("POST /api/dishes")
  class CreateDish {

    @Test
    @Story("Créer un plat")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Vérifie que POST /api/dishes retourne 201 + l'ID généré + available=true par défaut")
    @DisplayName("201 — plat créé")
    void shouldReturn201WhenValid() {
      Allure.step("HTTP POST /api/dishes — payload {name:Margherita, price:9.90}");
      var response = RestTestClientResponse.from(
          restTestClient.post()
              .uri("/api/dishes")
              .contentType(MediaType.APPLICATION_JSON)
              .body(buildDish("Margherita", new BigDecimal("9.90")))
              .exchange());

      Allure.step("Assert : status=201, $.id présent, available=true");
      assertThat(response)
          .hasStatus(HttpStatus.CREATED)
          .bodyJson()
          .hasPath("$.id");

      assertThat(RestTestClientResponse.from(
          restTestClient.post()
              .uri("/api/dishes")
              .contentType(MediaType.APPLICATION_JSON)
              .body(buildDish("Regina", new BigDecimal("11.50")))
              .exchange()))
          .bodyJson()
          .extractingPath("$.available").isEqualTo(true);
    }

    @Test
    @Story("Créer un plat")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que POST /api/dishes retourne 400 + CODE_NOT_VALIDATED quand name=\"\"")
    @DisplayName("400 — name vide")
    void shouldReturn400WhenNameBlank() {
      Allure.step("HTTP POST /api/dishes — payload {name:\"\", price:9.90}");
      var response = RestTestClientResponse.from(
          restTestClient.post()
              .uri("/api/dishes")
              .contentType(MediaType.APPLICATION_JSON)
              .body(buildDish("", new BigDecimal("9.90")))
              .exchange());

      Allure.step("Assert : status=400, codeExtended=CODE_NOT_VALIDATED");
      assertThat(response)
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_VALIDATED");
    }

    @Test
    @Story("Créer un plat")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que POST /api/dishes retourne 400 + CODE_NOT_VALIDATED quand name trop court")
    @DisplayName("400 — name trop court")
    void shouldReturn400WhenNameTooShort() {
      Allure.step("HTTP POST /api/dishes — payload {name:\"A\", price:9.90}");
      var response = RestTestClientResponse.from(
          restTestClient.post()
              .uri("/api/dishes")
              .contentType(MediaType.APPLICATION_JSON)
              .body(buildDish("A", new BigDecimal("9.90")))
              .exchange());

      Allure.step("Assert : status=400, codeExtended=CODE_NOT_VALIDATED");
      assertThat(response)
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_VALIDATED");
    }

    @Test
    @Story("Créer un plat")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que POST /api/dishes retourne 400 + CODE_NOT_VALIDATED quand price=null")
    @DisplayName("400 — price null")
    void shouldReturn400WhenPriceNull() {
      Allure.step("HTTP POST /api/dishes — payload {name:Margherita, price:null}");
      var response = RestTestClientResponse.from(
          restTestClient.post()
              .uri("/api/dishes")
              .contentType(MediaType.APPLICATION_JSON)
              .body(buildDish("Margherita", null))
              .exchange());

      Allure.step("Assert : status=400, codeExtended=CODE_NOT_VALIDATED");
      assertThat(response)
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_VALIDATED");
    }

    @Test
    @Story("Créer un plat")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que POST /api/dishes retourne 409 + CODE_DB_INTEGRITY_VIOLATION si le nom existe déjà")
    @DisplayName("409 — nom dupliqué")
    void shouldReturn409WhenNameAlreadyExists() {
      persistDish("Margherita", new BigDecimal("9.90"));

      Allure.step("HTTP POST /api/dishes — payload {name:Margherita, price:12.00} (doublon)");
      var response = RestTestClientResponse.from(
          restTestClient.post()
              .uri("/api/dishes")
              .contentType(MediaType.APPLICATION_JSON)
              .body(buildDish("Margherita", new BigDecimal("12.00")))
              .exchange());

      Allure.step("Assert : status=409, codeExtended=CODE_DB_INTEGRITY_VIOLATION");
      assertThat(response)
          .hasStatus(HttpStatus.CONFLICT)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_DB_INTEGRITY_VIOLATION");
    }
  }

  // =========================================================================
  // PUT /api/dishes/{id}
  // =========================================================================

  @Nested
  @DisplayName("PUT /api/dishes/{id}")
  class UpdateDish {

    @Test
    @Story("Modifier un plat")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Vérifie que PUT /api/dishes/{id} met à jour les données H2 et retourne 200")
    @DisplayName("200 — plat mis à jour")
    void shouldReturn200WhenUpdated() {
      Dish saved = persistDish("Margherita", new BigDecimal("9.90"));

      Allure.step("HTTP PUT /api/dishes/" + saved.getId() + " — payload {name:Regina, price:11.50}");
      var response = RestTestClientResponse.from(
          restTestClient.put()
              .uri("/api/dishes/{id}", saved.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .body(buildDish("Regina", new BigDecimal("11.50")))
              .exchange());

      Allure.step("Assert : status=200, name=Regina, price=11.50 en base");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.name").isEqualTo("Regina");

      assertThat(exchange(restTestClient.get().uri("/api/dishes/{id}", saved.getId())))
          .bodyJson()
          .extractingPath("$.price").isEqualTo(11.50);
    }

    @Test
    @Story("Modifier un plat")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que PUT /api/dishes/{id} retourne 404 + CODE_NOT_FOUND pour un ID inexistant")
    @DisplayName("404 — plat inexistant")
    void shouldReturn404WhenNotFound() {
      Allure.step("HTTP PUT /api/dishes/9999 (ID absent de H2)");
      var response = RestTestClientResponse.from(
          restTestClient.put()
              .uri("/api/dishes/{id}", 9999)
              .contentType(MediaType.APPLICATION_JSON)
              .body(buildDish("Regina", new BigDecimal("11.50")))
              .exchange());

      Allure.step("Assert : status=404, codeExtended=CODE_NOT_FOUND");
      assertThat(response)
          .hasStatus(HttpStatus.NOT_FOUND)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
    }
  }

  // =========================================================================
  // DELETE /api/dishes/{id}
  // =========================================================================

  @Nested
  @DisplayName("DELETE /api/dishes/{id}")
  class DeleteDish {

    @Test
    @Story("Supprimer un plat (soft delete)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Vérifie que DELETE /api/dishes/{id} retourne 204 et met available=false en H2")
    @DisplayName("204 — soft delete effectué")
    void shouldReturn204AndSetAvailableFalse() {
      Dish saved = persistDish("Margherita", new BigDecimal("9.90"));

      Allure.step("HTTP DELETE /api/dishes/" + saved.getId());
      var response = exchange(restTestClient.delete().uri("/api/dishes/{id}", saved.getId()));

      Allure.step("Assert : status=204, available=false en H2");
      assertThat(response).hasStatus(HttpStatus.NO_CONTENT);

      Dish inDb = dishRepository.findById(saved.getId()).orElseThrow();
      assertThat(inDb.getAvailable()).isFalse();
    }

    @Test
    @Story("Supprimer un plat (soft delete)")
    @Severity(SeverityLevel.NORMAL)
    @Description("Vérifie que DELETE /api/dishes/{id} retourne 404 + CODE_NOT_FOUND pour un ID inexistant")
    @DisplayName("404 — plat inexistant")
    void shouldReturn404WhenNotFound() {
      Allure.step("HTTP DELETE /api/dishes/9999 (ID absent de H2)");
      var response = exchange(restTestClient.delete().uri("/api/dishes/{id}", 9999));

      Allure.step("Assert : status=404, codeExtended=CODE_NOT_FOUND");
      assertThat(response)
          .hasStatus(HttpStatus.NOT_FOUND)
          .bodyJson()
          .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
    }

    @Test
    @Story("Supprimer un plat (soft delete)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Après DELETE, le plat ne doit plus apparaître dans GET /api/dishes (available=false filtré)")
    @DisplayName("Plat soft-deleté disparaît du GET /api/dishes")
    void shouldMakeDishInvisibleInGetAll() {
      Dish saved = persistDish("Margherita", new BigDecimal("9.90"));

      Allure.step("HTTP DELETE /api/dishes/" + saved.getId());
      exchange(restTestClient.delete().uri("/api/dishes/{id}", saved.getId()));

      Allure.step("HTTP GET /api/dishes — le plat doit être absent");
      var response = exchange(restTestClient.get().uri("/api/dishes"));

      Allure.step("Assert : status=200, body=[]");
      assertThat(response)
          .hasStatusOk()
          .bodyJson()
          .isEqualTo("[]");
    }
  }
}