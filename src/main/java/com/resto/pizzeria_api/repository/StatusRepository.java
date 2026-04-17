package com.resto.pizzeria_api.repository;

import com.resto.pizzeria_api.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour gérer les statuts.
 * Étend JpaRepository pour fournir les opérations CRUD standard.
 */
@Repository
public interface StatusRepository
        extends JpaRepository<Status, Integer> {
}
