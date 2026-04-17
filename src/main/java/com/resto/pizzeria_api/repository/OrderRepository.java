package com.resto.pizzeria_api.repository;

import com.resto.pizzeria_api.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour gérer les commandes.
 * Étend JpaRepository pour fournir les opérations CRUD standard.
 */
@Repository
public interface OrderRepository
        extends JpaRepository<Order, Integer> {
}
