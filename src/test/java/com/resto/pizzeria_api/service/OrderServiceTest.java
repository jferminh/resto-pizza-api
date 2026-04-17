package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Client;
import com.resto.pizzeria_api.model.Order;
import com.resto.pizzeria_api.model.OrderItem;
import com.resto.pizzeria_api.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de OrderService.
 *
 * Points métier spécifiques couverts :
 * - getAllOrders() délègue à findAll() sans filtrage (pas de soft delete)
 * - getOrderById() utilise findById() standard
 * - deleteOrder() est un hard delete contrairement à DishService (soft delete)
 * - saveOrder() gère aussi bien la création (id=null) que la mise à jour
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService — tests unitaires")
class OrderServiceTest {

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private OrderService orderService;

  // -------------------------------------------------------------------------
  // Fixtures partagées
  // -------------------------------------------------------------------------

  /**
   * Construit une commande minimale valide avec un client et un item.
   * La relation item → order est configurée pour refléter le comportement
   * du contrôleur qui appelle item.setOrder(order) avant saveOrder().
   */
  private Order buildOrder(Integer id) {
    Client client = new Client();
    client.setId(1);
    client.setFirstName("Jean");
    client.setLastName("Dupont");

    Order order = new Order();
    order.setId(id);
    order.setDailyId(1);
    order.setCreationDate(LocalDateTime.now());
    order.setClient(client);
    order.setItems(new ArrayList<>());
    return order;
  }

  /**
   * Construit un OrderItem minimal et l'associe à la commande.
   * Reflète le comportement du contrôleur (item.setOrder(order)).
   */
  private OrderItem buildItem(Order order) {
    OrderItem item = new OrderItem();
    item.setId(1);
    item.setOrder(order);
    item.setQuantity(2);
    return item;
  }

  // =========================================================================
  // getAllOrders
  // =========================================================================

  @Nested
  @DisplayName("getAllOrders")
  class GetAllOrders {

    @Test
    @DisplayName("Doit retourner toutes les commandes sans filtrage")
    void shouldReturnAllOrders() {
      List<Order> orders = List.of(buildOrder(1), buildOrder(2));
      when(orderRepository.findAll()).thenReturn(orders);

      List<Order> result = orderService.getAllOrders();

      assertEquals(2, result.size());
      verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Doit retourner une liste vide si aucune commande")
    void shouldReturnEmptyListWhenNoOrders() {
      when(orderRepository.findAll()).thenReturn(List.of());

      List<Order> result = orderService.getAllOrders();

      assertTrue(result.isEmpty());
      verify(orderRepository).findAll();
    }
  }

  // =========================================================================
  // getOrderById
  // =========================================================================

  @Nested
  @DisplayName("getOrderById")
  class GetOrderById {

    @Test
    @DisplayName("Doit retourner la commande si l'ID existe")
    void shouldReturnOrderWhenIdExists() throws ApiNotFoundException {
      Order order = buildOrder(1);
      when(orderRepository.findById(1)).thenReturn(Optional.of(order));

      Order result = orderService.getOrderById(1);

      assertNotNull(result);
      assertEquals(1, result.getId());
      assertEquals(1, result.getDailyId());
      assertNotNull(result.getClient());
      verify(orderRepository).findById(1);
    }

    @Test
    @DisplayName("Doit retourner la commande avec ses items si présents")
    void shouldReturnOrderWithItems() throws ApiNotFoundException {
      Order order = buildOrder(1);
      order.getItems().add(buildItem(order));
      when(orderRepository.findById(1)).thenReturn(Optional.of(order));

      Order result = orderService.getOrderById(1);

      assertFalse(result.getItems().isEmpty());
      assertEquals(1, result.getItems().size());
      // Vérifie la relation bidirectionnelle item → order
      assertEquals(result, result.getItems().get(0).getOrder());
      verify(orderRepository).findById(1);
    }

    @Test
    @DisplayName("Doit lever ApiNotFoundException si l'ID n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenIdNotExists() {
      when(orderRepository.findById(999)).thenReturn(Optional.empty());

      ApiNotFoundException ex = assertThrows(
          ApiNotFoundException.class,
          () -> orderService.getOrderById(999)
      );

      assertNotNull(ex.getMessage());
      verify(orderRepository).findById(999);
    }
  }

  // =========================================================================
  // saveOrder
  // =========================================================================

  @Nested
  @DisplayName("saveOrder")
  class SaveOrder {

    @Test
    @DisplayName("Doit créer une commande et retourner l'objet avec son ID généré")
    void shouldCreateOrderAndReturnWithGeneratedId() {
      Order input = buildOrder(null);
      Order saved = buildOrder(10);
      when(orderRepository.save(input)).thenReturn(saved);

      Order result = orderService.saveOrder(input);

      assertNotNull(result.getId());
      assertEquals(10, result.getId());
      verify(orderRepository).save(input);
    }

    @Test
    @DisplayName("Doit mettre à jour une commande existante")
    void shouldUpdateExistingOrder() {
      Order existing = buildOrder(5);
      when(orderRepository.save(existing)).thenReturn(existing);

      Order result = orderService.saveOrder(existing);

      assertEquals(5, result.getId());
      verify(orderRepository).save(existing);
    }

    @Test
    @DisplayName("Doit persister la relation item → order lors de la sauvegarde")
    void shouldPersistItemOrderRelationship() {
      Order order = buildOrder(null);
      OrderItem item = buildItem(order);
      order.getItems().add(item);

      Order saved = buildOrder(7);
      saved.getItems().add(item);
      when(orderRepository.save(order)).thenReturn(saved);

      Order result = orderService.saveOrder(order);

      assertFalse(result.getItems().isEmpty());
      // La relation bidirectionnelle doit être maintenue
      assertNotNull(result.getItems().get(0).getOrder());
      verify(orderRepository).save(order);
    }
  }

  // =========================================================================
  // deleteOrder — hard delete
  // =========================================================================

  @Nested
  @DisplayName("deleteOrder — hard delete")
  class DeleteOrder {

    @Test
    @DisplayName("Doit supprimer la commande si l'ID existe")
    void shouldDeleteOrderWhenIdExists() throws ApiNotFoundException {
      when(orderRepository.existsById(1)).thenReturn(true);

      assertDoesNotThrow(() -> orderService.deleteOrder(1));

      verify(orderRepository).existsById(1);
      verify(orderRepository).deleteById(1);
    }

    @Test
    @DisplayName("Doit lever ApiNotFoundException si la commande n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenOrderNotFound() {
      when(orderRepository.existsById(999)).thenReturn(false);

      ApiNotFoundException ex = assertThrows(
          ApiNotFoundException.class,
          () -> orderService.deleteOrder(999)
      );

      assertNotNull(ex.getMessage());
      // deleteById ne doit jamais être appelé si la commande n'existe pas
      verify(orderRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("La suppression doit être un hard delete — save n'est jamais appelé")
    void shouldBeHardDeleteNotSoftDelete() throws ApiNotFoundException {
      // Contrairement à DishService, Order ne fait pas de soft delete
      when(orderRepository.existsById(2)).thenReturn(true);

      orderService.deleteOrder(2);

      // Garantit qu'il n'y a aucun soft delete (pas de save avec available=false)
      verify(orderRepository, never()).save(any());
      verify(orderRepository).deleteById(2);
    }
  }
}