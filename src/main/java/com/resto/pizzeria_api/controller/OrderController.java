package com.resto.pizzeria_api.controller;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Order;
import com.resto.pizzeria_api.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller pour gérer les commandes.
 * Fournit les opérations CRUD standard via /api/orders.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    /**
     * Retourne toutes les commandes.
     *
     * @return Liste des commandes
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    /**
     * Retourne une commande par ID.
     *
     * @param id ID de la commande
     * @return Commande trouvée
     * @throws ApiNotFoundException si non trouvé
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Order getOrderById(
            @PathVariable final Integer id
    ) throws ApiNotFoundException {
        return orderService.getOrderById(id);
    }

    /**
     * Crée une nouvelle commande.
     *
     * @param order Commande à créer
     * @return Commande créée
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestBody final Order order) {
        order.setId(null);

        order.setCreationDate(LocalDateTime.now());

        if (order.getItems() != null) {
            order.getItems().forEach(item -> item.setOrder(order));
        }

        return orderService.saveOrder(order);
    }

    /**
     * Met à jour une commande existant.
     *
     * @param id           ID de la commande
     * @param updatedOrder Nouvelles données
     * @return Commande mise à jour
     * @throws ApiNotFoundException si non trouvé
     */
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Order updateOrder(
            @PathVariable final Integer id,
            @RequestBody final Order updatedOrder
    ) throws ApiNotFoundException {
        final Order existing = orderService.getOrderById(id);

        existing.setDailyId(updatedOrder.getDailyId());
        existing.setStatus(updatedOrder.getStatus());
        existing.setClient(updatedOrder.getClient());

        existing.getItems().clear();

        if (updatedOrder.getItems() != null) {
            updatedOrder.getItems().forEach(item -> {
                item.setOrder(existing);
                existing.getItems().add(item);
            });
        }

        return orderService.saveOrder(existing);
    }

    /**
     * Supprime une commande.
     *
     * @param id ID de la commande
     * @throws ApiNotFoundException si non trouvé
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(
            @PathVariable final Integer id
    ) throws ApiNotFoundException {
        orderService.deleteOrder(id);
    }
}