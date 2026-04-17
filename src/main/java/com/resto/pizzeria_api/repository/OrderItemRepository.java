package com.resto.pizzeria_api.repository;

import com.resto.pizzeria_api.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour gérer les articles des commandes.
 * Étend JpaRepository pour fournir les opérations CRUD standard.
 */
@Repository
public interface OrderItemRepository
        extends JpaRepository<OrderItem, Integer> {
}
