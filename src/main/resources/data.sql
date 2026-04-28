-- ============================================================
-- data.sql — Jeu de données de test
-- Chargé automatiquement par Spring Boot au démarrage
-- Compatible avec le schéma Hibernate généré
-- ============================================================

-- ─────────────────────────────────────────────
-- DISHES (plats du menu)
-- ─────────────────────────────────────────────
INSERT IGNORE INTO dishes (name_dish, description_dish, price_dish, category_dish, available_dish)
VALUES
  ('Margherita',    'Tomate, mozzarella, basilic',                    9.50,  'PIZZA',   1),
  ('Reine',         'Tomate, mozzarella, jambon, champignons',        11.50, 'PIZZA',   1),
  ('4 Fromages',    'Mozzarella, gorgonzola, chèvre, parmesan',       12.00, 'PIZZA',   1),
  ('Calzone',       'Tomate, mozzarella, jambon, oeuf',               13.00, 'PIZZA',   1),
  ('Végétarienne',  'Tomate, mozzarella, poivrons, courgettes',       11.00, 'PIZZA',   1),
  ('Pepperoni',     'Tomate, mozzarella, pepperoni',                  12.50, 'PIZZA',   1),
  ('Tiramisu',      'Mascarpone, café, biscuit',                       5.50, 'DESSERT', 1),
  ('Panna Cotta',   'Crème, vanille, coulis de fruits rouges',         4.50, 'DESSERT', 1),
  ('Coca-Cola',     'Boisson gazeuse 33cl',                            2.50, 'BOISSON', 1),
  ('Eau minérale',  'Eau plate 50cl',                                  1.50, 'BOISSON', 1),
  ('Jus d orange',  'Jus d orange pressé 25cl',                        3.00, 'BOISSON', 1);

-- ─────────────────────────────────────────────
-- CLIENTS
-- ─────────────────────────────────────────────
INSERT IGNORE INTO clients (first_name_client, last_name_client)
VALUES
  ('Alice',   'Martin'),
  ('Bob',     'Dupont'),
  ('Carlos',  'Fermin'),
  ('Sophie',  'Leblanc'),
  ('Thomas',  'Bernard');

-- ─────────────────────────────────────────────
-- ORDERS (commandes)
-- id_status nullable → NULL = commande en attente
-- daily_id_order = numéro de commande du jour
-- ─────────────────────────────────────────────
INSERT IGNORE INTO orders (id_order, date_creation_order, daily_id_order, id_client, id_status)
VALUES
  (1, NOW(), 1, 1, NULL),
  (2, NOW(), 2, 2, NULL),
  (3, NOW(), 3, 3, NULL);

-- ─────────────────────────────────────────────
-- ORDER_ITEMS (lignes de commandes)
-- ─────────────────────────────────────────────
INSERT IGNORE INTO order_items (quantity_order_item, id_dish, id_order)
VALUES
  (2, 1, 1),   -- 2x Margherita → commande 1
  (1, 7, 1),   -- 1x Tiramisu   → commande 1
  (1, 2, 2),   -- 1x Reine      → commande 2
  (1, 9, 2),   -- 1x Coca-Cola  → commande 2
  (3, 3, 3),   -- 3x 4 Fromages → commande 3
  (2, 8, 3);   -- 2x Panna Cotta→ commande 3