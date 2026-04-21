package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Order;
import com.resto.pizzeria_api.model.OrderItem;
import com.resto.pizzeria_api.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void deleteOrderItem(final Integer id) throws ApiNotFoundException {
      OrderItem item = orderItemRepository.findById(id)
          .orElseThrow(() -> new ApiNotFoundException(
              "Article de commande n'a pas été trouvé"));
      // ✅ On retire l'item de la collection du parent
      // orphanRemoval=true sur Order.items garantit la suppression en BDD
      Order order = item.getOrder();
      order.getItems().remove(item);

      // ✅ La session reste ouverte (@Transactional) → suppression committée
      // Pas besoin d'appeler deleteById — orphanRemoval s'en charge
      //  orderItemRepository.deleteById(id);
    }
}
