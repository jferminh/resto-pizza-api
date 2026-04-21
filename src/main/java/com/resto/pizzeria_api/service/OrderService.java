package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Order;
import com.resto.pizzeria_api.model.OrderItem;
import com.resto.pizzeria_api.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service pour gérer les commandes.
 */
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    /**
     * Récupère toutes les commandes de la base de données.
     *
     * @return Toutes les commandes trouvées
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Récupère une commande par son identifiant.
     *
     * @param id L'identifiant de la commande
     * @return La commande trouvée
     * @throws ApiNotFoundException Si aucune commande ne correspond à l'ID
     */
    @Transactional(readOnly = true)
    public Order getOrderById(
            final Integer id
    ) throws ApiNotFoundException {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Commande n'a pas été trouvée"));
    }

    /**
     * Sauvegarde une nouvelle commande ou met à jour une commande existant.
     *
     * @param order L'objet commande à sauvegarder
     * @return La commande sauvegardée (avec son ID généré)
     */
    @Transactional
    public Order saveOrder(final Order order) {
        return orderRepository.save(order);
    }

  @Transactional
  public Order updateOrder(final Integer id, final Order updatedOrder)
      throws ApiNotFoundException {

    // findById dans la même transaction → session active
    final Order existing = orderRepository.findById(id)
        .orElseThrow(() -> new ApiNotFoundException(
            "Commande n'a pas été trouvée"));

    // Mise à jour des champs simples
    existing.setDailyId(updatedOrder.getDailyId());
    existing.setStatus(updatedOrder.getStatus());
    existing.setClient(updatedOrder.getClient());

    // ✅ Manipulation des items dans la transaction → session active → OK
    existing.getItems().clear();

    if (updatedOrder.getItems() != null) {
      for (OrderItem item : updatedOrder.getItems()) {
        item.setOrder(existing);
        existing.getItems().add(item);
      }
    }

    // save() déclenche la persistance dans la même transaction
    return orderRepository.save(existing);
  }

    /**
     * Supprime une commande par son identifiant.
     *
     * @param id L'identifiant de la commande à supprimer
     * @throws ApiNotFoundException Si la commande n'existe pas
     *                              avant la suppression
     */
    @Transactional
    public void deleteOrder(final Integer id) throws ApiNotFoundException {
        if (!orderRepository.existsById(id)) {
            throw new ApiNotFoundException(
                    "Commande n'a pas été trouvée");
        }

        orderRepository.deleteById(id);
    }
}
