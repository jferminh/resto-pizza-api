package com.resto.pizzeria_api.controller;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Client;
import com.resto.pizzeria_api.service.ClientService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller pour gérer les clients.
 * Fournit les opérations CRUD standard via /api/clients.
 */
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {
    private final ClientService clientService;

    /**
     * Retourne tous les clients.
     *
     * @return Liste des clients
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Client> getAllClients() {
        return clientService.getAllClients();
    }

    /**
     * Retourne un client par ID.
     *
     * @param id ID du client
     * @return Client trouvé
     * @throws ApiNotFoundException si non trouvé
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Client getClientById(
            @PathVariable final Integer id
    ) throws ApiNotFoundException {
        return clientService.getClientById(id);
    }

    /**
     * Crée un nouveau client.
     *
     * @param client Client à créer
     * @return Client créé
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Client createClient(@Valid @RequestBody final Client client) {
        client.setId(null);

        return clientService.saveClient(client);
    }

    /**
     * Met à jour un client existant.
     *
     * @param id            ID du client
     * @param updatedClient Nouvelles données
     * @return Client mis à jour
     * @throws ApiNotFoundException si non trouvé
     */
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Client updateClient(
            @PathVariable final Integer id,
            @Valid @RequestBody final Client updatedClient
    ) throws ApiNotFoundException {

        // 1. On vérifie que le client existe (sinon ça lève l'exception).
        final Client existing = clientService.getClientById(id);

        // 2. On met à jour uniquement les champs modifiables
        existing.setFirstName(updatedClient.getFirstName());
        existing.setLastName(updatedClient.getLastName());

        // 3. On sauvegarde les modifications
        return clientService.saveClient(existing);
    }

    /**
     * Supprime un client.
     *
     * @param id ID du client
     * @throws ApiNotFoundException si non trouvé
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClient(
            @PathVariable final Integer id
    ) throws ApiNotFoundException {
        clientService.deleteClient(id);
    }
}