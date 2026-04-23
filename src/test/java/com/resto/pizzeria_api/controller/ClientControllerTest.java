package com.resto.pizzeria_api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.resto.pizzeria_api.model.Client;
import com.resto.pizzeria_api.repository.ClientRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@ExtendWith(AllureJunit5.class)

@Epic("Gestion des clients")
@Feature("API REST — Clients")
@Owner("resto-pizza-api")

@DisplayName("ClientController — tests d'intégration")
class ClientControllerTest {

  @Autowired private RestTestClient restTestClient;
  @Autowired private WebApplicationContext context;
  @Autowired private ClientRepository clientRepository;

  // -------------------------------------------------------------------------
  // Setup
  // -------------------------------------------------------------------------

  @BeforeEach
  void setUp() {
    clientRepository.deleteAll();
    restTestClient = RestTestClient
        .bindToApplicationContext(context)
        .build();
  }

  // -------------------------------------------------------------------------
  // Fixtures
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

  private RestTestClientResponse exchange(RestTestClient.RequestHeadersSpec<?> spec) {
    return RestTestClientResponse.from(spec.exchange());
  }

  // =========================================================================
  // GET /api/clients
  // =========================================================================

  @Test
  @Story("Lister tous les clients")
  @Severity(SeverityLevel.NORMAL)
  @Description("Vérifie que GET /api/clients retourne 200 avec un tableau vide quand la base est vide")
  @DisplayName("GET /api/clients — 200 tableau vide")
  void getAllClients_shouldReturn200WithEmptyArray() {
    Allure.step("Appel GET /api/clients sans données en base");
    var response = exchange(restTestClient.get().uri("/api/clients"));

    Allure.step("Vérification : statut 200 et body []");
    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .isEqualTo("[]");
  }

  @Test
  @Story("Lister tous les clients")
  @Severity(SeverityLevel.NORMAL)
  @Description("Vérifie que GET /api/clients retourne 200 avec la liste des clients persistés")
  @DisplayName("GET /api/clients — 200 avec clients en base")
  void getAllClients_shouldReturn200WithClients() {
    Allure.step("Persister deux clients en base");
    persistClient("Jean", "Dupont");
    persistClient("Marie", "Martin");

    Allure.step("Appel GET /api/clients");
    var response = exchange(restTestClient.get().uri("/api/clients"));

    Allure.step("Vérification : 2 clients retournés dans l'ordre");
    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].firstName").isEqualTo("Jean");

    assertThat(exchange(restTestClient.get().uri("/api/clients")))
        .bodyJson()
        .extractingPath("$[1].firstName").isEqualTo("Marie");
  }

  // =========================================================================
  // GET /api/clients/{id}
  // =========================================================================

  @Test
  @Story("Récupérer un client par ID")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Vérifie que GET /api/clients/{id} retourne 200 et le client correspondant")
  @DisplayName("GET /api/clients/{id} — 200 client trouvé")
  void getClientById_shouldReturn200WhenExists() {
    Allure.step("Persister un client en base");
    Client saved = persistClient("Jean", "Dupont");

    Allure.step("Appel GET /api/clients/" + saved.getId());
    var response = exchange(restTestClient.get().uri("/api/clients/{id}", saved.getId()));

    Allure.step("Vérification firstName et lastName");
    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.firstName").isEqualTo("Jean");

    assertThat(exchange(restTestClient.get().uri("/api/clients/{id}", saved.getId())))
        .bodyJson()
        .extractingPath("$.lastName").isEqualTo("Dupont");
  }

  @Test
  @Story("Récupérer un client par ID")
  @Severity(SeverityLevel.NORMAL)
  @Description("Vérifie que GET /api/clients/{id} retourne 404 et CODE_NOT_FOUND pour un ID inexistant")
  @DisplayName("GET /api/clients/{id} — 404 client inexistant")
  void getClientById_shouldReturn404WhenNotFound() {
    Allure.step("Appel GET /api/clients/9999 (ID inexistant)");
    var response = exchange(restTestClient.get().uri("/api/clients/{id}", 9999));

    Allure.step("Vérification : 404 + codeExtended CODE_NOT_FOUND");
    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  // =========================================================================
  // POST /api/clients
  // =========================================================================

  @Test
  @Story("Créer un client")
  @Severity(SeverityLevel.BLOCKER)
  @Description("Vérifie que POST /api/clients retourne 201 avec l'ID généré et les données du client créé")
  @DisplayName("POST /api/clients — 201 client créé")
  void createClient_shouldReturn201WhenValid() {
    Allure.step("Appel POST /api/clients avec un client valide");
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/clients")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildClient("Jean", "Dupont"))
            .exchange());

    Allure.step("Vérification : statut 201 et présence du champ id");
    assertThat(response)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .hasPath("$.id");

    Allure.step("Vérification : firstName retourné = Jean");
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
  @Story("Créer un client")
  @Severity(SeverityLevel.NORMAL)
  @Description("Vérifie que POST /api/clients retourne 400 + CODE_NOT_VALIDATED quand firstName est vide")
  @DisplayName("POST /api/clients — 400 firstName vide")
  void createClient_shouldReturn400WhenFirstNameBlank() {
    Allure.step("Appel POST avec firstName vide (\"\")");
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/clients")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildClient("", "Dupont"))
            .exchange());

    Allure.step("Vérification : 400 + CODE_NOT_VALIDATED");
    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_VALIDATED");
  }

  @Test
  @Story("Créer un client")
  @Severity(SeverityLevel.NORMAL)
  @Description("Vérifie que POST /api/clients retourne 400 + CODE_NOT_VALIDATED quand firstName est trop court")
  @DisplayName("POST /api/clients — 400 firstName trop court")
  void createClient_shouldReturn400WhenFirstNameTooShort() {
    Allure.step("Appel POST avec firstName = \"J\" (1 caractère)");
    var response = RestTestClientResponse.from(
        restTestClient.post()
            .uri("/api/clients")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildClient("J", "Dupont"))
            .exchange());

    Allure.step("Vérification : 400 + CODE_NOT_VALIDATED");
    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_VALIDATED");
  }

  // =========================================================================
  // PUT /api/clients/{id}
  // =========================================================================

  @Test
  @Story("Modifier un client")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Vérifie que PUT /api/clients/{id} met à jour les données et retourne 200")
  @DisplayName("PUT /api/clients/{id} — 200 client mis à jour")
  void updateClient_shouldReturn200WhenUpdated() {
    Allure.step("Persister un client initial (Jean Dupont)");
    Client saved = persistClient("Jean", "Dupont");

    Allure.step("Appel PUT avec nouvelles données (Pierre Martin)");
    var response = RestTestClientResponse.from(
        restTestClient.put()
            .uri("/api/clients/{id}", saved.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildClient("Pierre", "Martin"))
            .exchange());

    Allure.step("Vérification : 200 + firstName = Pierre");
    assertThat(response)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.firstName").isEqualTo("Pierre");
  }

  @Test
  @Story("Modifier un client")
  @Severity(SeverityLevel.NORMAL)
  @Description("Vérifie que PUT /api/clients/{id} retourne 404 pour un ID inexistant")
  @DisplayName("PUT /api/clients/{id} — 404 client inexistant")
  void updateClient_shouldReturn404WhenNotFound() {
    Allure.step("Appel PUT /api/clients/9999 (ID inexistant)");
    var response = RestTestClientResponse.from(
        restTestClient.put()
            .uri("/api/clients/{id}", 9999)
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildClient("Pierre", "Martin"))
            .exchange());

    Allure.step("Vérification : 404 + CODE_NOT_FOUND");
    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }

  // =========================================================================
  // DELETE /api/clients/{id}
  // =========================================================================

  @Test
  @Story("Supprimer un client")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Vérifie que DELETE /api/clients/{id} retourne 204 après suppression")
  @DisplayName("DELETE /api/clients/{id} — 204 client supprimé")
  void deleteClient_shouldReturn204WhenDeleted() {
    Allure.step("Persister un client à supprimer");
    Client saved = persistClient("Jean", "Dupont");

    Allure.step("Appel DELETE /api/clients/" + saved.getId());
    var response = exchange(restTestClient.delete().uri("/api/clients/{id}", saved.getId()));

    Allure.step("Vérification : statut 204 No Content");
    assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
  }

  @Test
  @Story("Supprimer un client")
  @Severity(SeverityLevel.NORMAL)
  @Description("Vérifie que DELETE /api/clients/{id} retourne 404 pour un ID inexistant")
  @DisplayName("DELETE /api/clients/{id} — 404 client inexistant")
  void deleteClient_shouldReturn404WhenNotFound() {
    Allure.step("Appel DELETE /api/clients/9999 (ID inexistant)");
    var response = exchange(restTestClient.delete().uri("/api/clients/{id}", 9999));

    Allure.step("Vérification : 404 + CODE_NOT_FOUND");
    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.codeExtended").isEqualTo("CODE_NOT_FOUND");
  }
}
