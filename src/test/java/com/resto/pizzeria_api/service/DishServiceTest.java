package com.resto.pizzeria_api.service;

import com.resto.pizzeria_api.exception.ApiNotFoundException;
import com.resto.pizzeria_api.model.Dish;
import com.resto.pizzeria_api.repository.DishRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// OPTIMISATION 1 : les deux extensions coexistent sur une seule annotation
@ExtendWith({MockitoExtension.class, AllureJunit5.class})
@Epic("Gestion des plats")
@Feature("Service - DishService")
@Owner("resto-pizza-api")
@DisplayName("DishService tests unitaires")
class DishServiceTest {

  @Mock
  private DishRepository dishRepository;

  @InjectMocks
  private DishService dishService;

  // ── Fixture ────────────────────────────────────────────────────────────────
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

  // ── getAllDishes ───────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getAllDishes")
  class GetAllDishes {

    @Test
    @Story("Lister les plats disponibles")
    @Severity(SeverityLevel.NORMAL)
    @Description("Doit retourner uniquement les plats dont available=true")
    @DisplayName("Doit retourner uniquement les plats disponibles (available=true)")
    void shouldReturnOnlyAvailableDishes() {
      List<Dish> available = List.of(
          buildDish(1, "Margherita", new BigDecimal("10.50")),
          buildDish(2, "Reine", new BigDecimal("12.00"))
      );
      when(dishRepository.findByAvailableTrue()).thenReturn(available);

      Allure.step("Appel dishService.getAllDishes()", () -> {
        List<Dish> result = dishService.getAllDishes();
        Allure.step("Vérifier taille = 2", () -> assertEquals(2, result.size()));
        Allure.step("Vérifier que tous les plats sont available=true",
            () -> assertTrue(result.stream().allMatch(Dish::getAvailable)));
      });

      verify(dishRepository, times(1)).findByAvailableTrue();
      // findAll ne doit jamais être appelé
      verify(dishRepository, never()).findAll();
    }

    @Test
    @Story("Lister les plats disponibles")
    @Severity(SeverityLevel.MINOR)
    @Description("Doit retourner une liste vide si aucun plat n'est disponible")
    @DisplayName("Doit retourner une liste vide si aucun plat disponible")
    void shouldReturnEmptyListWhenNoDishAvailable() {
      when(dishRepository.findByAvailableTrue()).thenReturn(List.of());

      Allure.step("Appel dishService.getAllDishes()", () -> {
        List<Dish> result = dishService.getAllDishes();
        Allure.step("Vérifier liste vide", () -> assertTrue(result.isEmpty()));
      });

      verify(dishRepository).findByAvailableTrue();
    }
  }

  // ── getDishById ────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getDishById")
  class GetDishById {

    @Test
    @Story("Consulter un plat par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit retourner le plat si l'ID existe et que le plat est disponible")
    @DisplayName("Doit retourner le plat si l'ID existe et que le plat est disponible")
    void shouldReturnDishWhenIdExistsAndAvailable() throws ApiNotFoundException {
      Dish dish = buildDish(1, "Margherita", new BigDecimal("10.50"));
      when(dishRepository.findByIdAndAvailableTrue(1)).thenReturn(Optional.of(dish));

      Allure.step("Appel dishService.getDishById(1)", () -> {
        Dish result = dishService.getDishById(1);
        Allure.step("Vérifier id=1", () -> assertEquals(1, result.getId()));
        Allure.step("Vérifier name=Margherita", () -> assertEquals("Margherita", result.getName()));
        Allure.step("Vérifier available=true", () -> assertTrue(result.getAvailable()));
      });

      verify(dishRepository).findByIdAndAvailableTrue(1);
    }

    @Test
    @Story("Consulter un plat par ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit lever ApiNotFoundException si l'ID n'existe pas")
    @DisplayName("Doit lever ApiNotFoundException si l'ID n'existe pas")
    void shouldThrowApiNotFoundExceptionWhenIdNotExists() {
      when(dishRepository.findByIdAndAvailableTrue(999)).thenReturn(Optional.empty());

      Allure.step("Appel dishService.getDishById(999) → exception attendue", () -> {
        ApiNotFoundException ex = assertThrows(
            ApiNotFoundException.class,
            () -> dishService.getDishById(999)
        );
        Allure.step("Vérifier message contient 999",
            () -> assertTrue(ex.getMessage().contains("999")));
      });

      verify(dishRepository).findByIdAndAvailableTrue(999);
    }

    @Test
    @Story("Consulter un plat par ID")
    @Severity(SeverityLevel.NORMAL)
    @Description("Un plat archivé (available=false) est introuvable par ID")
    @DisplayName("Doit lever ApiNotFoundException si le plat est archivé (available=false)")
    void shouldThrowApiNotFoundExceptionWhenDishIsArchived() {
      when(dishRepository.findByIdAndAvailableTrue(2)).thenReturn(Optional.empty());

      Allure.step("Appel dishService.getDishById(2) → plat archivé", () -> {
        ApiNotFoundException ex = assertThrows(
            ApiNotFoundException.class,
            () -> dishService.getDishById(2)
        );
        Allure.step("Vérifier message non null", () -> assertNotNull(ex.getMessage()));
      });

      verify(dishRepository).findByIdAndAvailableTrue(2);
    }
  }

  // ── saveDish ───────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("saveDish")
  class SaveDish {

    @Test
    @Story("Créer un plat")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Doit sauvegarder et retourner le plat avec son ID généré")
    @DisplayName("Doit sauvegarder et retourner le plat avec son ID généré")
    void shouldSaveAndReturnDishWithGeneratedId() {
      Dish input = buildDish(null, "Calzone", new BigDecimal("11.00"));
      Dish saved = buildDish(10, "Calzone", new BigDecimal("11.00"));
      when(dishRepository.save(input)).thenReturn(saved);

      Allure.step("Appel dishService.saveDish(input)", () -> {
        Dish result = dishService.saveDish(input);
        Allure.step("Vérifier id=10", () -> assertEquals(10, result.getId()));
        Allure.step("Vérifier name=Calzone", () -> assertEquals("Calzone", result.getName()));
      });

      verify(dishRepository).save(input);
    }

    @Test
    @Story("Modifier un plat")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit sauvegarder un plat existant (mise à jour)")
    @DisplayName("Doit sauvegarder un plat existant (mise à jour)")
    void shouldSaveExistingDish() {
      Dish existing = buildDish(5, "4 Fromages", new BigDecimal("13.00"));
      when(dishRepository.save(existing)).thenReturn(existing);

      Allure.step("Appel dishService.saveDish(existing)", () -> {
        Dish result = dishService.saveDish(existing);
        Allure.step("Vérifier id=5", () -> assertEquals(5, result.getId()));
        Allure.step("Vérifier name=4 Fromages", () -> assertEquals("4 Fromages", result.getName()));
      });

      verify(dishRepository).save(existing);
    }
  }

  // ── deleteDish (soft delete) ───────────────────────────────────────────────
  @Nested
  @DisplayName("deleteDish soft delete")
  class DeleteDish {

    @Test
    @Story("Supprimer un plat (soft delete)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Doit archiver le plat en mettant available=false et appeler save(), jamais deleteById()")
    @DisplayName("Doit archiver le plat en mettant available=false")
    void shouldSetAvailableFalseInsteadOfDeleting() throws ApiNotFoundException {
      Dish dish = buildDish(1, "Margherita", new BigDecimal("10.50"));
      when(dishRepository.findByIdAndAvailableTrue(1)).thenReturn(Optional.of(dish));
      when(dishRepository.save(dish)).thenReturn(dish);

      Allure.step("Appel dishService.deleteDish(1)", () -> {
        dishService.deleteDish(1);
        Allure.step("Vérifier available=false (soft delete)", () -> assertFalse(dish.getAvailable()));
        Allure.step("Vérifier save() appelé", () -> verify(dishRepository).save(dish));
        Allure.step("Vérifier deleteById() jamais appelé",
            () -> verify(dishRepository, never()).deleteById(any()));
      });
    }

    @Test
    @Story("Supprimer un plat (soft delete)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Doit lever ApiNotFoundException si le plat n'existe pas ou est déjà archivé")
    @DisplayName("Doit lever ApiNotFoundException si le plat n'existe pas ou est déjà archivé")
    void shouldThrowApiNotFoundExceptionWhenDishNotFoundOrArchived() {
      when(dishRepository.findByIdAndAvailableTrue(999)).thenReturn(Optional.empty());

      Allure.step("Appel dishService.deleteDish(999) → exception attendue", () -> {
        ApiNotFoundException ex = assertThrows(
            ApiNotFoundException.class,
            () -> dishService.deleteDish(999)
        );
        Allure.step("Vérifier message contient 999",
            () -> assertTrue(ex.getMessage().contains("999")));
        Allure.step("Vérifier save() jamais appelé",
            () -> verify(dishRepository, never()).save(any()));
        Allure.step("Vérifier deleteById() jamais appelé",
            () -> verify(dishRepository, never()).deleteById(any()));
      });
    }

    @Test
    @Story("Supprimer un plat (soft delete)")
    @Severity(SeverityLevel.NORMAL)
    @Description("Un plat déjà archivé ne doit pas être trouvé pour une deuxième suppression")
    @DisplayName("Un plat déjà archivé est introuvable pour une 2e suppression")
    void shouldNotFindAlreadyArchivedDish() {
      when(dishRepository.findByIdAndAvailableTrue(3)).thenReturn(Optional.empty());

      Allure.step("Appel dishService.deleteDish(3) → plat déjà archivé", () -> {
        assertThrows(ApiNotFoundException.class, () -> dishService.deleteDish(3));
        Allure.step("Vérifier save() jamais appelé",
            () -> verify(dishRepository, never()).save(any()));
      });
    }
  }
}