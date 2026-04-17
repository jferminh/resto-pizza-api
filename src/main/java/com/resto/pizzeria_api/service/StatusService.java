package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Status;
import com.resto.pizzeria_api.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service pour gérer les statuts.
 */
@Service
@RequiredArgsConstructor
public class StatusService {
    private final StatusRepository statusRepository;

    /**
     * Récupère tous les statuts de la base de données.
     *
     * @return Tous les statuts trouvés
     */
    public List<Status> getAllStatuses() {
        return statusRepository.findAll();
    }

    /**
     * Récupère un statut par son identifiant.
     *
     * @param id L'identifiant du statut
     * @return Le statut trouvé
     * @throws ApiNotFoundException Si aucun statut ne correspond à l'ID
     */
    public Status getStatusById(
            final Integer id
    ) throws ApiNotFoundException {
        return statusRepository.findById(id)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Statut n'a pas été trouvé"));
    }
}
