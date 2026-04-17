package com.resto.pizzeria_api.controller;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Dish;
import com.resto.pizzeria_api.service.DishService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller pour gérer les plats.
 * Fournit les opérations CRUD standard via /api/dishes.
 */
@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {
    private final DishService dishService;

    /**
     * Retourne tous les plats.
     *
     * @return Liste des plats
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Dish> getAllDishes() {
        return dishService.getAllDishes();
    }

    /**
     * Retourne un plat par ID.
     *
     * @param id ID du plat
     * @return Plat trouvé
     * @throws ApiNotFoundException si non trouvé
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Dish getDishById(
            @PathVariable final Integer id
    ) throws ApiNotFoundException {
        return dishService.getDishById(id);
    }

    /**
     * Crée un nouveau plat.
     *
     * @param dish plat à créer
     * @return Plat créé
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dish createDish(@Valid @RequestBody final Dish dish) {
        dish.setId(null);
        dish.setAvailable(true);
        return dishService.saveDish(dish);
    }

    /**
     * Met à jour un plat existant.
     *
     * @param id      ID du plat
     * @param updated Nouvelles données
     * @return Plat mis à jour
     * @throws ApiNotFoundException si non trouvé
     */
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Dish updateDish(
            @PathVariable final Integer id,
            @Valid @RequestBody final Dish updated
    ) throws ApiNotFoundException {
        final Dish existing = dishService.getDishById(id);

        existing.setName(updated.getName());
        existing.setPrice(updated.getPrice());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());

        return dishService.saveDish(existing);
    }

    /**
     * Supprime un plat.
     *
     * @param id ID du plat
     * @throws ApiNotFoundException si non trouvé
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDish(
            @PathVariable final Integer id
    ) throws ApiNotFoundException {
        dishService.deleteDish(id);
    }
}
