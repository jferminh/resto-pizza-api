CREATE
DATABASE  IF NOT EXISTS `resto` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE
`resto`;
-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: resto
-- ------------------------------------------------------
-- Server version	8.4.7

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `clients`
--

DROP TABLE IF EXISTS `clients`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `clients`
(
    `id_client`         int                                    NOT NULL AUTO_INCREMENT,
    `first_name_client` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    `last_name_client`  varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`id_client`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dishes`
--

DROP TABLE IF EXISTS `dishes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dishes`
(
    `id_dish`          int                                     NOT NULL AUTO_INCREMENT,
    `name_dish`        varchar(50) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `price_dish`       decimal(15, 2)                          NOT NULL,
    `description_dish` varchar(250) COLLATE utf8mb4_unicode_ci NOT NULL,
    `available_dish`   tinyint(1)                              NOT NULL DEFAULT 1,
    `category_dish`    varchar(50) null,
    PRIMARY KEY (`id_dish`),
    UNIQUE KEY `name_dish` (`name_dish`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders`
(
    `id_order`            int      NOT NULL AUTO_INCREMENT,
    `daily_id_order`      int      NOT NULL,
    `date_creation_order` datetime NOT NULL,
    `id_status`           int,
    `id_client`           int      NOT NULL,
    PRIMARY KEY (`id_order`),
    KEY                   `id_status` (`id_status`),
    KEY                   `id_client` (`id_client`),
    CONSTRAINT `fk_orders_clients` FOREIGN KEY (`id_client`) REFERENCES `clients` (`id_client`),
    CONSTRAINT `fk_orders_status` FOREIGN KEY (`id_status`) REFERENCES `status` (`id_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items`
(
    `id_order_item`       int NOT NULL AUTO_INCREMENT,
    `id_order`            int NOT NULL,
    `id_dish`             int NOT NULL,
    `quantity_order_item` int NOT NULL,
    PRIMARY KEY (`id_order_item`),
    KEY                   `id_dish` (`id_dish`),
    KEY                   `id_order` (`id_order`),
    KEY                   `id_order_item` (`id_order_item`),
    CONSTRAINT `fk_order_items_orders` FOREIGN KEY (`id_order`) REFERENCES `orders` (`id_order`),
    CONSTRAINT `fk_order_items_dishes` FOREIGN KEY (`id_dish`) REFERENCES `dishes` (`id_dish`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `status`
--

DROP TABLE IF EXISTS `status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `status`
(
    `id_status`    int                                    NOT NULL AUTO_INCREMENT,
    `label_status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`id_status`),
    UNIQUE KEY `label_status` (`label_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-01 16:50:52

-- ==========================================
-- JEU D'ESSAI INITIAL - BASE RESTO
-- ==========================================

USE
`resto`;

-- 1. Insertion des statuts de commande
-- Toujours insérer les nomenclatures en premier pour éviter les erreurs de clés étrangères
INSERT INTO `status` (`label_status`)
VALUES ('En attente'),
       ('En préparation'),
       ('Prête'),
       ('Livrée'),
       ('Annulée');

-- 2. Insertion des clients (Table client)
-- Attention à la colonne `last_name_client` définie dans le schéma
INSERT INTO `clients` (`first_name_client`, `last_name_client`)
VALUES ('Jean', 'Dupont'),
       ('Marie', 'Curie'),
       ('Alan', 'Turing'),
       ('Ada', 'Lovelace');

-- 3. Insertion des plats/pizzas (Table dish)
INSERT INTO `dishes` (`name_dish`, `price_dish`, `description_dish`, `available_dish`, `category_dish`)
VALUES ('Pizza Margherita', 10.00, 'Sauce tomate, mozzarella fraîche, basilic', 1, 'pizza'),
       ('Pizza 4 Fromages', 13.50, 'Sauce tomate, mozzarella, chèvre, gorgonzola, emmental', 1, 'pizza'),
       ('Pizza Reine', 11.50, 'Sauce tomate, mozzarella, jambon blanc, champignons frais', 1, 'pizza'),
       ('Tiramisu Maison', 5.50, 'Dessert traditionnel italien au café et mascarpone', 1, 'dessert'),
       ('Coca-Cola', 2.50, 'Canette 33cl', 1, 'boisson');

-- 4. Insertion des commandes (Table order_)
-- On simule quelques commandes passées aujourd'hui.
-- Les id_status font référence aux ID de 1 à 5 créés ci-dessus.
-- Les id_client font référence aux ID de 1 à 4 créés ci-dessus.
INSERT INTO `orders` (`daily_id_order`, `date_creation_order`, `id_status`, `id_client`)
VALUES (1, '2026-04-01 12:15:00', 4, 1), -- Commande 1 : Livrée pour Jean
       (2, '2026-04-01 12:45:00', 3, 2), -- Commande 2 : Prête pour Marie
       (3, '2026-04-01 19:30:00', 2, 3), -- Commande 3 : En préparation pour Alan
       (4, '2026-04-01 19:45:00', 1, 4);
-- Commande 4 : En attente pour Ada

-- 5. Insertion des détails de commande (Table order_item)
-- Association entre les commandes (id_order) et les plats (id_dish) avec la quantité
INSERT INTO `order_items` (`id_order`, `id_dish`, `quantity_order_item`)
VALUES
-- Commande 1 (Jean) : 2 Margherita, 2 Coca
(1, 1, 2),
(1, 5, 2),
-- Commande 2 (Marie) : 1 Reine, 1 Tiramisu
(2, 3, 1),
(2, 4, 1),
-- Commande 3 (Alan) : 1 4 Fromages, 1 Margherita, 2 Coca
(3, 2, 1),
(3, 1, 1),
(3, 5, 2),
-- Commande 4 (Ada) : 2 Reine
(4, 3, 2);
