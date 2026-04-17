package com.resto.pizzeria_api.repository;

import com.resto.pizzeria_api.model.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour gérer les plats.
 * Étend JpaRepository pour fournir les opérations CRUD standard.
 */
@Repository
public interface DishRepository extends JpaRepository<Dish, Integer> {
    /**
     * Trouve les plats que ne sont pas archivé.
     */
    List<Dish> findByAvailableTrue();

    /**
     * Trouve le plat qui si n'est pas archivé.
     */
    Optional<Dish> findByIdAndAvailableTrue(final Integer id);
}
