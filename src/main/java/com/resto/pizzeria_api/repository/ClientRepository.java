package com.resto.pizzeria_api.repository;

import com.resto.pizzeria_api.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour gérer les clients.
 * Étend JpaRepository pour fournir les opérations CRUD standard.
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {
}
