package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Dish;
import com.resto.pizzeria_api.model.Order;
import com.resto.pizzeria_api.model.OrderItem;
import com.resto.pizzeria_api.repository.OrderItemRepository;
import io.qameta.allure.*;
import io.qameta.allure.junit5.AllureJunit5;
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

// OPTIMISATION 1 : les deux extensions coexistent sur une seule annotation
@ExtendWith({MockitoExtension.class, AllureJunit5.class})
@Epic("Gestion des articles de commande")
@Feature("Service - OrderItemService")
@Owner("resto-pizza-api")
@DisplayName("OrderItemService tests unitaires")
class OrderItemServiceTest {

  @Mock
  private OrderItemRepository orderItemRepository;

  @InjectMocks
  private OrderItemService orderItemService;

  // ── Fixtures ───────────────────────────────────────────────────────────────

  private Order buildOrder(Integer id) {
    Order order = new Order();
    order.setId(id);
    order.setDailyId(1);
    order.setCreationDate(LocalDateTime.now());
    order.setItems(new ArrayList<>());
    return order;
  }

  private Dish buildDish(Integer id, String name) {
    Dish dish = new Dish();
    dish.setId(id);
    dish.setName(name);
    dish.setPrice(new BigDecimal("10.50"));
    dish.setAvailable(true);
    return dish;
  }

  private OrderItem buildOrderItem(Integer id, Order order, Dish dish, int quantity) {
    OrderItem item = new OrderItem();
    item.setId(id);
    item.setOrder(order);
    item.setDish(dish);
    item.setQuantity(quantity);
    return item;
  }

  // ── getAllOrderItems ───────────────────────────────────────────────────────
  @Nested
  @DisplayName("getAllOrderItems")
  class GetAllOrderItems {

    @Test
    @Story("Lister tous les articles")
    @Severity(SeverityLevel.NORMAL)
    @Description("Doit retourner tous les articles sans filtrage")
    @DisplayName("Doit retourner tous les articles sans filtrage")
    void shouldReturnAllOrderItems() {
      Order order = buildOrder(1);
      Dish dish   = buildDish(1, "Margherita");
      List<OrderItem> items = List.of(
          buildOrderItem(1, order, dish, 2),
          buildOrderItem(2, order, dish, 1)
      );
      when(orderItemRepository.findAll()).thenReturn(items);

      Allure.step("Appel orderItemService.getAllOrderItems()", () -> {
        List<OrderItem> result = orderItemService.getAllOrderItems();
        Allure.step("Vérifier taille = 2", () -> assertEquals(2, result.size()));
      });

      verify(orderItemRepository, times(1)).findAll();
    }

    @Test
    @Story("Lister tous les articles")
    @Severity(SeverityLevel.MINOR)
    @Description("Doit retourner une liste vide si aucun article en base")
    @DisplayName("Doit retourner une liste vide si aucun article")
    void shouldReturnEmptyListWhenNoItems() {
      when(orderItemRepository.findAll()).thenReturn(List.of());

      Allure.step("Appel orderItemService.getAllOrderItems()", () -> {
        List<OrderItem> result = orderItemService.getAllOrderItems();
        Allure.step("Vérifier liste vide", () -> assertTrue(result.isEmpty()));
      });

      verify(orderItemRepository).findAll();
    }
  }

  // ── getOrderItemById ───────────────────────────────────────────────────────
  @Nested
  @DisplayName("getOrderItemById")
  class GetOrderItemById {

    @Test
    @Story("Consulter un article par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit retourner l'article avec ses relations Order et Dish")
    @DisplayName("Doit retourner l'article avec ses relations Order et Dish")
    void shouldReturnOrderItemWithRelations() throws ApiNotFoundException {
      Order order    = buildOrder(1);
      Dish dish      = buildDish(1, "Margherita");
      OrderItem item = buildOrderItem(1, order, dish, 3);
      when(orderItemRepository.findById(1)).thenReturn(Optional.of(item));

      Allure.step("Appel orderItemService.getOrderItemById(1)", () -> {
        OrderItem result = orderItemService.getOrderItemById(1);
        Allure.step("Vérifier id=1", () -> assertEquals(1, result.getId()));
        Allure.step("Vérifier quantity=3", () -> assertEquals(3, result.getQuantity()));
        Allure.step("Vérifier order non null", () -> assertNotNull(result.getOrder()));
        Allure.step("Vérifier dish non null", () -> assertNotNull(result.getDish()));
        Allure.step("Vérifier order.id=1", () -> assertEquals(1, result.getOrder().getId()));
        Allure.step("Vérifier dish.name=Margherita",
            () -> assertEquals("Margherita", result.getDish().getName()));
      });

      verify(orderItemRepository).findById(1);
    }

    @Test
    @Story("Consulter un article par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit lever ApiNotFoundException si l'ID n'existe pas")
    @DisplayName("Doit lever ApiNotFoundException si l'ID n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenIdNotExists() {
      when(orderItemRepository.findById(999)).thenReturn(Optional.empty());

      Allure.step("Appel orderItemService.getOrderItemById(999) → exception attendue", () -> {
        ApiNotFoundException ex = assertThrows(
            ApiNotFoundException.class,
            () -> orderItemService.getOrderItemById(999)
        );
        Allure.step("Vérifier message non null", () -> assertNotNull(ex.getMessage()));
      });

      verify(orderItemRepository).findById(999);
    }
  }

  // ── saveOrderItem ──────────────────────────────────────────────────────────
  @Nested
  @DisplayName("saveOrderItem")
  class SaveOrderItem {

    @Test
    @Story("Créer un article")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Doit créer un article et retourner l'objet avec son ID généré")
    @DisplayName("Doit créer un article et retourner l'objet avec son ID généré")
    void shouldCreateOrderItemAndReturnWithGeneratedId() {
      Order order     = buildOrder(1);
      Dish dish       = buildDish(1, "Calzone");
      OrderItem input = buildOrderItem(null, order, dish, 2);
      OrderItem saved = buildOrderItem(10, order, dish, 2);
      when(orderItemRepository.save(input)).thenReturn(saved);

      Allure.step("Appel orderItemService.saveOrderItem(input)", () -> {
        OrderItem result = orderItemService.saveOrderItem(input);
        Allure.step("Vérifier id non null", () -> assertNotNull(result.getId()));
        Allure.step("Vérifier id=10", () -> assertEquals(10, result.getId()));
        Allure.step("Vérifier quantity=2", () -> assertEquals(2, result.getQuantity()));
      });

      verify(orderItemRepository).save(input);
    }

    @Test
    @Story("Créer un article")
    @Severity(SeverityLevel.NORMAL)
    @Description("Doit conserver la relation bidirectionnelle order↔item après save")
    @DisplayName("Doit conserver la relation bidirectionnelle order↔item après save")
    void shouldPreserveBidirectionalRelationship() {
      Order order    = buildOrder(1);
      Dish dish      = buildDish(1, "Reine");
      OrderItem item = buildOrderItem(null, order, dish, 1);
      order.getItems().add(item);

      OrderItem saved = buildOrderItem(5, order, dish, 1);
      when(orderItemRepository.save(item)).thenReturn(saved);

      Allure.step("Appel orderItemService.saveOrderItem(item)", () -> {
        OrderItem result = orderItemService.saveOrderItem(item);
        Allure.step("Vérifier order non null", () -> assertNotNull(result.getOrder()));
        Allure.step("Vérifier order.id=1", () -> assertEquals(1, result.getOrder().getId()));
        Allure.step("Vérifier order.items contient l'item",
            () -> assertTrue(result.getOrder().getItems().contains(item)));
      });

      verify(orderItemRepository).save(item);
    }

    @Test
    @Story("Modifier un article")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit mettre à jour un article existant (quantité)")
    @DisplayName("Doit mettre à jour un article existant (quantité)")
    void shouldUpdateExistingOrderItemQuantity() {
      Order order    = buildOrder(1);
      Dish dish      = buildDish(1, "Margherita");
      OrderItem item = buildOrderItem(3, order, dish, 5);
      when(orderItemRepository.save(item)).thenReturn(item);

      Allure.step("Appel orderItemService.saveOrderItem(item existant)", () -> {
        OrderItem result = orderItemService.saveOrderItem(item);
        Allure.step("Vérifier id=3", () -> assertEquals(3, result.getId()));
        Allure.step("Vérifier quantity=5", () -> assertEquals(5, result.getQuantity()));
      });

      verify(orderItemRepository).save(item);
    }
  }

  // ── deleteOrderItem (hard delete via orphanRemoval) ────────────────────────
  @Nested
  @DisplayName("deleteOrderItem hard delete")
  class DeleteOrderItem {

    @Test
    @Story("Supprimer un article (hard delete)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Doit supprimer l'article via orphanRemoval — deleteById jamais appelé")
    @DisplayName("Doit supprimer l'article si l'ID existe")
    void shouldDeleteOrderItemWhenIdExists() throws ApiNotFoundException {
      OrderItem mockItem = new OrderItem();
      mockItem.setId(1);
      Order mockOrder = new Order();
      mockOrder.setItems(new ArrayList<>(List.of(mockItem)));
      mockItem.setOrder(mockOrder);
      when(orderItemRepository.findById(1)).thenReturn(Optional.of(mockItem));

      Allure.step("Appel orderItemService.deleteOrderItem(1)", () -> {
        assertDoesNotThrow(() -> orderItemService.deleteOrderItem(1));
        Allure.step("Vérifier findById(1) appelé", () -> verify(orderItemRepository).findById(1));
        Allure.step("Vérifier deleteById jamais appelé (orphanRemoval)",
            () -> verify(orderItemRepository, never()).deleteById(any()));
      });
    }

    @Test
    @Story("Supprimer un article (hard delete)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit lever ApiNotFoundException si l'article n'existe pas")
    @DisplayName("Doit lever ApiNotFoundException si l'article n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenItemNotFound() {
      when(orderItemRepository.findById(999)).thenReturn(Optional.empty());

      Allure.step("Appel orderItemService.deleteOrderItem(999) → exception attendue", () -> {
        ApiNotFoundException ex = assertThrows(
            ApiNotFoundException.class,
            () -> orderItemService.deleteOrderItem(999)
        );
        Allure.step("Vérifier message non null", () -> assertNotNull(ex.getMessage()));
        Allure.step("Vérifier deleteById jamais appelé",
            () -> verify(orderItemRepository, never()).deleteById(any()));
      });
    }

    @Test
    @Story("Supprimer un article (hard delete)")
    @Severity(SeverityLevel.NORMAL)
    @Description("Hard delete via orphanRemoval — save() ne doit jamais être appelé (pas de soft delete)")
    @DisplayName("Hard delete — save() jamais appelé")
    void shouldBeHardDeleteNotSoftDelete() throws ApiNotFoundException {
      OrderItem mockItem = new OrderItem();
      mockItem.setId(2);
      Order mockOrder = new Order();
      mockOrder.setItems(new ArrayList<>(List.of(mockItem)));
      mockItem.setOrder(mockOrder);
      when(orderItemRepository.findById(2)).thenReturn(Optional.of(mockItem));

      Allure.step("Appel orderItemService.deleteOrderItem(2)", () -> {
        orderItemService.deleteOrderItem(2);
        Allure.step("Vérifier save() jamais appelé (pas de soft delete)",
            () -> verify(orderItemRepository, never()).save(any()));
        Allure.step("Vérifier deleteById jamais appelé (orphanRemoval gère)",
            () -> verify(orderItemRepository, never()).deleteById(any()));
      });
    }
  }
}