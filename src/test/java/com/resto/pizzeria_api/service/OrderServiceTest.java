package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Client;
import com.resto.pizzeria_api.model.Order;
import com.resto.pizzeria_api.model.OrderItem;
import com.resto.pizzeria_api.repository.OrderRepository;
import io.qameta.allure.*;
import io.qameta.allure.junit5.AllureJunit5;
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

// OPTIMISATION 1 : les deux extensions coexistent sur une seule annotation
@ExtendWith({MockitoExtension.class, AllureJunit5.class})
@Epic("Gestion des commandes")
@Feature("Service - OrderService")
@Owner("resto-pizza-api")
@DisplayName("OrderService tests unitaires")
class OrderServiceTest {

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private OrderService orderService;

  // ── Fixtures ───────────────────────────────────────────────────────────────

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

  private OrderItem buildItem(Order order) {
    OrderItem item = new OrderItem();
    item.setId(1);
    item.setOrder(order);
    item.setQuantity(2);
    return item;
  }

  // ── getAllOrders ───────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getAllOrders")
  class GetAllOrders {

    @Test
    @Story("Lister toutes les commandes")
    @Severity(SeverityLevel.NORMAL)
    @Description("Doit retourner toutes les commandes sans filtrage (pas de soft delete)")
    @DisplayName("Doit retourner toutes les commandes sans filtrage")
    void shouldReturnAllOrders() {
      List<Order> orders = List.of(buildOrder(1), buildOrder(2));
      when(orderRepository.findAll()).thenReturn(orders);

      Allure.step("Appel orderService.getAllOrders()", () -> {
        List<Order> result = orderService.getAllOrders();
        Allure.step("Vérifier taille = 2", () -> assertEquals(2, result.size()));
      });

      verify(orderRepository, times(1)).findAll();
    }

    @Test
    @Story("Lister toutes les commandes")
    @Severity(SeverityLevel.MINOR)
    @Description("Doit retourner une liste vide si aucune commande en base")
    @DisplayName("Doit retourner une liste vide si aucune commande")
    void shouldReturnEmptyListWhenNoOrders() {
      when(orderRepository.findAll()).thenReturn(List.of());

      Allure.step("Appel orderService.getAllOrders()", () -> {
        List<Order> result = orderService.getAllOrders();
        Allure.step("Vérifier liste vide", () -> assertTrue(result.isEmpty()));
      });

      verify(orderRepository).findAll();
    }
  }

  // ── getOrderById ───────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getOrderById")
  class GetOrderById {

    @Test
    @Story("Consulter une commande par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit retourner la commande si l'ID existe")
    @DisplayName("Doit retourner la commande si l'ID existe")
    void shouldReturnOrderWhenIdExists() throws ApiNotFoundException {
      Order order = buildOrder(1);
      when(orderRepository.findById(1)).thenReturn(Optional.of(order));

      Allure.step("Appel orderService.getOrderById(1)", () -> {
        Order result = orderService.getOrderById(1);
        Allure.step("Vérifier non null", () -> assertNotNull(result));
        Allure.step("Vérifier id=1", () -> assertEquals(1, result.getId()));
        Allure.step("Vérifier dailyId=1", () -> assertEquals(1, result.getDailyId()));
        Allure.step("Vérifier client non null", () -> assertNotNull(result.getClient()));
      });

      verify(orderRepository).findById(1);
    }

    @Test
    @Story("Consulter une commande par ID")
    @Severity(SeverityLevel.NORMAL)
    @Description("Doit retourner la commande avec ses items et la relation bidirectionnelle item→order")
    @DisplayName("Doit retourner la commande avec ses items si présents")
    void shouldReturnOrderWithItems() throws ApiNotFoundException {
      Order order = buildOrder(1);
      order.getItems().add(buildItem(order));
      when(orderRepository.findById(1)).thenReturn(Optional.of(order));

      Allure.step("Appel orderService.getOrderById(1)", () -> {
        Order result = orderService.getOrderById(1);
        Allure.step("Vérifier items non vide", () -> assertFalse(result.getItems().isEmpty()));
        Allure.step("Vérifier taille items = 1", () -> assertEquals(1, result.getItems().size()));
        Allure.step("Vérifier relation bidirectionnelle item→order",
            () -> assertEquals(result, result.getItems().get(0).getOrder()));
      });

      verify(orderRepository).findById(1);
    }

    @Test
    @Story("Consulter une commande par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit lever ApiNotFoundException si l'ID n'existe pas")
    @DisplayName("Doit lever ApiNotFoundException si l'ID n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenIdNotExists() {
      when(orderRepository.findById(999)).thenReturn(Optional.empty());

      Allure.step("Appel orderService.getOrderById(999) → exception attendue", () -> {
        ApiNotFoundException ex = assertThrows(
            ApiNotFoundException.class,
            () -> orderService.getOrderById(999)
        );
        Allure.step("Vérifier message non null", () -> assertNotNull(ex.getMessage()));
      });

      verify(orderRepository).findById(999);
    }
  }

  // ── saveOrder ──────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("saveOrder")
  class SaveOrder {

    @Test
    @Story("Créer une commande")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Doit créer une commande et retourner l'objet avec son ID généré")
    @DisplayName("Doit créer une commande et retourner l'objet avec son ID généré")
    void shouldCreateOrderAndReturnWithGeneratedId() {
      Order input = buildOrder(null);
      Order saved = buildOrder(10);
      when(orderRepository.save(input)).thenReturn(saved);

      Allure.step("Appel orderService.saveOrder(input)", () -> {
        Order result = orderService.saveOrder(input);
        Allure.step("Vérifier id non null", () -> assertNotNull(result.getId()));
        Allure.step("Vérifier id=10", () -> assertEquals(10, result.getId()));
      });

      verify(orderRepository).save(input);
    }

    @Test
    @Story("Modifier une commande")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit mettre à jour une commande existante")
    @DisplayName("Doit mettre à jour une commande existante")
    void shouldUpdateExistingOrder() {
      Order existing = buildOrder(5);
      when(orderRepository.save(existing)).thenReturn(existing);

      Allure.step("Appel orderService.saveOrder(existing)", () -> {
        Order result = orderService.saveOrder(existing);
        Allure.step("Vérifier id=5", () -> assertEquals(5, result.getId()));
      });

      verify(orderRepository).save(existing);
    }

    @Test
    @Story("Créer une commande")
    @Severity(SeverityLevel.NORMAL)
    @Description("Doit persister la relation item→order lors de la sauvegarde")
    @DisplayName("Doit persister la relation item→order lors de la sauvegarde")
    void shouldPersistItemOrderRelationship() {
      Order order = buildOrder(null);
      OrderItem item = buildItem(order);
      order.getItems().add(item);

      Order saved = buildOrder(7);
      saved.getItems().add(item);
      when(orderRepository.save(order)).thenReturn(saved);

      Allure.step("Appel orderService.saveOrder(order avec item)", () -> {
        Order result = orderService.saveOrder(order);
        Allure.step("Vérifier items non vide", () -> assertFalse(result.getItems().isEmpty()));
        Allure.step("Vérifier relation bidirectionnelle maintenue",
            () -> assertNotNull(result.getItems().get(0).getOrder()));
      });

      verify(orderRepository).save(order);
    }
  }

  // ── deleteOrder (hard delete) ──────────────────────────────────────────────
  @Nested
  @DisplayName("deleteOrder hard delete")
  class DeleteOrder {

    @Test
    @Story("Supprimer une commande (hard delete)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Doit supprimer la commande si l'ID existe (hard delete)")
    @DisplayName("Doit supprimer la commande si l'ID existe")
    void shouldDeleteOrderWhenIdExists() throws ApiNotFoundException {
      when(orderRepository.existsById(1)).thenReturn(true);

      Allure.step("Appel orderService.deleteOrder(1)", () -> {
        assertDoesNotThrow(() -> orderService.deleteOrder(1));
        Allure.step("Vérifier existsById(1) appelé", () -> verify(orderRepository).existsById(1));
        Allure.step("Vérifier deleteById(1) appelé", () -> verify(orderRepository).deleteById(1));
      });
    }

    @Test
    @Story("Supprimer une commande (hard delete)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit lever ApiNotFoundException si la commande n'existe pas — deleteById jamais appelé")
    @DisplayName("Doit lever ApiNotFoundException si la commande n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenOrderNotFound() {
      when(orderRepository.existsById(999)).thenReturn(false);

      Allure.step("Appel orderService.deleteOrder(999) → exception attendue", () -> {
        ApiNotFoundException ex = assertThrows(
            ApiNotFoundException.class,
            () -> orderService.deleteOrder(999)
        );
        Allure.step("Vérifier message non null", () -> assertNotNull(ex.getMessage()));
        Allure.step("Vérifier deleteById jamais appelé",
            () -> verify(orderRepository, never()).deleteById(any()));
      });
    }

    @Test
    @Story("Supprimer une commande (hard delete)")
    @Severity(SeverityLevel.NORMAL)
    @Description("La suppression est un hard delete — save() ne doit jamais être appelé (pas de soft delete)")
    @DisplayName("La suppression doit être un hard delete — save() jamais appelé")
    void shouldBeHardDeleteNotSoftDelete() throws ApiNotFoundException {
      when(orderRepository.existsById(2)).thenReturn(true);

      Allure.step("Appel orderService.deleteOrder(2)", () -> {
        orderService.deleteOrder(2);
        Allure.step("Vérifier save() jamais appelé (pas de soft delete)",
            () -> verify(orderRepository, never()).save(any()));
        Allure.step("Vérifier deleteById(2) appelé",
            () -> verify(orderRepository).deleteById(2));
      });
    }
  }
}