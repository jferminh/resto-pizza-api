package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Client;
import com.resto.pizzeria_api.repository.ClientRepository;
import io.qameta.allure.*;
import io.qameta.allure.junit5.AllureJunit5;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de ClientService.
 * Isole complètement le service via Mockito — aucun accès base de données.
 */
@ExtendWith({MockitoExtension.class, AllureJunit5.class})

@Epic("Gestion des clients")
@Feature("Service — Clients")
@Owner("resto-pizza-api")

@DisplayName("ClientService — tests unitaires")
class ClientServiceTest {

  @Mock
  private ClientRepository clientRepository;

  @InjectMocks
  private ClientService clientService;

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------

  private Client buildClient(Integer id, String firstName, String lastName) {
    Client c = new Client();
    c.setId(id);
    c.setFirstName(firstName);
    c.setLastName(lastName);
    return c;
  }

  // =========================================================================
  // getAllClients
  // =========================================================================

  @Nested
  @DisplayName("getAllClients")
  class GetAllClients {

    @Test
    @Story("Lister tous les clients")
    @Severity(SeverityLevel.NORMAL)
    @Description("Le repository retourne une liste — le service la propage sans modification")
    @DisplayName("Doit retourner la liste complète des clients")
    void shouldReturnAllClients() {
      List<Client> clients = List.of(
          buildClient(1, "Jean", "Dupont"),
          buildClient(2, "Marie", "Martin")
      );

      Allure.step("Mock : clientRepository.findAll() retourne 2 clients");
      when(clientRepository.findAll()).thenReturn(clients);

      Allure.step("Appel clientService.getAllClients()");
      List<Client> result = clientService.getAllClients();

      Allure.step("Vérification : liste de 2 éléments + appel repository vérifié");
      assertEquals(2, result.size());
      verify(clientRepository, times(1)).findAll();
    }

    @Test
    @Story("Lister tous les clients")
    @Severity(SeverityLevel.MINOR)
    @Description("Quand la base est vide, le service retourne une liste vide sans exception")
    @DisplayName("Doit retourner une liste vide si aucun client")
    void shouldReturnEmptyListWhenNoClients() {
      Allure.step("Mock : clientRepository.findAll() retourne []");
      when(clientRepository.findAll()).thenReturn(List.of());

      Allure.step("Appel clientService.getAllClients()");
      List<Client> result = clientService.getAllClients();

      Allure.step("Vérification : résultat vide + findAll appelé une fois");
      assertTrue(result.isEmpty());
      verify(clientRepository, times(1)).findAll();
    }
  }

  // =========================================================================
  // getClientById
  // =========================================================================

  @Nested
  @DisplayName("getClientById")
  class GetClientById {

    @Test
    @Story("Récupérer un client par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("findById retourne un Optional<Client> — le service le désemballe et retourne le client")
    @DisplayName("Doit retourner le client si l'ID existe")
    void shouldReturnClientWhenIdExists() throws ApiNotFoundException {
      Client client = buildClient(1, "Jean", "Dupont");

      Allure.step("Mock : findById(1) retourne Optional.of(client)");
      when(clientRepository.findById(1)).thenReturn(Optional.of(client));

      Allure.step("Appel clientService.getClientById(1)");
      Client result = clientService.getClientById(1);

      Allure.step("Vérification : client non null, id=1, firstName=Jean");
      assertNotNull(result);
      assertEquals(1, result.getId());
      assertEquals("Jean", result.getFirstName());
      verify(clientRepository).findById(1);
    }

    @Test
    @Story("Récupérer un client par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("findById retourne Optional.empty() — le service doit lever ApiNotFoundException avec l'ID dans le message")
    @DisplayName("Doit lever ApiNotFoundException si l'ID n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenIdNotExists() {
      Allure.step("Mock : findById(999) retourne Optional.empty()");
      when(clientRepository.findById(999)).thenReturn(Optional.empty());

      Allure.step("Appel clientService.getClientById(999) — ApiNotFoundException attendue");
      ApiNotFoundException ex = assertThrows(
          ApiNotFoundException.class,
          () -> clientService.getClientById(999)
      );

      Allure.step("Vérification : message contient \"999\" + findById appelé");
      assertTrue(ex.getMessage().contains("999"));
      verify(clientRepository).findById(999);
    }
  }

  // =========================================================================
  // saveClient
  // =========================================================================

  @Nested
  @DisplayName("saveClient")
  class SaveClient {

    @Test
    @Story("Créer un client")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Le service délègue au repository.save() et retourne l'entité avec l'ID généré par la base")
    @DisplayName("Doit sauvegarder et retourner le client avec son ID généré")
    void shouldSaveAndReturnClient() {
      Client input  = buildClient(null, "Jean", "Dupont");
      Client saved  = buildClient(1,    "Jean", "Dupont");

      Allure.step("Mock : repository.save(input) retourne le client avec id=1");
      when(clientRepository.save(input)).thenReturn(saved);

      Allure.step("Appel clientService.saveClient(input)");
      Client result = clientService.saveClient(input);

      Allure.step("Vérification : id non null = 1");
      assertNotNull(result.getId());
      assertEquals(1, result.getId());
      verify(clientRepository).save(input);
    }
  }

  // =========================================================================
  // deleteClient
  // =========================================================================

  @Nested
  @DisplayName("deleteClient")
  class DeleteClient {

    @Test
    @Story("Supprimer un client")
    @Severity(SeverityLevel.CRITICAL)
    @Description("existsById retourne true — le service appelle deleteById sans lever d'exception")
    @DisplayName("Doit supprimer le client si l'ID existe")
    void shouldDeleteClientWhenIdExists() throws ApiNotFoundException {
      Allure.step("Mock : existsById(1) retourne true");
      when(clientRepository.existsById(1)).thenReturn(true);

      Allure.step("Appel clientService.deleteClient(1) — aucune exception attendue");
      assertDoesNotThrow(() -> clientService.deleteClient(1));

      Allure.step("Vérification : existsById + deleteById appelés une fois");
      verify(clientRepository).existsById(1);
      verify(clientRepository).deleteById(1);
    }

    @Test
    @Story("Supprimer un client")
    @Severity(SeverityLevel.CRITICAL)
    @Description("existsById retourne false — le service lève ApiNotFoundException sans appeler deleteById")
    @DisplayName("Doit lever ApiNotFoundException si le client n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenClientNotFound() {
      Allure.step("Mock : existsById(999) retourne false");
      when(clientRepository.existsById(999)).thenReturn(false);

      Allure.step("Appel clientService.deleteClient(999) — ApiNotFoundException attendue");
      ApiNotFoundException ex = assertThrows(
          ApiNotFoundException.class,
          () -> clientService.deleteClient(999)
      );

      Allure.step("Vérification : message contient \"999\" + deleteById jamais appelé");
      assertTrue(ex.getMessage().contains("999"));
      verify(clientRepository, never()).deleteById(any());
    }
  }
}
