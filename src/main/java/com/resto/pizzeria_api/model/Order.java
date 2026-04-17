package com.resto.pizzeria_api.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité représentant une commande.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /**
     * Identifiant de la commande.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_order")
    private Integer id;

    /**
     * Identifiant quotidien de la commande.
     */
    @Column(name = "daily_id_order", nullable = false)
    @NotNull(message = "L'identifiant journalier est obligatoire")
    @Positive(message = "L'identifiant journalier doit être supérieur à 0")
    private Integer dailyId;

    /**
     * Date de création de la commande.
     */
    @Column(name = "date_creation_order", nullable = false)
    private LocalDateTime creationDate;

    /**
     * Statut de la commande.
     */
    @ManyToOne
    @JoinColumn(name = "id_status")
    private Status status;

    /**
     * Client associé.
     */
    @ManyToOne
    @JoinColumn(name = "id_client", nullable = false)
    @NotNull(message = "Le client est obligatoire")
    private Client client;

    /**
     * Articles de la commande.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    // @NotEmpty(message = "La commande doit contenir au moins un article")
    @Valid
    private List<OrderItem> items = new ArrayList<>();
}