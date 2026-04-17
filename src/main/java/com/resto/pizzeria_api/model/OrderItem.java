package com.resto.pizzeria_api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * Entité représentant un article de commande.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    /**
     * Identifiant de l'article.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_order_item")
    private Integer id;

    /**
     * Commande associée.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_order", nullable = false)
    @JsonBackReference
    @NotNull(message = "La commande est obligatoire")
    private Order order;

    /**
     * Plat associé.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_dish", nullable = false)
    @NotNull(message = "Le plat est obligatoire")
    private Dish dish;

    /**
     * Quantité commandée.
     */
    @Column(name = "quantity_order_item", nullable = false)
    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être supérieure à 0")
    private Integer quantity;
}