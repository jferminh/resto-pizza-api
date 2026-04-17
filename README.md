# Pizzeria Firenze API

API REST pour gérer les données d'une pizzeria avec **Spring Boot**.

## Prérequis

* Java 25
* Maven
* MySQL

## Installation

1. Cloner le dépôt :
```
git clone <url_du_repo>
cd pizzeria-api
```

2. Configurer les variables d’environnement pour la base de données :
```
export DB_URL=jdbc:mysql://localhost:3306/*BASE_DE_DONNEES*
export DB_USERNAME=*NOM_UTILISATEUR*
export DB_PASSWORD=*MOT_DE_PASSE*
```

3. Créer la base de données avec `database/resto_schema.sql`
