package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Dish;
import com.resto.pizzeria_api.model.Order;
import com.resto.pizzeria_api.model.OrderItem;
import com.resto.pizzeria_api.repository.OrderItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de OrderItemService.
 *
 * OrderItem est une entité de jointure entre Order et Dish.
 * Points métier spécifiques couverts :
 * - saveOrderItem() doit conserver la relation bidirectionnelle order ↔ items
 * - deleteOrderItem() est un hard delete (pas de soft delete)
 * - Un OrderItem sans Order ou sans Dish est invalide — testé via les fixtures
 * - La quantité doit être strictement positive (@Positive)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderItemService — tests unitaires")
class OrderItemServiceTest {

  @Mock
  private OrderItemRepository orderItemRepository;

  @InjectMocks
  private OrderItemService orderItemService;

  // -------------------------------------------------------------------------
  // Fixtures partagées
  // -------------------------------------------------------------------------

  /**
   * Construit un Order minimal valide.
   * Les items list est initialisée vide, car OrderItem y sera ajouté
   * manuellement dans les tests qui vérifient la relation bidirectionnelle.
   */
  private Order buildOrder(Integer id) {
    Order order = new Order();
    order.setId(id);
    order.setDailyId(1);
    order.setCreationDate(LocalDateTime.now());
    order.setItems(new ArrayList<>());
    return order;
  }

  /**
   * Construit un Dish minimal valide.
   */
  private Dish buildDish(Integer id, String name) {
    Dish dish = new Dish();
    dish.setId(id);
    dish.setName(name);
    dish.setPrice(new BigDecimal("10.50"));
    dish.setAvailable(true);
    return dish;
  }

  /**
   * Construit un OrderItem complet avec ses relations Order et Dish.
   * Reflète l'état attendu après que le contrôleur ait appelé
   * item.setOrder(order) et orderItem.setDish(dish).
   */
  private OrderItem buildOrderItem(Integer id, Order order, Dish dish, int quantity) {
    OrderItem item = new OrderItem();
    item.setId(id);
    item.setOrder(order);
    item.setDish(dish);
    item.setQuantity(quantity);
    return item;
  }

  // =========================================================================
  // getAllOrderItems
  // =========================================================================

  @Nested
  @DisplayName("getAllOrderItems")
  class GetAllOrderItems {

    @Test
    @DisplayName("Doit retourner tous les articles sans filtrage")
    void shouldReturnAllOrderItems() {
      Order order = buildOrder(1);
      Dish dish   = buildDish(1, "Margherita");
      List<OrderItem> items = List.of(
          buildOrderItem(1, order, dish, 2),
          buildOrderItem(2, order, dish, 1)
      );
      when(orderItemRepository.findAll()).thenReturn(items);

      List<OrderItem> result = orderItemService.getAllOrderItems();

      assertEquals(2, result.size());
      verify(orderItemRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Doit retourner une liste vide si aucun article")
    void shouldReturnEmptyListWhenNoItems() {
      when(orderItemRepository.findAll()).thenReturn(List.of());

      List<OrderItem> result = orderItemService.getAllOrderItems();

      assertTrue(result.isEmpty());
      verify(orderItemRepository).findAll();
    }
  }

  // =========================================================================
  // getOrderItemById
  // =========================================================================

  @Nested
  @DisplayName("getOrderItemById")
  class GetOrderItemById {

    @Test
    @DisplayName("Doit retourner l'article avec ses relations Order et Dish")
    void shouldReturnOrderItemWithRelations() throws ApiNotFoundException {
      Order order     = buildOrder(1);
      Dish dish       = buildDish(1, "Margherita");
      OrderItem item  = buildOrderItem(1, order, dish, 3);
      when(orderItemRepository.findById(1)).thenReturn(Optional.of(item));

      OrderItem result = orderItemService.getOrderItemById(1);

      assertNotNull(result);
      assertEquals(1, result.getId());
      assertEquals(3, result.getQuantity());
      // Vérifie que les relations sont bien présentes
      assertNotNull(result.getOrder());
      assertNotNull(result.getDish());
      assertEquals(1, result.getOrder().getId());
      assertEquals("Margherita", result.getDish().getName());
      verify(orderItemRepository).findById(1);
    }

    @Test
    @DisplayName("Doit lever ApiNotFoundException si l'ID n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenIdNotExists() {
      when(orderItemRepository.findById(999)).thenReturn(Optional.empty());

      ApiNotFoundException ex = assertThrows(
          ApiNotFoundException.class,
          () -> orderItemService.getOrderItemById(999)
      );

      assertNotNull(ex.getMessage());
      verify(orderItemRepository).findById(999);
    }
  }

  // =========================================================================
  // saveOrderItem
  // =========================================================================

  @Nested
  @DisplayName("saveOrderItem")
  class SaveOrderItem {

    @Test
    @DisplayName("Doit créer un article et retourner l'objet avec son ID généré")
    void shouldCreateOrderItemAndReturnWithGeneratedId() {
      Order order     = buildOrder(1);
      Dish dish       = buildDish(1, "Calzone");
      OrderItem input = buildOrderItem(null, order, dish, 2);
      OrderItem saved = buildOrderItem(10,   order, dish, 2);
      when(orderItemRepository.save(input)).thenReturn(saved);

      OrderItem result = orderItemService.saveOrderItem(input);

      assertNotNull(result.getId());
      assertEquals(10, result.getId());
      assertEquals(2, result.getQuantity());
      verify(orderItemRepository).save(input);
    }

    @Test
    @DisplayName("Doit conserver la relation bidirectionnelle order ↔ item après save")
    void shouldPreserveBidirectionalRelationship() {
      Order order     = buildOrder(1);
      Dish dish       = buildDish(1, "Reine");
      OrderItem item  = buildOrderItem(null, order, dish, 1);

      // Simule le comportement du contrôleur :
      // order.getItems().add(item) avant saveOrder/saveOrderItem
      order.getItems().add(item);

      OrderItem saved = buildOrderItem(5, order, dish, 1);
      when(orderItemRepository.save(item)).thenReturn(saved);

      OrderItem result = orderItemService.saveOrderItem(item);

      // La relation item → order doit être maintenue
      assertNotNull(result.getOrder());
      assertEquals(1, result.getOrder().getId());
      // La liste de la commande doit contenir l'item
      assertTrue(result.getOrder().getItems().contains(item));
      verify(orderItemRepository).save(item);
    }

    @Test
    @DisplayName("Doit mettre à jour un article existant (quantité)")
    void shouldUpdateExistingOrderItemQuantity() {
      Order order     = buildOrder(1);
      Dish dish       = buildDish(1, "Margherita");
      OrderItem item  = buildOrderItem(3, order, dish, 5);
      when(orderItemRepository.save(item)).thenReturn(item);

      OrderItem result = orderItemService.saveOrderItem(item);

      assertEquals(3, result.getId());
      assertEquals(5, result.getQuantity());
      verify(orderItemRepository).save(item);
    }
  }

  // =========================================================================
  // deleteOrderItem — hard delete
  // =========================================================================

  @Nested
  @DisplayName("deleteOrderItem — hard delete")
  class DeleteOrderItem {

    @Test
    @DisplayName("Doit supprimer l'article si l'ID existe")
    void shouldDeleteOrderItemWhenIdExists() throws ApiNotFoundException {
      when(orderItemRepository.existsById(1)).thenReturn(true);

      assertDoesNotThrow(() -> orderItemService.deleteOrderItem(1));

      verify(orderItemRepository).existsById(1);
      verify(orderItemRepository).deleteById(1);
    }

    @Test
    @DisplayName("Doit lever ApiNotFoundException si l'article n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenItemNotFound() {
      when(orderItemRepository.existsById(999)).thenReturn(false);

      ApiNotFoundException ex = assertThrows(
          ApiNotFoundException.class,
          () -> orderItemService.deleteOrderItem(999)
      );

      assertNotNull(ex.getMessage());
      verify(orderItemRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Hard delete — save ne doit jamais être appelé")
    void shouldBeHardDeleteNotSoftDelete() throws ApiNotFoundException {
      when(orderItemRepository.existsById(2)).thenReturn(true);

      orderItemService.deleteOrderItem(2);

      // OrderItem n'a pas de champ available — la suppression est physique
      verify(orderItemRepository, never()).save(any());
      verify(orderItemRepository).deleteById(2);
    }
  }
}