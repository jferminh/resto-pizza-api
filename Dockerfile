# ============================================================
# Dockerfile — resto-pizza-api
# Pattern : multi-stage build (séparation build / run)
# Java 25 LTS — Eclipse Temurin (image officielle légère)
# ============================================================

# ─────────────────────────────────────────────
# STAGE 1 : BUILD
# Objectif : compiler le code et produire le .jar
# Cette image NE sera PAS dans l'image finale
# ─────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS build
# Installer Maven dans l'image Alpine
RUN apk add --no-cache maven
# Définir le répertoire de travail dans le conteneur de build
WORKDIR /app

# Copier d'abord uniquement le pom.xml
# → Docker met en cache les dépendances Maven séparément du code source
# → Si seul le code change (pas les dépendances), le téléchargement Maven
#   est réutilisé depuis le cache Docker = build plus rapide
COPY pom.xml .

# Copier le wrapper Maven s'il existe (sinon Maven doit être dans l'image)
COPY .mvn/ .mvn/
COPY mvnw .

# Copier tout le code source
COPY src/ src/

# Compiler le projet et produire le .jar
# -DskipTests : les tests sont déjà exécutés AVANT ce stage dans le pipeline
# Ici on veut juste l'artefact final, proprement
RUN mvn package -DskipTests --no-transfer-progress

# ─────────────────────────────────────────────
# STAGE 2 : RUN
# Objectif : image finale légère avec uniquement le .jar
# On utilise le JRE (pas le JDK) → image ~3x plus petite
# ─────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS run

# Bonne pratique sécurité : ne pas tourner en root
# Créer un utilisateur dédié sans droits d'administration
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Répertoire de travail de l'application
WORKDIR /app
# Copier UNIQUEMENT le .jar depuis le stage build
# → l'image finale ne contient pas Maven, pas le JDK, pas le code source
COPY --from=build /app/target/resto-pizza-api-0.0.1-SNAPSHOT.jar app.jar

# Donner les droits sur le fichier à l'utilisateur non-root
RUN chown appuser:appgroup app.jar

# Basculer sur l'utilisateur non-root
USER appuser

# Exposer le port sur lequel Spring Boot écoute
# (doit correspondre à server.port dans application.properties)
EXPOSE 8080

# Point d'entrée : démarrer l'application
# Forme EXEC (tableau) recommandée : reçoit correctement les signaux OS (SIGTERM)
# → permet un arrêt propre du conteneur (graceful shutdown)
ENTRYPOINT ["java", "-jar", "app.jar"]