package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Dish;
import com.resto.pizzeria_api.repository.DishRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de DishService.
 *
 * Vérifie la logique métier de chaque méthode en isolant complètement
 * le service via Mockito — aucun accès base de données.
 *
 * Points spécifiques à Dish testés ici :
 * - getAllDishes() utilise findByAvailableTrue() et non findAll()
 * - getDishById() utilise findByIdAndAvailableTrue() — un plat archivé est introuvable
 * - deleteDish() est un soft delete : il met available=false au lieu de supprimer la ligne
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DishService — tests unitaires")
class DishServiceTest {

  @Mock
  private DishRepository dishRepository;

  @InjectMocks
  private DishService dishService;

  // -------------------------------------------------------------------------
  // Fixture partagée
  // -------------------------------------------------------------------------

  /**
   * Construit un plat disponible avec les champs minimaux valides.
   */
  private Dish buildDish(Integer id, String name, BigDecimal price) {
    Dish d = new Dish();
    d.setId(id);
    d.setName(name);
    d.setPrice(price);
    d.setDescription("Description du plat");
    d.setCategory("PIZZA");
    d.setAvailable(true);
    return d;
  }

  // =========================================================================
  // getAllDishes
  // =========================================================================

  @Nested
  @DisplayName("getAllDishes")
  class GetAllDishes {

    @Test
    @DisplayName("Doit retourner uniquement les plats disponibles (available=true)")
    void shouldReturnOnlyAvailableDishes() {
      // Le service appelle findByAvailableTrue, pas findAll
      List<Dish> available = List.of(
          buildDish(1, "Margherita", new BigDecimal("10.50")),
          buildDish(2, "Reine",      new BigDecimal("12.00"))
      );
      when(dishRepository.findByAvailableTrue()).thenReturn(available);

      List<Dish> result = dishService.getAllDishes();

      assertEquals(2, result.size());
      assertTrue(result.stream().allMatch(Dish::getAvailable));
      verify(dishRepository, times(1)).findByAvailableTrue();
      // Vérifie que findAll n'est jamais appelé
      verify(dishRepository, never()).findAll();
    }

    @Test
    @DisplayName("Doit retourner une liste vide si aucun plat disponible")
    void shouldReturnEmptyListWhenNoDishAvailable() {
      when(dishRepository.findByAvailableTrue()).thenReturn(List.of());

      List<Dish> result = dishService.getAllDishes();

      assertTrue(result.isEmpty());
      verify(dishRepository).findByAvailableTrue();
    }
  }

  // =========================================================================
  // getDishById
  // =========================================================================

  @Nested
  @DisplayName("getDishById")
  class GetDishById {

    @Test
    @DisplayName("Doit retourner le plat si l'ID existe et que le plat est disponible")
    void shouldReturnDishWhenIdExistsAndAvailable() throws ApiNotFoundException {
      Dish dish = buildDish(1, "Margherita", new BigDecimal("10.50"));
      when(dishRepository.findByIdAndAvailableTrue(1)).thenReturn(Optional.of(dish));

      Dish result = dishService.getDishById(1);

      assertNotNull(result);
      assertEquals(1, result.getId());
      assertEquals("Margherita", result.getName());
      assertTrue(result.getAvailable());
      verify(dishRepository).findByIdAndAvailableTrue(1);
    }

    @Test
    @DisplayName("Doit lever ApiNotFoundException si l'ID n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenIdNotExists() {
      when(dishRepository.findByIdAndAvailableTrue(999)).thenReturn(Optional.empty());

      ApiNotFoundException ex = assertThrows(
          ApiNotFoundException.class,
          () -> dishService.getDishById(999)
      );

      assertTrue(ex.getMessage().contains("999"));
      verify(dishRepository).findByIdAndAvailableTrue(999);
    }

    @Test
    @DisplayName("Doit lever ApiNotFoundException si le plat est archivé (available=false)")
    void shouldThrowApiNotFoundExceptionWhenDishIsArchived() {
      // Un plat archivé n'est pas retourné par findByIdAndAvailableTrue
      when(dishRepository.findByIdAndAvailableTrue(2)).thenReturn(Optional.empty());

      ApiNotFoundException ex = assertThrows(
          ApiNotFoundException.class,
          () -> dishService.getDishById(2)
      );

      assertNotNull(ex.getMessage());
      verify(dishRepository).findByIdAndAvailableTrue(2);
    }
  }

  // =========================================================================
  // saveDish
  // =========================================================================

  @Nested
  @DisplayName("saveDish")
  class SaveDish {

    @Test
    @DisplayName("Doit sauvegarder et retourner le plat avec son ID généré")
    void shouldSaveAndReturnDishWithGeneratedId() {
      Dish input = buildDish(null, "Calzone", new BigDecimal("11.00"));
      Dish saved = buildDish(10,   "Calzone", new BigDecimal("11.00"));
      when(dishRepository.save(input)).thenReturn(saved);

      Dish result = dishService.saveDish(input);

      assertNotNull(result.getId());
      assertEquals(10, result.getId());
      assertEquals("Calzone", result.getName());
      verify(dishRepository).save(input);
    }

    @Test
    @DisplayName("Doit sauvegarder un plat existant (mise à jour)")
    void shouldSaveExistingDish() {
      Dish existing = buildDish(5, "4 Fromages", new BigDecimal("13.00"));
      when(dishRepository.save(existing)).thenReturn(existing);

      Dish result = dishService.saveDish(existing);

      assertEquals(5, result.getId());
      assertEquals("4 Fromages", result.getName());
      verify(dishRepository).save(existing);
    }
  }

  // =========================================================================
  // deleteDish — soft delete
  // =========================================================================

  @Nested
  @DisplayName("deleteDish — soft delete")
  class DeleteDish {

    @Test
    @DisplayName("Doit archiver le plat en mettant available=false")
    void shouldSetAvailableFalseInsteadOfDeleting() throws ApiNotFoundException {
      Dish dish = buildDish(1, "Margherita", new BigDecimal("10.50"));
      when(dishRepository.findByIdAndAvailableTrue(1)).thenReturn(Optional.of(dish));
      when(dishRepository.save(dish)).thenReturn(dish);

      dishService.deleteDish(1);

      // Vérifie le soft delete : available passe à false
      assertFalse(dish.getAvailable());
      // Vérifie que save est appelé pour persister le changement
      verify(dishRepository).save(dish);
      // Vérifie que deleteById n'est JAMAIS appelé (ce n'est pas un vrai delete)
      verify(dishRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Doit lever ApiNotFoundException si le plat n'existe pas ou est déjà archivé")
    void shouldThrowApiNotFoundExceptionWhenDishNotFoundOrArchived() {
      when(dishRepository.findByIdAndAvailableTrue(999)).thenReturn(Optional.empty());

      ApiNotFoundException ex = assertThrows(
          ApiNotFoundException.class,
          () -> dishService.deleteDish(999)
      );

      assertTrue(ex.getMessage().contains("999"));
      // Ni save ni deleteById ne doivent être appelés
      verify(dishRepository, never()).save(any());
      verify(dishRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Un plat déjà archivé ne doit pas être trouvé pour une deuxième suppression")
    void shouldNotFindAlreadyArchivedDish() {
      // Simule un plat déjà archivé : findByIdAndAvailableTrue retourne empty
      when(dishRepository.findByIdAndAvailableTrue(3)).thenReturn(Optional.empty());

      assertThrows(
          ApiNotFoundException.class,
          () -> dishService.deleteDish(3)
      );

      verify(dishRepository, never()).save(any());
    }
  }
}