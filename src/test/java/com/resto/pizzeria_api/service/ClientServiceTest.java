package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Client;
import com.resto.pizzeria_api.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService — tests unitaires")
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    // -------------------------------------------------------------------------
    // Fixture partagée
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
        @DisplayName("Doit retourner la liste complète des clients")
        void shouldReturnAllClients() {
            List<Client> clients = List.of(
                    buildClient(1, "Jean", "Dupont"),
                    buildClient(2, "Marie", "Martin")
            );
            when(clientRepository.findAll()).thenReturn(clients);

            List<Client> result = clientService.getAllClients();

            assertEquals(2, result.size());
            verify(clientRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Doit retourner une liste vide si aucun client")
        void shouldReturnEmptyListWhenNoClients() {
            when(clientRepository.findAll()).thenReturn(List.of());

            List<Client> result = clientService.getAllClients();

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
        @DisplayName("Doit retourner le client si l'ID existe")
        void shouldReturnClientWhenIdExists() throws ApiNotFoundException {
            Client client = buildClient(1, "Jean", "Dupont");
            when(clientRepository.findById(1)).thenReturn(Optional.of(client));

            Client result = clientService.getClientById(1);

            assertNotNull(result);
            assertEquals(1, result.getId());
            assertEquals("Jean", result.getFirstName());
            verify(clientRepository).findById(1);
        }

        @Test
        @DisplayName("Doit lever ApiNotFoundException si l'ID n'existe pas")
        void shouldThrowApiNotFoundExceptionWhenIdNotExists() {
            when(clientRepository.findById(999)).thenReturn(Optional.empty());

            ApiNotFoundException ex = assertThrows(
                    ApiNotFoundException.class,
                    () -> clientService.getClientById(999)
            );

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
        @DisplayName("Doit sauvegarder et retourner le client avec son ID généré")
        void shouldSaveAndReturnClient() {
            Client input = buildClient(null, "Jean", "Dupont");
            Client saved = buildClient(1, "Jean", "Dupont");
            when(clientRepository.save(input)).thenReturn(saved);

            Client result = clientService.saveClient(input);

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
        @DisplayName("Doit supprimer le client si l'ID existe")
        void shouldDeleteClientWhenIdExists() throws ApiNotFoundException {
            when(clientRepository.existsById(1)).thenReturn(true);

            assertDoesNotThrow(() -> clientService.deleteClient(1));

            verify(clientRepository).existsById(1);
            verify(clientRepository).deleteById(1);
        }

        @Test
        @DisplayName("Doit lever ApiNotFoundException si le client n'existe pas")
        void shouldThrowApiNotFoundExceptionWhenClientNotFound() {
            when(clientRepository.existsById(999)).thenReturn(false);

            ApiNotFoundException ex = assertThrows(
                    ApiNotFoundException.class,
                    () -> clientService.deleteClient(999)
            );

            assertTrue(ex.getMessage().contains("999"));
            verify(clientRepository, never()).deleteById(any());
        }
    }
}