package com.resto.pizzeria_api.controller;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.OrderItem;
import com.resto.pizzeria_api.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller pour gérer les articles de commande.
 * Fournit les opérations CRUD standard via /api/order-items.
 */
@RestController
@RequestMapping("/api/order-items")
@RequiredArgsConstructor
public class OrderItemController {
    private final OrderItemService orderItemService;

    /**
     * Retourne tous les articles de commande.
     *
     * @return Liste des articles de commande
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<OrderItem> getAllOrderItems() {
        return orderItemService.getAllOrderItems();
    }

    /**
     * Retourne un article de commande par ID.
     *
     * @param id ID de l'article de commande
     * @return Article de commande trouvé
     * @throws ApiNotFoundException si non trouvé
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderItem getOrderItemById(
            @PathVariable final Integer id
    ) throws ApiNotFoundException {
        return orderItemService.getOrderItemById(id);
    }

    /**
     * Crée un nouvel article de commande.
     *
     * @param item Article de commande à créer
     * @return Article de commande créé
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItem createOrderItem(@RequestBody OrderItem item) {
        item.setId(null);

        return orderItemService.saveOrderItem(item);
    }

    /**
     * Met à jour un article de commande existant.
     *
     * @param id      ID de l'article de commande
     * @param updated Nouvelles données
     * @return Article de commande mis à jour
     * @throws ApiNotFoundException si non trouvé
     */
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderItem updateOrderItem(
            @PathVariable final Integer id,
            @RequestBody final OrderItem updated
    ) throws ApiNotFoundException {
        final OrderItem existing = orderItemService.getOrderItemById(id);

        existing.setQuantity(updated.getQuantity());
        existing.setDish(updated.getDish());

        return orderItemService.saveOrderItem(existing);
    }

    /**
     * Supprime un article de commande.
     *
     * @param id ID de l'article de commande
     * @throws ApiNotFoundException si non trouvé
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Integer id
    ) throws ApiNotFoundException {
        orderItemService.deleteOrderItem(id);
    }
}