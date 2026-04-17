package com.resto.pizzeria_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entité représentant un plat.
 */
@Entity
@Table(name = "dishes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Dish {
    /**
     * Identifiant du plat.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dish")
    private Integer id;

    /**
     * Nom du plat.
     */
    @Column(name = "name_dish", unique = true, nullable = false, length = 50)
    @NotBlank(message = "Le nom du plat est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom du plat doit être entre 2 et 50 caractères")
    private String name;

    /**
     * Type de plat.
     *
     */
    @Column(name = "category_dish", length = 50)
    @Size(max = 50, message = "La catégorie ne doit pas dépasser 50 caractères")
    private String category;

    /**
     * Prix du plat.
     */
    @Column(name = "price_dish", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Le prix est obligatoire")
    @PositiveOrZero(message = "Le prix doit être supérieur à 0")
    private BigDecimal price;

    /**
     * Description du plat.
     */
    @Column(name = "description_dish", nullable = false, length = 250)
    @Size(max = 250, message = "La description ne doit pas dépasser 250 caractères")
    private String description;

    /**
     * Permet d'injecter la valeur par défaut directement dans le SQL généré
     */
    @Column(name = "available_dish", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean available = true;
}