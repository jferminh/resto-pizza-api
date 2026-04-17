package com.resto.pizzeria_api.controller;

import com.resto.pizzeria_api.model.Dish;
import com.resto.pizzeria_api.repository.DishRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@DisplayName("DishController — tests d'intégration")
class DishControllerTest {

  @Autowired
  private RestTestClient restTestClient;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DishRepository dishRepository;

  @BeforeEach
  void setUp() {
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

  /** Plat soft-deleté (available = false) — ne doit jamais apparaître dans les résultats */
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

  @Test
  @DisplayName("GET /api/dishes — 200 tableau vide")
  void getAllDishes_shouldReturn200WithEmptyArray() {
    var response = exchange(restTestClient.get().uri("/api/dishes"));

    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .isEqualTo("[]");
  }

  @Test
  @DisplayName("GET /api/dishes — 200 avec plats disponibles")
  void getAllDishes_shouldReturn200WithAvailableDishes() {
    persistDish("Margherita", new BigDecimal("9.90"));
    persistDish("Regina", new BigDecimal("11.50"));

    var response = exchange(restTestClient.get().uri("/api/dishes"));

    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].name").isEqualTo("Margherita");

    assertThat(RestTestClientResponse.from(
        restTestClient.get().uri("/api/dishes").exchange()))
        .bodyJson()
        .extractingPath("$[1].name").isEqualTo("Regina");
  }

  @Test
  @DisplayName("GET /api/dishes — les plats soft-deletés sont exclus")
  void getAllDishes_shouldExcludeSoftDeletedDishes() {
    persistDish("Margherita", new BigDecimal("9.90"));
    persistDeletedDish("Invisible", new BigDecimal("5.00"));

    // Seule Margherita doit apparaître → tableau de taille 1
    var response = exchange(restTestClient.get().uri("/api/dishes"));

    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.length()").isEqualTo(1);
  }

  // =========================================================================
  // GET /api/dishes/{id}
  // =========================================================================

  @Test
  @DisplayName("GET /api/dishes/{id} — 200 plat trouvé")
  void getDishById_shouldReturn200WhenExists() {
    Dish saved = persistDish("Margherita", new BigDecimal("9.90"));

    var response = exchange(restTestClient.get().uri("/api/dishes/{id}", saved.getId()));

    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.name").isEqualTo("Margherita");

    assertThat(RestTestClientResponse.from(
        restTestClient.get().uri("/api/dishes/{id}", saved.getId()).exchange()))
        .bodyJson()
        .extractingPath("$.price").isEqualTo(9.90);
  }

  @Test
  @DisplayName("GET /api/dishes/{id} — 404 plat inexistant")
  void getDishById_shouldReturn404WhenNotFound() {
    var response = exchange(restTestClient.get().uri("/api/dishes/{id}", 9999));

    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  @Test
  @DisplayName("GET /api/dishes/{id} — 404 si plat soft-deleté")
  void getDishById_shouldReturn404WhenSoftDeleted() {
    Dish deleted = persistDeletedDish("Invisible", new BigDecimal("5.00"));

    var response = exchange(restTestClient.get().uri("/api/dishes/{id}", deleted.getId()));

    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  // =========================================================================
  // POST /api/dishes
  // =========================================================================

  @Test
  @DisplayName("POST /api/dishes — 201 plat créé")
  void createDish_shouldReturn201WhenValid() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/dishes")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildDish("Margherita", new BigDecimal("9.90")))
            .exchange());

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
  @DisplayName("POST /api/dishes — 400 name vide")
  void createDish_shouldReturn400WhenNameBlank() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/dishes")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildDish("", new BigDecimal("9.90")))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_VALIDATED");
  }

  @Test
  @DisplayName("POST /api/dishes — 400 name trop court")
  void createDish_shouldReturn400WhenNameTooShort() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/dishes")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildDish("A", new BigDecimal("9.90")))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_VALIDATED");
  }

  @Test
  @DisplayName("POST /api/dishes — 400 price null")
  void createDish_shouldReturn400WhenPriceNull() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/dishes")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildDish("Margherita", null))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_VALIDATED");
  }

  @Test
  @DisplayName("POST /api/dishes — 409 nom dupliqué")
  void createDish_shouldReturn409WhenNameAlreadyExists() {
    persistDish("Margherita", new BigDecimal("9.90"));

    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/dishes")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildDish("Margherita", new BigDecimal("12.00")))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.CONFLICT)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_DB_INTEGRITY_VIOLATION");
  }

  // =========================================================================
  // PUT /api/dishes/{id}
  // =========================================================================

  @Test
  @DisplayName("PUT /api/dishes/{id} — 200 plat mis à jour")
  void updateDish_shouldReturn200WhenUpdated() {
    Dish saved = persistDish("Margherita", new BigDecimal("9.90"));

    var response = RestTestClientResponse.from(
        restTestClient.put()
            .uri("/api/dishes/{id}", saved.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildDish("Regina", new BigDecimal("11.50")))
            .exchange());

    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.name").isEqualTo("Regina");

    assertThat(RestTestClientResponse.from(
        restTestClient.get().uri("/api/dishes/{id}", saved.getId()).exchange()))
        .bodyJson()
        .extractingPath("$.price").isEqualTo(11.50);
  }

  @Test
  @DisplayName("PUT /api/dishes/{id} — 404 plat inexistant")
  void updateDish_shouldReturn404WhenNotFound() {
    var response = RestTestClientResponse.from(
        restTestClient.put()
            .uri("/api/dishes/{id}", 9999)
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildDish("Regina", new BigDecimal("11.50")))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  // =========================================================================
  // DELETE /api/dishes/{id}
  // =========================================================================

  @Test
  @DisplayName("DELETE /api/dishes/{id} — 204 soft delete effectué")
  void deleteDish_shouldReturn204AndSetAvailableFalse() {
    Dish saved = persistDish("Margherita", new BigDecimal("9.90"));

    var response = exchange(restTestClient.delete().uri("/api/dishes/{id}", saved.getId()));

    // Vérifie le statut HTTP
    assertThat(response).hasStatus(HttpStatus.NO_CONTENT);

    // Vérifie que le plat est bien soft-deleté en base
    Dish inDb = dishRepository.findById(saved.getId()).orElseThrow();
    assertThat(inDb.getAvailable()).isFalse();
  }

  @Test
  @DisplayName("DELETE /api/dishes/{id} — 404 plat inexistant")
  void deleteDish_shouldReturn404WhenNotFound() {
    var response = exchange(restTestClient.delete().uri("/api/dishes/{id}", 9999));

    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  @Test
  @DisplayName("DELETE /api/dishes/{id} — plat soft-deleté disparaît du GET")
  void deleteDish_shouldMakeDishInvisibleInGetAll() {
    Dish saved = persistDish("Margherita", new BigDecimal("9.90"));

    // Suppression
    exchange(restTestClient.delete().uri("/api/dishes/{id}", saved.getId()));

    // Le GET /api/dishes ne doit plus retourner ce plat
    var response = exchange(restTestClient.get().uri("/api/dishes"));

    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .isEqualTo("[]");
  }
}