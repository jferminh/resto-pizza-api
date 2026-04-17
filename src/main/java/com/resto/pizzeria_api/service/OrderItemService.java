package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.OrderItem;
import com.resto.pizzeria_api.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service pour gérer les articles de commande.
 */
@Service
@RequiredArgsConstructor
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;

    /**
     * Récupère tous les articles de commande de la base de données.
     *
     * @return Tous les articles trouvés
     */
    public List<OrderItem> getAllOrderItems() {
        return orderItemRepository.findAll();
    }

    /**
     * Récupère un article de commande par son identifiant.
     *
     * @param id L'identifiant de l'article de commande
     * @return L'article de commande trouvé
     * @throws ApiNotFoundException Si aucun article de commande
     *                              ne correspond à l'ID
     */
    public OrderItem getOrderItemById(
            final Integer id
    ) throws ApiNotFoundException {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Article de commande n'a pas été trouvé"));
    }

    /**
     * Sauvegarde un nouvel article de commande
     * ou met à jour un article de commande existant.
     *
     * @param orderItem L'objet article de commande à sauvegarder
     * @return L'article de commande sauvegardé (avec son ID généré)
     */
    public OrderItem saveOrderItem(final OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }

    /**
     * Supprime un article de commande par son identifiant.
     *
     * @param id L'identifiant de l'article de commande à supprimer
     * @throws ApiNotFoundException Si l'article de commande n'existe pas
     *                              avant la suppression
     */
    public void deleteOrderItem(final Integer id) throws ApiNotFoundException {
        if (!orderItemRepository.existsById(id)) {
            throw new ApiNotFoundException(
                    "Article de commande n'a pas été trouvé");
        }

        orderItemRepository.deleteById(id);
    }
}
