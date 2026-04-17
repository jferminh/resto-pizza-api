package com.resto.pizzeria_api.service;

import lombok.RequiredArgsConstructor;
import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Client;
import com.resto.pizzeria_api.repository.ClientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service pour gérer les clients.
 */
@Service
@RequiredArgsConstructor
public class ClientService {

    // Injection du repository par constructeur grâce à Lombok
    private final ClientRepository clientRepository;

    /**
     * Récupère tous les clients de la base de données.
     *
     * @return Tous les clients trouvés
     */
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    /**
     * Récupère un client par son identifiant.
     *
     * @param id L'identifiant du client
     * @return Le client trouvé
     * @throws ApiNotFoundException Si aucun client ne correspond à l'ID
     */
    public Client getClientById(final Integer id) throws ApiNotFoundException {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Le client avec l'ID " + id + " n'a pas été trouvé."));
    }

    /**
     * Sauvegarde un nouveau client ou met à jour un client existant.
     *
     * @param client L'objet client à sauvegarder
     * @return Le client sauvegardé (avec son ID généré)
     */
    public Client saveClient(final Client client) {
        return clientRepository.save(client);
    }

    /**
     * Supprime un client par son identifiant.
     *
     * @param id L'identifiant du client à supprimer
     * @throws ApiNotFoundException Si le client n'existe pas
     *                              avant la suppression
     */
    public void deleteClient(final Integer id) throws ApiNotFoundException {
        if (!clientRepository.existsById(id)) {
            throw new ApiNotFoundException(
                    "Impossible de supprimer : " +
                            "le client avec l'ID " + id + " n'existe pas.");
        }

        clientRepository.deleteById(id);
    }
}
