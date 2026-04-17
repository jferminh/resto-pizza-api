package com.resto.pizzeria_api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.resto.pizzeria_api.model.Client;
import com.resto.pizzeria_api.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@DisplayName("ClientController — tests d'intégration")
class ClientControllerTest {

  @Autowired
  private RestTestClient restTestClient;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ClientRepository clientRepository;

  @BeforeEach
  void setUp() {
    clientRepository.deleteAll();
    restTestClient = RestTestClient
        .bindToApplicationContext(context)
        .build();
  }

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------

  private Client buildClient(String firstName, String lastName) {
    Client c = new Client();
    c.setFirstName(firstName);
    c.setLastName(lastName);
    return c;
  }

  private Client persistClient(String firstName, String lastName) {
    return clientRepository.save(buildClient(firstName, lastName));
  }

  // Méthode utilitaire : transforme un ResponseSpec en RestTestClientResponse AssertJ
  private RestTestClientResponse exchange(RestTestClient.RequestHeadersSpec<?> spec) {
    return RestTestClientResponse.from(spec.exchange());
  }

  // =========================================================================
  // GET /api/clients
  // =========================================================================

  @Test
  @DisplayName("GET /api/clients — 200 tableau vide")
  void getAllClients_shouldReturn200WithEmptyArray() {
    var response = exchange(restTestClient.get().uri("/api/clients"));

    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .isEqualTo("[]");
  }

  @Test
  @DisplayName("GET /api/clients — 200 avec clients en base")
  void getAllClients_shouldReturn200WithClients() {
    persistClient("Jean", "Dupont");
    persistClient("Marie", "Martin");

    var response = exchange(restTestClient.get().uri("/api/clients"));

    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].firstName").isEqualTo("Jean"); // ✅ extractingPath, pas hasPath

    assertThat(RestTestClientResponse.from(
        restTestClient.get().uri("/api/clients").exchange()))
        .bodyJson()
        .extractingPath("$[1].firstName").isEqualTo("Marie");
  }

  // =========================================================================
  // GET /api/clients/{id}
  // =========================================================================

  @Test
  @DisplayName("GET /api/clients/{id} — 200 client trouvé")
  void getClientById_shouldReturn200WhenExists() {
    Client saved = persistClient("Jean", "Dupont");

    var response = exchange(restTestClient.get().uri("/api/clients/{id}", saved.getId()));

    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.firstName").isEqualTo("Jean");

    assertThat(RestTestClientResponse.from(
        restTestClient.get().uri("/api/clients/{id}", saved.getId()).exchange()))
        .bodyJson()
        .extractingPath("$.lastName").isEqualTo("Dupont");
  }

  @Test
  @DisplayName("GET /api/clients/{id} — 404 client inexistant")
  void getClientById_shouldReturn404WhenNotFound() {
    var response = exchange(restTestClient.get().uri("/api/clients/{id}", 9999));

    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  // =========================================================================
  // POST /api/clients
  // =========================================================================

  @Test
  @DisplayName("POST /api/clients — 201 client créé")
  void createClient_shouldReturn201WhenValid() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/clients")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildClient("Jean", "Dupont"))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .hasPath("$.id");                     // ✅ hasPath() seul pour vérifier l'existence

    assertThat(RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/clients")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildClient("Jean", "Dupont"))
            .exchange()))
        .bodyJson()
        .extractingPath("$.firstName").isEqualTo("Jean");
  }

  @Test
  @DisplayName("POST /api/clients — 400 firstName vide")
  void createClient_shouldReturn400WhenFirstNameBlank() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/clients")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildClient("", "Dupont"))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_VALIDATED");
  }

  @Test
  @DisplayName("POST /api/clients — 400 firstName trop court")
  void createClient_shouldReturn400WhenFirstNameTooShort() {
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/clients")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildClient("J", "Dupont"))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_VALIDATED");
  }

  // =========================================================================
  // PUT /api/clients/{id}
  // =========================================================================

  @Test
  @DisplayName("PUT /api/clients/{id} — 200 client mis à jour")
  void updateClient_shouldReturn200WhenUpdated() {
    Client saved = persistClient("Jean", "Dupont");

    var response = RestTestClientResponse.from(
        restTestClient.put()
            .uri("/api/clients/{id}", saved.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildClient("Pierre", "Martin"))
            .exchange());

    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.firstName").isEqualTo("Pierre");
  }

  @Test
  @DisplayName("PUT /api/clients/{id} — 404 client inexistant")
  void updateClient_shouldReturn404WhenNotFound() {
    var response = RestTestClientResponse.from(
        restTestClient.put()
            .uri("/api/clients/{id}", 9999)
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildClient("Pierre", "Martin"))
            .exchange());

    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  // =========================================================================
  // DELETE /api/clients/{id}
  // =========================================================================

  @Test
  @DisplayName("DELETE /api/clients/{id} — 204 client supprimé")
  void deleteClient_shouldReturn204WhenDeleted() {
    Client saved = persistClient("Jean", "Dupont");

    var response = exchange(restTestClient.delete().uri("/api/clients/{id}", saved.getId()));

    assertThat(response)
        .hasStatus(HttpStatus.NO_CONTENT);
  }

  @Test
  @DisplayName("DELETE /api/clients/{id} — 404 client inexistant")
  void deleteClient_shouldReturn404WhenNotFound() {
    var response = exchange(restTestClient.delete().uri("/api/clients/{id}", 9999));

    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }
}