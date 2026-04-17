package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Dish;
import com.resto.pizzeria_api.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service pour gérer les plats.
 */
@Service
@RequiredArgsConstructor
public class DishService {
    private final DishRepository dishRepository;

    /**
     * Récupère tous les plats de la base de données.
     *
     * @return Tous les plats trouvés
     */
    public List<Dish> getAllDishes() {
        return dishRepository.findByAvailableTrue();
    }

    /**
     * Récupère un plat par son identifiant.
     *
     * @param id L'identifiant du plat
     * @return Le plat trouvé
     * @throws ApiNotFoundException Si aucun plat ne correspond à l'ID
     */
    public Dish getDishById(final Integer id) throws ApiNotFoundException {
        return dishRepository.findByIdAndAvailableTrue(id)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Le plat avec l'ID " + id + " n'a pas été trouvé."
                ));
    }

    /**
     * Sauvegarde un nouveau plat ou met à jour un plat existant.
     *
     * @param dish L'objet plat à sauvegarder
     * @return Le plat sauvegardé (avec son ID généré)
     */
    public Dish saveDish(final Dish dish) {
        return dishRepository.save(dish);
    }

    /**
     * Supprime un plat par son identifiant.
     *
     * @param id L'identifiant du plat à supprimer
     * @throws ApiNotFoundException Si le plat n'existe pas
     *                              avant la suppression
     */
    public void deleteDish(final Integer id) throws ApiNotFoundException {
        Dish dish = dishRepository.findByIdAndAvailableTrue(id)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Dish n'a pas été trouvé avec ID : " + id
                ));

        dish.setAvailable(false);
        dishRepository.save(dish);
    }
}
